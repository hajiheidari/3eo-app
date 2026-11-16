import SwiftUI

enum Theme {
    static let green = Color(red: 4 / 255, green: 120 / 255, blue: 87 / 255)
    static let greenDark = Color(red: 6 / 255, green: 95 / 255, blue: 70 / 255)
    static let greenSoft = Color(red: 209 / 255, green: 250 / 255, blue: 229 / 255)
    static let ink = Color(red: 15 / 255, green: 23 / 255, blue: 42 / 255)
    static let muted = Color(red: 100 / 255, green: 116 / 255, blue: 139 / 255)
    static let bg = Color(red: 245 / 255, green: 250 / 255, blue: 247 / 255)
    static let line = Color(red: 226 / 255, green: 232 / 255, blue: 240 / 255)
    static let red = Color(red: 220 / 255, green: 38 / 255, blue: 38 / 255)
    static let amber = Color(red: 217 / 255, green: 119 / 255, blue: 6 / 255)

    static func status(_ s: String) -> Color { s == "pass" ? green : (s == "warn" ? amber : red) }
    static func score(_ n: Int) -> Color { n >= 80 ? green : (n >= 50 ? amber : red) }

    static func fa(_ s: String) -> String {
        String(s.map { c -> Character in
            if let d = c.wholeNumberValue, c.isASCII, let u = UnicodeScalar(0x06F0 + d) { return Character(u) }
            return c
        })
    }
}

struct Card<Content: View>: View {
    let content: Content
    init(@ViewBuilder _ content: () -> Content) { self.content = content() }
    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.line, lineWidth: 1))
    }
}

struct PrimaryButton: View {
    let title: String
    var loading = false
    var dark = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack {
                if loading { ProgressView().tint(.white) }
                Text(title).font(.system(size: 16, weight: .bold))
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .foregroundColor(.white)
            .background(dark ? Theme.ink : Theme.green)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .disabled(loading)
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController { UIActivityViewController(activityItems: items, applicationActivities: nil) }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

struct WebScreen: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> WebViewController { WebViewController(startURL: url) }
    func updateUIViewController(_ uiViewController: WebViewController, context: Context) {}
}

struct WebItem: Identifiable {
    let url: URL
    var id: String { url.absoluteString }
}

struct WebSheet: View {
    let item: WebItem
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("بستن") { dismiss() }.font(.system(size: 16, weight: .bold))
                Spacer()
            }
            .padding(14)
            .background(Color.white)
            WebScreen(url: item.url)
        }
        .environment(\.layoutDirection, .rightToLeft)
    }
}
