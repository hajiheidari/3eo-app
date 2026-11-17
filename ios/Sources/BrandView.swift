import SwiftUI
import UIKit

struct BrandData: Decodable { let name: String, color: String, site: String, phone: String, email: String, address: String }
struct BrandResponse: Decodable { let brand: BrandData }
struct KeyRow: Decodable, Identifiable { let id: Int; let label: String; let hint: String }
struct KeysResponse: Decodable { let keys: [KeyRow]; let max: Int }
struct NewKey: Decodable { let key: String }
struct MonthlyInfo: Decodable { let periods: [[JSONNum]]?; let cost: Int; let brand: Bool }
struct MonthlyMade: Decodable { let url: String; let pdf: String }

/// Period list is [[30,"۳۰ روز گذشته"],...] so each cell is either a number or a string.
struct JSONNum: Decodable {
    let n: Int?
    let s: String?
    init(from d: Decoder) throws {
        let c = try d.singleValueContainer()
        n = try? c.decode(Int.self)
        s = try? c.decode(String.self)
    }
}

private func bField<V: View>(_ v: V) -> some View {
    v.font(.vz(14, false)).padding(12).background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line)).clipShape(RoundedRectangle(cornerRadius: 12))
}

private func bButton(_ t: String, _ filled: Bool, _ color: Color = Theme.green) -> some View {
    Text(t).font(.vz(14, true)).foregroundColor(filled ? .white : color)
        .frame(maxWidth: .infinity).frame(height: 46)
        .background(filled ? color : Color.white)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(filled ? Color.clear : color.opacity(0.5)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
}

struct BrandView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var loaded = false
    @State private var name = ""
    @State private var color = ""
    @State private var site = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var address = ""
    @State private var msg: String?
    @State private var bad = false
    @State private var keys: KeysResponse?
    @State private var newKey: String?
    @State private var revokeId: Int?

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }.font(.vz(15, true))
                Spacer()
                Text("سربرگ برند و API").font(.vz(16, true)).foregroundColor(Theme.ink)
                Spacer()
                Text("").frame(width: 60)
            }.padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("گزارش‌هایی که برای مشتریان می‌سازید با نام و مشخصات شما نمایش داده می‌شود.").font(.vz(12, false)).foregroundColor(Theme.muted)
                    Card {
                        VStack(alignment: .leading, spacing: 8) {
                            bField(TextField("نام برند یا شرکت شما", text: $name))
                            bField(TextField("رنگ برند (مثل #047857)", text: $color).textInputAutocapitalization(.never).disableAutocorrection(true).environment(\.layoutDirection, .leftToRight))
                            bField(TextField("وب‌سایت", text: $site).textInputAutocapitalization(.never).disableAutocorrection(true).environment(\.layoutDirection, .leftToRight))
                            bField(TextField("تلفن", text: $phone).keyboardType(.phonePad).environment(\.layoutDirection, .leftToRight))
                            bField(TextField("ایمیل", text: $email).textInputAutocapitalization(.never).keyboardType(.emailAddress).environment(\.layoutDirection, .leftToRight))
                            bField(TextField("نشانی", text: $address))
                            if let m = msg { Text(m).font(.vz(13, true)).foregroundColor(bad ? Theme.red : Theme.green) }
                            Button { save() } label: { bButton("ذخیره‌ی سربرگ", true) }
                        }
                    }
                    Text("لوگو را فعلاً می‌توانید از 3eo.ir/dashboard/brand بارگذاری کنید.").font(.vz(11, false)).foregroundColor(Theme.muted)
                    Text("کلید API").font(.vz(17, true)).foregroundColor(Theme.ink).padding(.top, 10)
                    Text("برای اتصال ابزارهای خودتان به سرویس. کلید فقط یک بار کامل نشان داده می‌شود.").font(.vz(12, false)).foregroundColor(Theme.muted)
                    if let k = keys {
                        ForEach(k.keys) { x in
                            Card {
                                HStack {
                                    Text(Theme.bidi(x.label.isEmpty ? "کلید" : x.label) + "  …" + x.hint).font(.vz(13, true)).foregroundColor(Theme.ink)
                                    Spacer()
                                    Button { revokeId = x.id } label: { Text("ابطال").font(.vz(13, true)).foregroundColor(Theme.red) }
                                }
                            }
                        }
                        if k.keys.count < k.max {
                            Button { makeKey() } label: { bButton("＋ ساخت کلید جدید", false) }
                        }
                    }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, .rightToLeft)
        .task { await load() }
        .alert("کلید شما (فقط همین حالا کامل دیده می‌شود)", isPresented: Binding(get: { newKey != nil }, set: { if !$0 { newKey = nil } })) {
            Button("کپی") { UIPasteboard.general.string = newKey; newKey = nil }
            Button("بستن", role: .cancel) { newKey = nil }
        } message: { Text(newKey ?? "") }
        .alert("ابطال کلید", isPresented: Binding(get: { revokeId != nil }, set: { if !$0 { revokeId = nil } })) {
            Button("انصراف", role: .cancel) {}
            Button("ابطال", role: .destructive) {
                if let i = revokeId { Task { _ = try? await API.call("POST", "/api/app/keys/\(i)/revoke", body: [:], as: Empty.self); await loadKeys() } }
            }
        } message: { Text("برنامه‌هایی که از این کلید استفاده می‌کنند از کار می‌افتند.") }
    }

    private func load() async {
        if !loaded, let r = try? await API.call("GET", "/api/app/brand", as: BrandResponse.self) {
            let b = r.brand
            name = b.name; color = b.color; site = b.site; phone = b.phone; email = b.email; address = b.address
            loaded = true
        }
        await loadKeys()
    }

    private func loadKeys() async {
        if let k = try? await API.call("GET", "/api/app/keys", as: KeysResponse.self) { keys = k }
    }

    private func save() {
        let b: [String: Any] = ["name": name, "color": color, "site": site, "phone": phone, "email": email, "address": address]
        Task {
            do { _ = try await API.call("POST", "/api/app/brand", body: b, as: Empty.self); bad = false; msg = "ذخیره شد ✓" }
            catch { bad = true; msg = error.localizedDescription }
        }
    }

    private func makeKey() {
        Task {
            if let r = try? await API.call("POST", "/api/app/keys", body: [:], as: NewKey.self) { newKey = r.key }
            await loadKeys()
        }
    }
}

struct MonthlySheet: View {
    let clientId: Int
    @Environment(\.dismiss) private var dismiss
    @State private var info: MonthlyInfo?
    @State private var days = 30
    @State private var note = ""
    @State private var error: String?
    @State private var share: [Any]?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("گزارش ماهانه برای مشتری").font(.vz(17, true)).foregroundColor(Theme.ink)
            if let i = info {
                if !i.brand {
                    Text("اول از «سربرگ برند و کلید API» نام برند خود را ثبت کنید.").font(.vz(13, true)).foregroundColor(Theme.red)
                } else {
                    Picker("", selection: $days) {
                        ForEach(Array((i.periods ?? []).enumerated()), id: \.offset) { _, p in Text(p.count > 1 ? (p[1].s ?? "") : "").tag(p.first?.n ?? 30) }
                    }.pickerStyle(.segmented)
                    bField(TextField("یادداشت برای مشتری (اختیاری)", text: $note, axis: .vertical).lineLimit(2...5))
                    Text("هزینه: " + Theme.fa(String(i.cost)) + " اعتبار").font(.vz(12, false)).foregroundColor(Theme.muted)
                    Button { make() } label: { bButton("ساخت و ارسال", true) }
                }
            } else { ProgressView().frame(maxWidth: .infinity) }
            if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
            Spacer()
        }
        .padding(20).environment(\.layoutDirection, .rightToLeft)
        .task { info = try? await API.call("GET", "/api/app/client/\(clientId)/monthly", as: MonthlyInfo.self) }
        .sheet(isPresented: Binding(get: { share != nil }, set: { if !$0 { share = nil; dismiss() } })) { ShareSheet(items: share ?? []) }
    }

    private func make() {
        let b: [String: Any] = ["days": days, "note": note]
        Task {
            do {
                let r = try await API.call("POST", "/api/app/client/\(clientId)/monthly", body: b, as: MonthlyMade.self)
                error = nil
                share = ["گزارش سئو\n" + r.url + "\n\nPDF:\n" + r.pdf]
            } catch { self.error = error.localizedDescription }
        }
    }
}
