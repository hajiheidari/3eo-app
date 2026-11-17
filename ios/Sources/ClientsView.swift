import SwiftUI

struct ClientRef: Identifiable { let id: Int }

struct ClientsView: View {
    @State private var data: ClientsResponse?
    @State private var adding = false
    @State private var selected: ClientRef?
    @State private var site = ""
    @State private var name = ""
    @State private var error: String?
    @State private var showBrand = false
    @State private var showContent = false
    @State private var showRank = false
    @State private var showMon = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("مشتریان من").font(.vz(20, true)).foregroundColor(Theme.ink)
                    Spacer()
                    Button { adding = true } label: {
                        Text("＋ افزودن سایت").font(.vz(14, true)).foregroundColor(.white)
                            .padding(.horizontal, 14).frame(height: 40)
                            .background(Theme.green).clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                Text("سایت مشتریان را اضافه کنید، امتیازشان را دنبال کنید و بررسی خودکار برایشان تنظیم کنید.")
                    .font(.vz(12, false)).foregroundColor(Theme.muted)
                proRow("تولید محتوا با هوش مصنوعی", "موضوع‌یاب، ساختار و مقاله‌ی کامل فارسی") { showContent = true }
                proRow("سربرگ برند و کلید API", "نام و مشخصات شما روی گزارش مشتریان") { showBrand = true }
                proRow("رتبه‌ی کلمات کلیدی", "جایگاه سایت در گوگل و پایش روزانه/هفتگی") { showRank = true }
                proRow("پایش برند در هوش مصنوعی", "ببینید ChatGPT و مشابه برند شما را می‌شناسند") { showMon = true }
                if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                if let d = data {
                    if d.clients.isEmpty {
                        Text("هنوز مشتری‌ای ندارید. با «افزودن سایت» شروع کنید.").font(.vz(14, false)).foregroundColor(Theme.muted).padding(.top, 8)
                    }
                    ForEach(d.clients) { c in
                        Button { selected = ClientRef(id: c.id) } label: {
                            Card {
                                HStack {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(c.name).font(.vz(15, true)).foregroundColor(Theme.ink)
                                        Text(c.host).font(.vz(12, false)).foregroundColor(Theme.muted)
                                            .environment(\.layoutDirection, .leftToRight)
                                        if c.auto > 0 {
                                            Text("بررسی خودکار: " + Theme.fa(String(c.auto)) + " فعال").font(.vz(11, true)).foregroundColor(Theme.green)
                                        }
                                    }
                                    Spacer()
                                    if let s = c.score {
                                        Text(Theme.fa(String(s))).font(.vz(24, true)).foregroundColor(Theme.score(s))
                                    }
                                }
                            }
                        }.buttonStyle(.plain)
                    }
                } else {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 20)
                }
            }
            .padding(16).frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Theme.bg.ignoresSafeArea())
        .task { await load() }
        .alert("افزودن سایت مشتری", isPresented: $adding) {
            TextField("example.ir", text: $site).textInputAutocapitalization(.never).disableAutocorrection(true)
            TextField("نام مشتری (اختیاری)", text: $name)
            Button("انصراف", role: .cancel) { site = ""; name = "" }
            Button("افزودن") { add() }
        }
        .fullScreenCover(isPresented: $showBrand) { BrandView() }
        .fullScreenCover(isPresented: $showContent) { ContentHomeView() }
        .fullScreenCover(isPresented: $showRank) { RankView() }
        .fullScreenCover(isPresented: $showMon) { MonitorView() }
        .fullScreenCover(item: $selected, onDismiss: { Task { await load() } }) { ClientDetailView(id: $0.id) }
    }

    private func proRow(_ t: String, _ sub: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Card {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(t).font(.vz(15, true)).foregroundColor(Theme.ink)
                        Text(sub).font(.vz(12, false)).foregroundColor(Theme.muted)
                    }
                    Spacer()
                    Text("‹").font(.vz(22, true)).foregroundColor(Theme.green)
                }
            }
        }.buttonStyle(.plain)
    }

    private func load() async {
        if let d = try? await API.call("GET", "/api/app/clients", as: ClientsResponse.self) { data = d }
    }

    private func add() {
        let body: [String: Any] = ["site": site, "name": name]
        site = ""; name = ""
        Task {
            do {
                let r = try await API.call("POST", "/api/app/clients", body: body, as: NewClient.self)
                error = nil
                await load()
                selected = ClientRef(id: r.id)
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct ClientDetailView: View {
    let id: Int
    @Environment(\.dismiss) private var dismiss
    @State private var d: ClientDetail?
    @State private var running = false
    @State private var error: String?
    @State private var report: Presented?
    @State private var openToken: ReportToken?
    @State private var editing = false
    @State private var editName = ""
    @State private var editNote = ""
    @State private var confirmDelete = false
    @State private var choice: [String: String] = [:]
    @State private var saved: String?
    @State private var showMonthly = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }
                Spacer()
            }
            .font(.vz(15, true)).padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let d = d { content(d) } else { ProgressView().frame(maxWidth: .infinity).padding(.top, 30) }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, .rightToLeft)
        .task { await load() }
        .sheet(isPresented: $showMonthly) { MonthlySheet(clientId: id) }
        .fullScreenCover(item: $report, onDismiss: { Task { await load() } }) { ReportView(report: $0) }
        .fullScreenCover(item: $openToken) { ReportLoader(token: $0.id) }
        .alert("ویرایش", isPresented: $editing) {
            TextField("نام مشتری", text: $editName)
            TextField("یادداشت", text: $editNote)
            Button("انصراف", role: .cancel) {}
            Button("ذخیره") { Task { _ = try? await API.call("POST", "/api/app/client/\(id)", body: ["name": editName, "note": editNote], as: Empty.self); await load() } }
        }
        .alert("حذف مشتری", isPresented: $confirmDelete) {
            Button("انصراف", role: .cancel) {}
            Button("حذف", role: .destructive) {
                Task { _ = try? await API.call("POST", "/api/app/client/\(id)/delete", body: [:], as: Empty.self); dismiss() }
            }
        } message: { Text("مشتری و بررسی‌های خودکارش حذف می‌شود. گزارش‌ها می‌مانند.") }
    }

    private func reportTitle(_ t: String) -> String {
        switch t {
        case "analyze": return "تحلیل کامل سئو"
        case "speed": return "سرعت سایت"
        case "ai": return "دیده‌شدن در هوش مصنوعی"
        default: return "گزارش"
        }
    }

    @ViewBuilder private func content(_ d: ClientDetail) -> some View {
        let c = d.client
        VStack(alignment: .leading, spacing: 6) {
            Text(c.name).font(.vz(19, true)).foregroundColor(.white)
            Text(c.host).font(.vz(13, false)).foregroundColor(Theme.greenSoft).environment(\.layoutDirection, .leftToRight)
            HStack(spacing: 10) {
                if let s = c.score {
                    Text(Theme.fa(String(s))).font(.vz(34, true)).foregroundColor(.white)
                    if let p = c.prev {
                        let delta = s - p
                        Text((delta > 0 ? "▲ " : (delta < 0 ? "▼ " : "")) + Theme.fa(String(abs(delta)))).font(.vz(14, true)).foregroundColor(Color(red: 167 / 255, green: 243 / 255, blue: 208 / 255))
                    }
                } else {
                    Text("هنوز تحلیلی انجام نشده").font(.vz(13, false)).foregroundColor(Theme.greenSoft)
                }
            }
        }
        .padding(18).frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [Color(red: 5 / 255, green: 150 / 255, blue: 105 / 255), Color(red: 6 / 255, green: 78 / 255, blue: 59 / 255)], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 22))

        if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
        PrimaryButton(title: running ? "در حال تحلیل… (تا ۳۰ ثانیه)" : "تحلیل کامل همین حالا", loading: running) { analyze(c.host) }

        Text("بررسی خودکار").font(.vz(17, true)).foregroundColor(Theme.ink).padding(.top, 8)
        Text("هر بار اجرا " + Theme.fa(String(d.cost)) + " اعتبار از حساب شما کم می‌کند. اگر اعتبار تمام شود، بررسی خودکار متوقف می‌شود.")
            .font(.vz(12, false)).foregroundColor(Theme.muted)
        ForEach(d.schedules) { s in
            Card {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(s.title).font(.vz(14, true)).foregroundColor(Theme.ink)
                        Spacer()
                        if s.status == "active" { Text("فعال").font(.vz(12, true)).foregroundColor(Theme.green) }
                        if s.status == "paused" { Text("متوقف").font(.vz(12, true)).foregroundColor(Theme.amber) }
                    }
                    if s.status == "paused", let er = s.error, !er.isEmpty {
                        Text(er).font(.vz(12, false)).foregroundColor(Theme.red)
                    }
                    if s.status == "active", let n = s.next_run, !n.isEmpty {
                        Text("اجرای بعدی: " + Theme.fa(n) + " (UTC)").font(.vz(11, false)).foregroundColor(Theme.muted)
                    }
                    Picker("تناوب", selection: Binding(get: { choice[s.tool] ?? s.every }, set: { choice[s.tool] = $0 })) {
                        Text("غیرفعال").tag("")
                        ForEach(d.every_options, id: \.self) { o in Text(o.count > 1 ? o[1] : "").tag(o.first ?? "") }
                    }
                    .pickerStyle(.segmented)
                    Button {
                        Task {
                            do {
                                _ = try await API.call("POST", "/api/app/client/\(id)/schedule", body: ["tool": s.tool, "every": choice[s.tool] ?? s.every], as: Empty.self)
                                error = nil; choice[s.tool] = nil; await load()
                            } catch { self.error = error.localizedDescription }
                        }
                    } label: {
                        Text("ذخیره").font(.vz(14, true)).foregroundColor(Theme.green)
                            .frame(maxWidth: .infinity, minHeight: 40)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green, lineWidth: 1))
                    }
                }
            }
        }

        Text("گزارش‌های این سایت").font(.vz(17, true)).foregroundColor(Theme.ink).padding(.top, 8)
        if d.reports.isEmpty { Text("هنوز گزارشی ندارد.").font(.vz(13, false)).foregroundColor(Theme.muted) }
        ForEach(d.reports) { r in
            Button { openToken = ReportToken(id: r.token) } label: {
                Card {
                    HStack(spacing: 10) {
                        IconTile(slug: Theme.slugOf(r.type), cat: Theme.catOf(r.type), size: 38)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(reportTitle(r.type)).font(.vz(14, true)).foregroundColor(Theme.ink)
                            Text(Theme.fa(String(r.created_at.prefix(10)))).font(.vz(11, false)).foregroundColor(Theme.muted)
                        }
                        Spacer()
                        if let s = r.score {
                            VStack(spacing: 0) {
                                Text(Theme.fa(String(s))).font(.vz(18, true)).foregroundColor(Theme.score(s))
                                Text("امتیاز").font(.vz(10, false)).foregroundColor(Theme.muted)
                            }
                        }
                    }
                }
            }.buttonStyle(.plain)
        }

        Button { showMonthly = true } label: {
            Text("ساخت گزارش ماهانه برای مشتری (با سربرگ شما)").font(.vz(15, true)).foregroundColor(Theme.green)
                .frame(maxWidth: .infinity, minHeight: 44).overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green, lineWidth: 1))
        }.padding(.top, 14)
        Button { editName = c.name; editNote = c.note; editing = true } label: {
            Text("ویرایش نام و یادداشت").font(.vz(15, true)).foregroundColor(Theme.green)
                .frame(maxWidth: .infinity, minHeight: 44).overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green, lineWidth: 1))
        }.padding(.top, 14)
        Button { confirmDelete = true } label: {
            Text("حذف این مشتری").font(.vz(15, true)).foregroundColor(Theme.red)
                .frame(maxWidth: .infinity, minHeight: 44).overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.red, lineWidth: 1))
        }
    }

    private func load() async {
        if let x = try? await API.call("GET", "/api/app/client/\(id)", as: ClientDetail.self) { d = x }
    }

    private func analyze(_ host: String) {
        running = true; error = nil
        Task {
            do { report = try await API.call("POST", "/api/app/tool/analyze", body: ["url": host], as: Presented.self) }
            catch { self.error = error.localizedDescription }
            running = false
        }
    }
}
