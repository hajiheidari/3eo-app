import SwiftUI

struct WPPost: Decodable, Identifiable {
    struct R: Decodable { let rendered: String }
    struct Media: Decodable { let source_url: String? }
    struct Emb: Decodable { let media: [Media]?; enum K: String, CodingKey { case media = "wp:featuredmedia" } 
        init(from d: Decoder) throws { let c = try d.container(keyedBy: K.self); media = try? c.decode([Media].self, forKey: .media) } }
    let id: Int
    let date: String
    let title: R
    let excerpt: R?
    let content: R?
    let link: String
    let _embedded: Emb?
    var thumb: URL? { _embedded?.media?.first?.source_url.flatMap { URL(string: $0) } }
}

enum Blog {
    static let base = "https://3eo.ir/blog"
    static let qs = "per_page=12&_embed=wp:featuredmedia&_fields=id,date,title,excerpt,content,link,_embedded"

    static func posts(page: Int) async throws -> [WPPost] {
        if Demo.on { return page == 1 ? try JSONDecoder().decode([WPPost].self, from: Data(Demo.blog.utf8)) : [] }
        let urls = ["\(base)/wp-json/wp/v2/posts?\(qs)&page=\(page)", "\(base)/?rest_route=/wp/v2/posts&\(qs)&page=\(page)"]
        for u in urls {
            guard let url = URL(string: u) else { continue }
            var req = URLRequest(url: url)
            req.timeoutInterval = 30
            if let (data, resp) = try? await URLSession.shared.data(for: req), (resp as? HTTPURLResponse)?.statusCode == 200,
               let list = try? JSONDecoder().decode([WPPost].self, from: data) { return list }
        }
        throw APIError(message: "دریافت مقاله‌ها ممکن نشد. اتصال اینترنت را بررسی کنید.")
    }

    static func plain(_ html: String) -> String {
        var s = html.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        for (a, b) in [("&nbsp;", " "), ("&amp;", "&"), ("&lt;", "<"), ("&gt;", ">"), ("&quot;", "\""), ("&#8217;", "'"), ("&hellip;", "…")] { s = s.replacingOccurrences(of: a, with: b) }
        if let re = try? NSRegularExpression(pattern: "&#(\\d+);") {
            let ns = s as NSString
            var out = ""; var last = 0
            for m in re.matches(in: s, range: NSRange(location: 0, length: ns.length)) {
                out += ns.substring(with: NSRange(location: last, length: m.range.location - last))
                if let n = UInt32(ns.substring(with: m.range(at: 1))), let u = Unicode.Scalar(n) { out.append(Character(u)) }
                last = m.range.location + m.range.length
            }
            out += ns.substring(from: last)
            s = out
        }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    struct Block: Identifiable { let id = UUID(); let kind: String; let text: String }

    static func blocks(_ html: String) -> [Block] {
        guard let re = try? NSRegularExpression(pattern: "<(h[1-6]|p|li|blockquote)\\b[^>]*>(.*?)</\\1>|<img\\b[^>]*?src=\"([^\"]+)\"[^>]*>", options: [.dotMatchesLineSeparators, .caseInsensitive]) else { return [] }
        let ns = html as NSString
        var out: [Block] = []
        for m in re.matches(in: html, range: NSRange(location: 0, length: ns.length)) {
            if m.range(at: 3).location != NSNotFound { out.append(Block(kind: "img", text: ns.substring(with: m.range(at: 3)))); continue }
            let tag = ns.substring(with: m.range(at: 1)).lowercased()
            let t = plain(ns.substring(with: m.range(at: 2)))
            if t.isEmpty { continue }
            out.append(Block(kind: tag.hasPrefix("h") ? "h" : (tag == "li" ? "li" : "p"), text: t))
        }
        return out
    }
}

struct BlogView: View {
    @State private var posts: [WPPost] = []
    @State private var page = 1
    @State private var more = true
    @State private var busy = false
    @State private var error: String?
    @State private var opened: WPPost?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("وبلاگ سئو").font(.vz(20, true)).foregroundColor(Theme.ink)
                Text("آخرین مقاله‌های آموزشی ما؛ همه‌ی متن‌ها همین‌جا داخل اپ خوانده می‌شود.").font(.vz(12, false)).foregroundColor(Theme.muted)
                if let e = error, posts.isEmpty { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                ForEach(posts) { p in
                    Button { opened = p } label: {
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                if let u = p.thumb {
                                    AsyncImage(url: u) { img in img.resizable().scaledToFill() } placeholder: { Theme.greenSoft }
                                        .frame(height: 150).frame(maxWidth: .infinity).clipShape(RoundedRectangle(cornerRadius: 10))
                                }
                                Text(Theme.bidi(Blog.plain(p.title.rendered))).font(.vz(16, true)).foregroundColor(Theme.ink)
                                let ex = Blog.plain(p.excerpt?.rendered ?? "")
                                if !ex.isEmpty { Text(Theme.bidi(String(ex.prefix(140)) + (ex.count > 140 ? "…" : ""))).font(.vz(12, false)).foregroundColor(Theme.muted) }
                                Text(Theme.fa(String(p.date.prefix(10)))).font(.vz(11, true)).foregroundColor(Theme.green)
                            }
                        }
                    }.buttonStyle(.plain)
                }
                if busy { ProgressView().frame(maxWidth: .infinity) }
                else if more && !posts.isEmpty {
                    Button { Task { await load() } } label: {
                        Text("مقاله‌های بیشتر").font(.vz(14, true)).foregroundColor(Theme.green).frame(maxWidth: .infinity, minHeight: 44)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green.opacity(0.5)))
                    }
                }
            }
            .padding(16).frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.bg.ignoresSafeArea())
        .task { if posts.isEmpty { await load() } }
        .fullScreenCover(item: $opened) { BlogPostView(post: $0) }
    }

    private func load() async {
        busy = true
        defer { busy = false }
        do {
            let l = try await Blog.posts(page: page)
            posts += l; page += 1; more = l.count >= 12; error = nil
        } catch {
            if posts.isEmpty { self.error = error.localizedDescription } else { more = false }
        }
    }
}

struct BlogPostView: View {
    let post: WPPost
    @Environment(\.dismiss) private var dismiss
    @State private var share = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }
                Spacer()
                Button("اشتراک") { share = true }
            }.font(.vz(15, true)).padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let u = post.thumb {
                        AsyncImage(url: u) { img in img.resizable().scaledToFill() } placeholder: { Theme.greenSoft }
                            .frame(height: 190).frame(maxWidth: .infinity).clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    Text(Theme.bidi(Blog.plain(post.title.rendered))).font(.vz(22, true)).foregroundColor(Theme.ink)
                    Text(Theme.fa(String(post.date.prefix(10)))).font(.vz(12, false)).foregroundColor(Theme.muted)
                    ForEach(Blog.blocks(post.content?.rendered ?? "")) { b in
                        switch b.kind {
                        case "img":
                            AsyncImage(url: URL(string: b.text)) { img in img.resizable().scaledToFit() } placeholder: { Color.clear.frame(height: 1) }
                        case "h":
                            Text(Theme.bidi(b.text)).font(.vz(18, true)).foregroundColor(Theme.ink).padding(.top, 8)
                        default:
                            Text(Theme.bidi((b.kind == "li" ? "• " : "") + b.text)).font(.vz(15, false)).foregroundColor(Theme.ink).lineSpacing(5).textSelection(.enabled)
                        }
                    }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, .rightToLeft)
        .sheet(isPresented: $share) { ShareSheet(items: [Blog.plain(post.title.rendered) + "\n" + post.link]) }
    }
}
