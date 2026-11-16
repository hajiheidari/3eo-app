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

struct Report: Codable, Identifiable {
    let token: String
    let url: String
    let host: String
    let score: Int
    let counts: Counts
    let categories: [Category]
    let share: String
    var id: String { token }
}

struct Tool: Codable, Identifiable {
    let slug: String
    let title: String
    let desc: String
    let cat: String
    var id: String { slug }
}

struct ToolsResponse: Codable {
    let tools: [Tool]
    let categories: [String: String]
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
