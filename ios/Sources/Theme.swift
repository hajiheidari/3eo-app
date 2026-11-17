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

    static func cat(_ c: String) -> Color {
        switch c {
        case "ai": return Color(red: 124 / 255, green: 58 / 255, blue: 237 / 255)
        case "tech": return Color(red: 37 / 255, green: 99 / 255, blue: 235 / 255)
        case "keyword": return Color(red: 234 / 255, green: 88 / 255, blue: 12 / 255)
        case "content": return Color(red: 219 / 255, green: 39 / 255, blue: 119 / 255)
        case "data": return Color(red: 8 / 255, green: 145 / 255, blue: 178 / 255)
        default: return Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255)
        }
    }

    static func tone(_ t: String) -> Color { t == "ok" ? green : (t == "warn" ? amber : (t == "bad" ? red : muted)) }

    static func catOf(_ type: String) -> String {
        switch type {
        case "ai", "aimon": return "ai"
        case "cluster", "suggest", "density", "pinkw": return "keyword"
        case "ideas", "metagen", "headings": return "content"
        case "authority": return "data"
        default: return "tech"
        }
    }

    static func slugOf(_ type: String) -> String { type == "metagen" ? "metadesc" : (type.isEmpty ? "analyze" : type) }

    /// isolate Latin words (ClaudeBot, CDN, URLs) so mixed Persian/English text keeps a readable order
    static var dir: LayoutDirection { L.isFa ? .rightToLeft : .leftToRight }

    static func bidi(_ s: String) -> String {
        if !L.isFa { return L.digits(s) }
        guard let re = try? NSRegularExpression(pattern: "[A-Za-z](?:[A-Za-z0-9_.\\-/:?=&%#@+~]*[A-Za-z0-9/])?") else { return s }
        let ns = s as NSString
        var out = ""; var last = 0
        for m in re.matches(in: s, range: NSRange(location: 0, length: ns.length)) {
            out += ns.substring(with: NSRange(location: last, length: m.range.location - last))
            out += "\u{2066}" + ns.substring(with: m.range) + "\u{2069}"
            last = m.range.location + m.range.length
        }
        return out + ns.substring(from: last)
    }

    static func fa(_ s: String) -> String {
        if !L.isFa { return s }
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
                Text(title).font(.vz(16, true))
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
                Button(L.t("بستن")) { dismiss() }.font(.vz(16, true))
                Spacer()
            }
            .padding(14)
            .background(Color.white)
            WebScreen(url: item.url)
        }
        .environment(\.layoutDirection, Theme.dir)
    }
}

/// white line icon (asset ic_<slug>) on a colored gradient rounded tile
struct IconTile: View {
    let slug: String
    let cat: String
    var size: CGFloat = 46
    var body: some View {
        let c = Theme.cat(cat)
        Image("ic_" + slug).renderingMode(.template).resizable().scaledToFit()
            .foregroundColor(.white)
            .padding(size * 0.25)
            .frame(width: size, height: size)
            .background(LinearGradient(colors: [c, c.opacity(0.72)], startPoint: .topLeading, endPoint: .bottomTrailing))
            .clipShape(RoundedRectangle(cornerRadius: size / 3))
    }
}

extension Font {
    /// Vazirmatn (bundled) for Persian text
    static func vz(_ size: CGFloat, _ bold: Bool) -> Font {
        .custom(bold ? "Vazirmatn-Bold" : "Vazirmatn-Regular", size: size)
    }
}
