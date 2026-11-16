import Foundation
import WebKit

struct APIError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

struct Counts: Codable { var pass = 0, warn = 0, fail = 0 }

struct Check: Codable, Identifiable {
    let status: String
    let title: String
    let detail: String
    let fix: String
    let gain: Double
    var id: String { status + title + detail }
}

struct Category: Codable, Identifiable {
    let title: String
    let score: Int
    let checks: [Check]
    var id: String { title }
}

struct ToolField: Codable, Identifiable {
    let key: String
    let label: String
    let type: String
    let hint: String?
    let required: Bool?
    let options: [[String]]?
    let def: String?
    var id: String { key }
}

struct Tool: Codable, Identifiable {
    let slug: String
    let title: String
    let desc: String
    let cat: String
    let heavy: Bool?
    let cost: String?
    let paid: Bool?
    let fields: [ToolField]
    var id: String { slug }
}

struct ToolsResponse: Codable {
    let tools: [Tool]
    let categories: [String: String]
}

/// one item of a block: a list row, a bar, or (for chips) just a string
struct BItem: Decodable, Identifiable {
    let id = UUID()
    var title = "", sub = "", tag = "", tone = "", label = "", text = ""
    var value = 0.0, max = 100.0
    var ltr = false
    enum K: String, CodingKey { case title, sub, tag, tone, ltr, label, value, max, text }
    init(from d: Decoder) throws {
        if let s = try? d.singleValueContainer().decode(String.self) { title = s; return }
        let c = try d.container(keyedBy: K.self)
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        sub = (try? c.decode(String.self, forKey: .sub)) ?? ""
        tag = (try? c.decode(String.self, forKey: .tag)) ?? ""
        tone = (try? c.decode(String.self, forKey: .tone)) ?? ""
        label = (try? c.decode(String.self, forKey: .label)) ?? ""
        text = (try? c.decode(String.self, forKey: .text)) ?? ""
        value = (try? c.decode(Double.self, forKey: .value)) ?? 0
        max = (try? c.decode(Double.self, forKey: .max)) ?? 100
        ltr = (try? c.decode(Bool.self, forKey: .ltr)) ?? false
    }
}

struct Block: Decodable, Identifiable {
    let id = UUID()
    var t = "", title = "", text = "", label = ""
    var score = 0, pass = 0, warn = 0, fail = 0
    var ltr = false, muted = false
    var rows: [[String]] = []
    var items: [BItem] = []
    var categories: [Category] = []
    enum K: String, CodingKey { case t, title, text, label, score, pass, warn, fail, ltr, muted, rows, items, categories }
    init(from d: Decoder) throws {
        let c = try d.container(keyedBy: K.self)
        t = (try? c.decode(String.self, forKey: .t)) ?? ""
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        text = (try? c.decode(String.self, forKey: .text)) ?? ""
        label = (try? c.decode(String.self, forKey: .label)) ?? ""
        score = (try? c.decode(Int.self, forKey: .score)) ?? 0
        pass = (try? c.decode(Int.self, forKey: .pass)) ?? 0
        warn = (try? c.decode(Int.self, forKey: .warn)) ?? 0
        fail = (try? c.decode(Int.self, forKey: .fail)) ?? 0
        ltr = (try? c.decode(Bool.self, forKey: .ltr)) ?? false
        muted = (try? c.decode(Bool.self, forKey: .muted)) ?? false
        rows = (try? c.decode([[String]].self, forKey: .rows)) ?? []
        items = (try? c.decode([BItem].self, forKey: .items)) ?? []
        categories = (try? c.decode([Category].self, forKey: .categories)) ?? []
    }
}

/// result of running a tool or opening a saved report
struct Presented: Decodable, Identifiable {
    var id: String { (token ?? "") + title + host }
    let title: String
    let host: String
    let type: String?
    let share: String?
    let token: String?
    let blocks: [Block]
    enum K: String, CodingKey { case title, host, type, share, token, blocks }
    init(from d: Decoder) throws {
        let c = try d.container(keyedBy: K.self)
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        host = (try? c.decode(String.self, forKey: .host)) ?? ""
        type = try? c.decode(String.self, forKey: .type)
        share = try? c.decode(String.self, forKey: .share)
        token = try? c.decode(String.self, forKey: .token)
        blocks = (try? c.decode([Block].self, forKey: .blocks)) ?? []
    }
}

struct ReportRow: Codable, Identifiable {
    let token: String
    let type: String
    let host: String
    let score: Int?
    let created_at: String
    var id: String { token }
}

struct Me: Codable {
    let mobile: String
    let credits: Int
    let reports: [ReportRow]
}

struct TokenResponse: Codable { let token: String }
struct WebAuthResponse: Codable { let url: String }
struct Empty: Codable {}

@MainActor
final class Session: ObservableObject {
    static let shared = Session()
    @Published var token: String? = UserDefaults.standard.string(forKey: "token")

    func set(_ t: String?) {
        token = t
        UserDefaults.standard.set(t, forKey: "token")
    }

    func logout() {
        set(nil)
        WKWebsiteDataStore.default().removeData(ofTypes: [WKWebsiteDataTypeCookies], modifiedSince: .distantPast) {}
    }
}

enum API {
    static let base = "https://3eo.ir"

    static func call<T: Decodable>(_ method: String, _ path: String, body: [String: Any]? = nil, as type: T.Type) async throws -> T {
        guard let url = URL(string: base + path) else { throw APIError(message: "آدرس نامعتبر است.") }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.timeoutInterval = 75
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1"
        req.setValue("3eoApp/\(version) (iOS)", forHTTPHeaderField: "User-Agent")
        if let t = UserDefaults.standard.string(forKey: "token") {
            req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization")
        }
        if let body = body {
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            req.httpBody = try JSONSerialization.data(withJSONObject: body)
        }
        let data: Data
        do {
            (data, _) = try await URLSession.shared.data(for: req)
        } catch {
            throw APIError(message: "اتصال به اینترنت برقرار نشد. دوباره امتحان کنید.")
        }
        guard let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw APIError(message: "پاسخ نامعتبر از سرور. دوباره امتحان کنید.")
        }
        if (obj["ok"] as? Bool) != true {
            if (obj["code"] as? String) == "auth" {
                await Session.shared.logout()
            }
            throw APIError(message: (obj["error"] as? String) ?? "خطایی رخ داد.")
        }
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw APIError(message: "پاسخ سرور قابل خواندن نبود.")
        }
    }

    static func webURL(_ path: String) async throws -> URL {
        let r = try await call("POST", "/api/app/webauth", body: ["to": path], as: WebAuthResponse.self)
        guard let u = URL(string: r.url) else { throw APIError(message: "آدرس نامعتبر است.") }
        return u
    }
}
