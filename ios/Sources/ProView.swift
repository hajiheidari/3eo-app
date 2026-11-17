import SwiftUI

// MARK: models
struct RankTrack: Decodable, Identifiable {
    let id: Int
    let domain: String
    let keyword: String
    let pending: Bool
    let now: Int?
    let delta: Int?
    let every: String
    let active: Bool
    enum K: String, CodingKey { case id, domain, keyword, now, delta, every, active }
    init(from d: Decoder) throws {
        let c = try d.container(keyedBy: K.self)
        id = try c.decode(Int.self, forKey: .id)
        domain = try c.decode(String.self, forKey: .domain)
        keyword = try c.decode(String.self, forKey: .keyword)
        every = (try? c.decode(String.self, forKey: .every)) ?? "weekly"
        active = (try? c.decode(Bool.self, forKey: .active)) ?? true
        delta = try? c.decode(Int.self, forKey: .delta)
        if let s = try? c.decode(String.self, forKey: .now) { pending = s == "pending"; now = nil }
        else { pending = false; now = try? c.decode(Int.self, forKey: .now) }
    }
}
struct RankCost: Decodable { let check: Int, daily: Int, weekly: Int }
struct RankSummary: Decodable { let avg: Double?; let top3: Int; let top10: Int }
struct RankResponse: Decodable {
    let enabled: Bool
    let tracks: [RankTrack]
    let summary: RankSummary
    let max: Int
    let cost: RankCost
    let credits: Int
    let free: Bool
}
struct RankJobStart: Decodable { let job: Int }
struct RankJobRow: Decodable, Identifiable {
    let kw: String
    let state: String
    let pos: Int?
    var id: String { kw }
}
struct RankJob: Decodable { let done: Bool; let rows: [RankJobRow] }
struct RankHistRow: Decodable, Identifiable {
    let day: String
    let pos: Int?
    var id: String { day }
}
struct RankHist: Decodable { let keyword: String; let rows: [RankHistRow] }

struct MonRun: Decodable { let score: Int; let known: Bool }
struct Mon: Decodable, Identifiable {
    let id: Int
    let brand: String
    let domain: String
    let topic: String
    let status: String
    let runs: [MonRun]
}
struct MonResponse: Decodable {
    let monitors: [Mon]
    let max: Int
    let cost: Int
    let credits: Int
    let free: Bool
}

private struct ProBar: View {
    let title: String
    let dismiss: () -> Void
    var body: some View {
        HStack {
            Button(L.t("‹  بازگشت")) { dismiss() }.font(.vz(15, true))
            Spacer()
            Text(title).font(.vz(16, true)).foregroundColor(Theme.ink)
            Spacer()
            Text("").frame(width: 60)
        }.padding(14).background(Color.white)
    }
}

private func fieldStyle<V: View>(_ v: V) -> some View {
    v.font(.vz(14, false)).padding(12).background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line)).clipShape(RoundedRectangle(cornerRadius: 12))
}

private func solidButton(_ t: String, _ filled: Bool, _ color: Color = Theme.green) -> some View {
    Text(t).font(.vz(14, true)).foregroundColor(filled ? .white : color)
        .frame(maxWidth: .infinity).frame(height: 46)
        .background(filled ? color : Color.white)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(filled ? Color.clear : color.opacity(0.5)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
}

// MARK: rank
struct RankView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var d: RankResponse?
    @State private var domain = Demo.on ? "karno-shop.ir" : ""
    @State private var kws = Demo.on ? L.t("خرید کفش ورزشی\nکفش پیاده‌روی زنانه") : ""
    @State private var error: String?
    @State private var job: RankJob?
    @State private var busy = false
    @State private var hist: RankHist?
    @State private var histId = 0
    @State private var note: String?

    private func summaryLine(_ a: Double, _ s: RankSummary) -> String {
        var t = L.t("میانگین رتبه ") + Theme.fa(String(a))
        t += " · " + Theme.fa(String(s.top3)) + L.t(" کلمه در ۳ رتبه‌ی اول")
        t += " · " + Theme.fa(String(s.top10)) + L.t(" کلمه در صفحه‌ی اول")
        return t
    }

    private func cost(_ n: Int) -> String { (d?.free ?? false) ? L.t("رایگان") : Theme.fa(String(n)) + L.t(" اعتبار") }

    var body: some View {
        VStack(spacing: 0) {
            ProBar(title: L.t("رتبه‌ی کلمات کلیدی")) { dismiss() }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let d = d {
                        Text(L.t("جایگاه سایت شما در نتایج گوگل ایران برای هر کلمه (تا ۵۰ نتیجه‌ی اول). اعتبار شما: ") + Theme.fa(String(d.credits)))
                            .font(.vz(12, false)).foregroundColor(Theme.muted)
                        Card {
                            VStack(alignment: .leading, spacing: 10) {
                                fieldStyle(TextField(L.t("آدرس سایت (مثل example.ir)"), text: $domain)
                                    .textInputAutocapitalization(.never).disableAutocorrection(true).environment(\.layoutDirection, .leftToRight))
                                fieldStyle(TextEditor(text: $kws).frame(height: 90).scrollContentBackground(.hidden))
                                Text(L.t("کلمات کلیدی را در خط‌های جدا بنویسید (تا ") + Theme.fa(String(d.max)) + L.t(" کلمه)")).font(.vz(11, false)).foregroundColor(Theme.muted)
                                Button { check() } label: { solidButton(L.t("بررسی یک‌باره · هر کلمه ") + cost(d.cost.check), true) }.disabled(busy)
                                Button { track("weekly") } label: { solidButton(L.t("پایش هفتگی · هر کلمه ") + cost(d.cost.weekly), false) }
                                Button { track("daily") } label: { solidButton(L.t("پایش روزانه · هر کلمه ") + cost(d.cost.daily), false) }
                            }
                        }
                        if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                        if let n = note { Text(n).font(.vz(13, true)).foregroundColor(Theme.green) }
                        if let j = job {
                            Card {
                                VStack(alignment: .leading, spacing: 8) {
                                    ForEach(j.rows) { r in
                                        HStack {
                                            Text(Theme.bidi(r.kw)).font(.vz(14, true)).foregroundColor(Theme.ink)
                                            Spacer()
                                            if r.state == "done" {
                                                if let p = r.pos { Text(L.t("رتبه ") + Theme.fa(String(p))).font(.vz(14, true)).foregroundColor(p <= 3 ? Theme.green : (p <= 10 ? Theme.amber : Theme.ink)) }
                                                else { Text(L.t("خارج از ۵۰")).font(.vz(14, true)).foregroundColor(Theme.red) }
                                            } else if r.state == "error" {
                                                Text(L.t("خطا (برگشت اعتبار)")).font(.vz(13, true)).foregroundColor(Theme.red)
                                            } else {
                                                Text(L.t("در حال بررسی…")).font(.vz(13, false)).foregroundColor(Theme.muted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Text(L.t("کلمات تحت پایش")).font(.vz(17, true)).foregroundColor(Theme.ink).padding(.top, 10)
                        if d.tracks.isEmpty {
                            Text(L.t("هنوز کلمه‌ای را پایش نمی‌کنید.")).font(.vz(13, false)).foregroundColor(Theme.muted)
                        } else if let a = d.summary.avg {
                            Text(summaryLine(a, d.summary))
                                .font(.vz(12, false)).foregroundColor(Theme.muted)
                        }
                        ForEach(d.tracks) { t in
                            Button { openHistory(t.id) } label: {
                                Card {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(Theme.bidi(t.keyword)).font(.vz(14, true)).foregroundColor(Theme.ink)
                                            Text(sub(t))
                                                .font(.vz(11, false)).foregroundColor(Theme.muted)
                                        }
                                        Spacer()
                                        VStack(spacing: 2) {
                                            if t.pending { Text(L.t("در انتظار")).font(.vz(13, false)).foregroundColor(Theme.muted) }
                                            else if let n = t.now { Text(Theme.fa(String(n))).font(.vz(20, true)).foregroundColor(n <= 3 ? Theme.green : (n <= 10 ? Theme.amber : Theme.ink)) }
                                            else { Text(L.t("خارج از ۵۰")).font(.vz(13, true)).foregroundColor(Theme.red) }
                                            if let dl = t.delta, dl != 0 {
                                                Text((dl > 0 ? "▲ " : "▼ ") + Theme.fa(String(abs(dl)))).font(.vz(11, true)).foregroundColor(dl > 0 ? Theme.green : Theme.red)
                                            }
                                        }
                                    }
                                }
                            }.buttonStyle(.plain)
                        }
                    } else {
                        ProgressView().frame(maxWidth: .infinity).padding(.top, 30)
                    }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, Theme.dir)
        .task { await load() }
        .sheet(item: $hist) { h in
            VStack(alignment: .leading, spacing: 10) {
                Text(Theme.bidi(h.keyword)).font(.vz(17, true)).foregroundColor(Theme.ink)
                ScrollView {
                    VStack(alignment: .leading, spacing: 6) {
                        if h.rows.isEmpty { Text(L.t("هنوز نتیجه‌ای ثبت نشده است.")).font(.vz(13, false)).foregroundColor(Theme.muted) }
                        ForEach(h.rows) { r in
                            HStack {
                                Text(Theme.fa(r.day)).font(.vz(13, false)).foregroundColor(Theme.muted)
                                Spacer()
                                Text(r.pos.map { L.t("رتبه ") + Theme.fa(String($0)) } ?? L.t("خارج از ۵۰")).font(.vz(14, true)).foregroundColor(Theme.ink)
                            }
                        }
                    }
                }
                Button { removeTrack() } label: { solidButton(L.t("حذف از پایش"), false, Theme.red) }
            }
            .padding(20).environment(\.layoutDirection, Theme.dir)
        }
    }

    private func sub(_ t: RankTrack) -> String {
        var x = Theme.bidi(t.domain)
        x += t.every == "daily" ? L.t(" · روزانه") : L.t(" · هفتگی")
        if !t.active { x += L.t(" · منقضی") }
        return x
    }

    private func load() async {
        if let r = try? await API.call("GET", "/api/app/rank", as: RankResponse.self) { d = r }
    }

    private func payload() -> [String: Any] { ["domain": domain, "keywords": kws] }

    private func valid() -> Bool {
        if domain.trimmingCharacters(in: .whitespaces).isEmpty || kws.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            error = L.t("آدرس سایت و دست‌کم یک کلمه را وارد کنید."); return false
        }
        error = nil; return true
    }

    private func check() {
        guard valid() else { return }
        busy = true; note = nil
        Task {
            defer { busy = false }
            do {
                let s = try await API.call("POST", "/api/app/rank/check", body: payload(), as: RankJobStart.self)
                for _ in 0..<40 {
                    try? await Task.sleep(nanoseconds: 3_000_000_000)
                    if let j = try? await API.call("GET", "/api/app/rank/job/\(s.job)", as: RankJob.self) {
                        job = j
                        if j.done { break }
                    }
                }
                await load()
            } catch { self.error = error.localizedDescription }
        }
    }

    private func track(_ every: String) {
        guard valid() else { return }
        var b = payload(); b["every"] = every
        Task {
            do {
                _ = try await API.call("POST", "/api/app/rank/track", body: b, as: Empty.self)
                note = L.t("پایش شروع شد؛ اولین نتیجه تا چند دقیقه‌ی دیگر ثبت می‌شود.")
                kws = ""
                await load()
            } catch { self.error = error.localizedDescription }
        }
    }

    private func openHistory(_ id: Int) {
        histId = id
        Task { if let h = try? await API.call("GET", "/api/app/rank/t/\(id)", as: RankHist.self) { hist = h } }
    }

    private func removeTrack() {
        let id = histId
        hist = nil
        Task { _ = try? await API.call("POST", "/api/app/rank/t/\(id)/delete", body: [:], as: Empty.self); await load() }
    }
}

extension RankHist: Identifiable { var id: String { keyword } }

// MARK: AI brand monitor
struct MonitorView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var d: MonResponse?
    @State private var adding = false
    @State private var brand = ""
    @State private var domain = ""
    @State private var topic = ""
    @State private var error: String?
    @State private var delId: Int?

    var body: some View {
        VStack(spacing: 0) {
            ProBar(title: L.t("پایش برند در هوش مصنوعی")) { dismiss() }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let d = d {
                        Text(intro(d))
                            .font(.vz(12, false)).foregroundColor(Theme.muted)
                        if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                        ForEach(d.monitors) { m in
                            Card {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text(Theme.bidi(m.brand)).font(.vz(16, true)).foregroundColor(Theme.ink)
                                        Spacer()
                                        Text(m.status == "paused" ? L.t("متوقف") : L.t("فعال")).font(.vz(12, true)).foregroundColor(m.status == "paused" ? Theme.amber : Theme.green)
                                    }
                                    Text(topicLine(m)).font(.vz(12, false)).foregroundColor(Theme.muted)
                                    if let last = m.runs.first {
                                        HStack(spacing: 10) {
                                            Text(Theme.fa(String(last.score))).font(.vz(30, true)).foregroundColor(Theme.score(last.score))
                                            Text(last.known ? L.t("هوش مصنوعی برند را می‌شناسد") : L.t("هوش مصنوعی برند را نمی‌شناسد")).font(.vz(13, true)).foregroundColor(Theme.ink)
                                        }
                                        Text(trend(m))
                                            .font(.vz(12, false)).foregroundColor(Theme.muted)
                                    } else {
                                        Text(L.t("اولین اجرا تا چند دقیقه‌ی دیگر انجام می‌شود.")).font(.vz(12, false)).foregroundColor(Theme.muted)
                                    }
                                    HStack {
                                        if m.status == "paused" {
                                            Button { act(m.id, "resume") } label: { solidButton(L.t("ازسرگیری"), false) }
                                        }
                                        Button { delId = m.id } label: { solidButton(L.t("حذف"), false, Theme.red) }
                                    }.padding(.top, 6)
                                }
                            }
                        }
                        if d.monitors.count < d.max {
                            Button { adding = true } label: { solidButton(L.t("＋ پایش جدید"), true) }
                        } else {
                            Text(L.t("به سقف ") + Theme.fa(String(d.max)) + L.t(" پایش رسیده‌اید.")).font(.vz(12, false)).foregroundColor(Theme.muted)
                        }
                    } else {
                        ProgressView().frame(maxWidth: .infinity).padding(.top, 30)
                    }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, Theme.dir)
        .task { await load() }
        .alert(L.t("پایش جدید"), isPresented: $adding) {
            TextField(L.t("نام برند"), text: $brand)
            TextField(L.t("آدرس سایت (اختیاری)"), text: $domain).textInputAutocapitalization(.never).disableAutocorrection(true)
            TextField(L.t("حوزه‌ی فعالیت (مثلاً طراحی سایت)"), text: $topic)
            Button(L.t("انصراف"), role: .cancel) {}
            Button(L.t("شروع")) { create() }
        }
        .alert(L.t("حذف پایش"), isPresented: Binding(get: { delId != nil }, set: { if !$0 { delId = nil } })) {
            Button(L.t("انصراف"), role: .cancel) {}
            Button(L.t("حذف"), role: .destructive) { if let i = delId { act(i, "delete") } }
        } message: { Text(L.t("پایش و تاریخچه‌اش حذف می‌شود.")) }
    }

    private func intro(_ d: MonResponse) -> String {
        let c: String = d.free ? L.t("رایگان") : Theme.fa(String(d.cost)) + L.t(" اعتبار")
        return L.t("هر هفته می‌سنجیم هوش مصنوعی (ChatGPT و مشابه) برند شما را می‌شناسد یا نه. هر اجرا ") + c + L.t(" · اعتبار شما: ") + Theme.fa(String(d.credits))
    }

    private func topicLine(_ m: Mon) -> String {
        var x = Theme.bidi(m.topic)
        if !m.domain.isEmpty { x += " · " + Theme.bidi(m.domain) }
        return x
    }

    private func trend(_ m: Mon) -> String {
        let parts: [String] = m.runs.reversed().map { Theme.fa(String($0.score)) }
        return L.t("روند اجراها: ") + parts.joined(separator: " ← ")
    }

    private func load() async {
        if let r = try? await API.call("GET", "/api/app/monitor", as: MonResponse.self) { d = r }
    }

    private func act(_ id: Int, _ a: String) {
        Task { _ = try? await API.call("POST", "/api/app/monitor/\(id)", body: ["act": a], as: Empty.self); await load() }
    }

    private func create() {
        let b: [String: Any] = ["brand": brand, "domain": domain, "topic": topic]
        brand = ""; domain = ""; topic = ""
        Task {
            do { _ = try await API.call("POST", "/api/app/monitor", body: b, as: Empty.self); error = nil }
            catch { self.error = error.localizedDescription }
            await load()
        }
    }
}
