import Foundation
import SwiftUI

/// App language (fa/en): auto-detected once from region/timezone, switchable by the user.
enum L {
    static let key = "lang"

    static var lang: String {
        get {
            if let v = UserDefaults.standard.string(forKey: key), v == "fa" || v == "en" { return v }
            return detect()
        }
        set { UserDefaults.standard.set(newValue == "fa" ? "fa" : "en", forKey: key) }
    }

    static var isFa: Bool { lang == "fa" }

    /// Persian only when the device is in Iran (timezone/region) or prefers Persian; everyone else gets English.
    static func detect() -> String {
        if TimeZone.current.identifier == "Asia/Tehran" { return "fa" }
        if Locale.current.region?.identifier == "IR" { return "fa" }
        if let p = Locale.preferredLanguages.first, p.hasPrefix("fa") { return "fa" }
        return "en"
    }

    private static let frags: [(String, String)] = EnDict.d
        .filter { $0.key.trimmingCharacters(in: .whitespaces).count >= 6 }
        .sorted { $0.key.count > $1.key.count }
        .map { ($0.key, $0.value) }

    /// translate a Persian source text when the app runs in English
    static func t(_ s: String) -> String {
        if isFa || s.isEmpty { return s }
        if let e = EnDict.d[s] { return e }
        var o = s
        var hit = false
        for (k, v) in frags where o.contains(k) { o = o.replacingOccurrences(of: k, with: v); hit = true }
        return hit ? digits(o) : s
    }

    static func digits(_ s: String) -> String {
        String(s.map { c -> Character in
            switch c {
            case "۰"..."۹": return Character(String(Int(c.unicodeScalars.first!.value) - 0x06F0))
            case "٫": return "."
            case "٬", "،": return ","
            case "٪": return "%"
            default: return c
            }
        })
    }
}
