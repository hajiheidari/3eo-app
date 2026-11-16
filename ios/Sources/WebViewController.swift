import UIKit
import WebKit

/// The 3eo.ir app: the site inside a WKWebView, plus what a plain browser tab does not give:
/// pull to refresh, an offline screen, native share sheet, file download with share, and swipe back.
/// The user agent ends with "3eoApp/<version>"; the site reads it and removes everything about buying (store rules).
final class WebViewController: UIViewController, WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler, WKDownloadDelegate {

    private let home = URL(string: "https://3eo.ir/")!
    private var startURL: URL
    private var lastURL: URL

    init(startURL: URL = URL(string: "https://3eo.ir/")!) {
        self.startURL = startURL
        self.lastURL = startURL
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }
    private var web: WKWebView!
    private let refresh = UIRefreshControl()
    private var downloadDestination: URL?
    private let background = UIColor(red: 245 / 255, green: 250 / 255, blue: 247 / 255, alpha: 1)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = background

        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1"
        let config = WKWebViewConfiguration()
        config.applicationNameForUserAgent = "3eoApp/\(version) (iOS)"
        config.websiteDataStore = WKWebsiteDataStore.default()
        let controller = WKUserContentController()
        controller.add(self, name: "retry")
        controller.add(self, name: "share")
        config.userContentController = controller

        web = WKWebView(frame: .zero, configuration: config)
        web.translatesAutoresizingMaskIntoConstraints = false
        web.navigationDelegate = self
        web.uiDelegate = self
        web.allowsBackForwardNavigationGestures = true
        web.isOpaque = false
        web.backgroundColor = background
        web.scrollView.backgroundColor = background
        view.addSubview(web)
        NSLayoutConstraint.activate([
            web.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            web.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            web.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            web.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])

        refresh.addTarget(self, action: #selector(pullToRefresh), for: .valueChanged)
        web.scrollView.refreshControl = refresh

        web.load(URLRequest(url: startURL))
    }

    // MARK: - actions

    @objc private func pullToRefresh() {
        if let url = web.url, !url.isFileURL {
            web.reload()
        } else {
            web.load(URLRequest(url: lastURL))
        }
    }

    private func isOwn(_ url: URL) -> Bool {
        guard let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http",
              let host = url.host?.lowercased() else { return false }
        return host == "3eo.ir" || host == "www.3eo.ir"
    }

    private func share(items: [Any]) {
        let sheet = UIActivityViewController(activityItems: items, applicationActivities: nil)
        sheet.popoverPresentationController?.sourceView = view
        sheet.popoverPresentationController?.sourceRect = CGRect(x: view.bounds.midX, y: view.bounds.maxY - 1, width: 1, height: 1)
        present(sheet, animated: true)
    }

    private func showOffline() {
        refresh.endRefreshing()
        guard let page = Bundle.main.url(forResource: "offline", withExtension: "html") else { return }
        web.loadFileURL(page, allowingReadAccessTo: page.deletingLastPathComponent())
    }

    // MARK: - WKNavigationDelegate

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url else { decisionHandler(.cancel); return }
        if navigationAction.targetFrame?.isMainFrame == false { decisionHandler(.allow); return } // iframes
        if url.isFileURL || ["about", "blob", "data"].contains(url.scheme?.lowercased() ?? "") { decisionHandler(.allow); return }
        if isOwn(url) {
            lastURL = url
            decisionHandler(.allow)
            return
        }
        // phone, mail, WhatsApp, Telegram and every other site open in the matching app or Safari
        UIApplication.shared.open(url)
        decisionHandler(.cancel)
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse,
                 decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        let disposition = (navigationResponse.response as? HTTPURLResponse)?
            .value(forHTTPHeaderField: "Content-Disposition")?.lowercased() ?? ""
        if disposition.contains("attachment") || !navigationResponse.canShowMIMEType {
            decisionHandler(.download)
        } else {
            decisionHandler(.allow)
        }
    }

    func webView(_ webView: WKWebView, navigationResponse: WKNavigationResponse, didBecome download: WKDownload) {
        download.delegate = self
    }

    func webView(_ webView: WKWebView, navigationAction: WKNavigationAction, didBecome download: WKDownload) {
        download.delegate = self
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        refresh.endRefreshing()
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        handleFailure(error)
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        handleFailure(error)
    }

    private func handleFailure(_ error: Error) {
        refresh.endRefreshing()
        let ns = error as NSError
        // a download or a cancelled load also ends a navigation with an error: that is not "offline"
        guard ns.domain == NSURLErrorDomain else { return }
        let offlineCodes = [NSURLErrorNotConnectedToInternet, NSURLErrorTimedOut, NSURLErrorCannotFindHost,
                            NSURLErrorCannotConnectToHost, NSURLErrorNetworkConnectionLost, NSURLErrorDNSLookupFailed,
                            NSURLErrorInternationalRoamingOff, NSURLErrorDataNotAllowed]
        if offlineCodes.contains(ns.code) { showOffline() }
    }

    // MARK: - WKUIDelegate

    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        // links that ask for a new tab: our own site loads here, everything else leaves the app
        if let url = navigationAction.request.url {
            if isOwn(url) { webView.load(navigationAction.request) } else { UIApplication.shared.open(url) }
        }
        return nil
    }

    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "باشه", style: .default) { _ in completionHandler() })
        present(alert, animated: true)
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "تأیید", style: .default) { _ in completionHandler(true) })
        alert.addAction(UIAlertAction(title: "انصراف", style: .cancel) { _ in completionHandler(false) })
        present(alert, animated: true)
    }

    // MARK: - WKScriptMessageHandler (the offline page and the site talk to the app here)

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        switch message.name {
        case "retry":
            web.load(URLRequest(url: lastURL))
        case "share":
            guard let body = message.body as? [String: Any] else { return }
            var items: [Any] = []
            if let text = body["text"] as? String, !text.isEmpty { items.append(text) }
            if let link = body["url"] as? String, let url = URL(string: link), isOwn(url) { items.append(url) }
            if !items.isEmpty { share(items: items) }
        default:
            break
        }
    }

    // MARK: - WKDownloadDelegate (CSV / PDF exports: save, then show the share sheet)

    func download(_ download: WKDownload, decideDestinationUsing response: URLResponse,
                  suggestedFilename: String, completionHandler: @escaping (URL?) -> Void) {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let destination = folder.appendingPathComponent(suggestedFilename)
        downloadDestination = destination
        completionHandler(destination)
    }

    func downloadDidFinish(_ download: WKDownload) {
        if let file = downloadDestination { share(items: [file]) }
    }

    func download(_ download: WKDownload, didFailWithError error: Error, resumeData: Data?) {
        let alert = UIAlertController(title: nil, message: "دانلود انجام نشد", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "باشه", style: .default))
        present(alert, animated: true)
    }
}
