import SwiftUI

struct ReportToken: Identifiable { let id: String }

struct MainTabs: View {
    @EnvironmentObject var session: Session
    @State private var tab = 0
    @State private var me: Me?
    @State private var tools: ToolsResponse?

    var body: some View {
        TabView(selection: $tab) {
            HomeView(tools: tools).tabItem { Label("خانه", systemImage: "house.fill") }.tag(0)
            ReportsView(me: me, tools: tools).tabItem { Label("گزارش‌ها", systemImage: "doc.text.fill") }.tag(1)
            AccountView(me: me).tabItem { Label("حساب من", systemImage: "person.fill") }.tag(2)
        }
        .task { await load() }
        .onChange(of: tab) { _ in Task { await loadMe() } }
    }

    private func load() async {
        await loadMe()
        tools = try? await API.call("GET", "/api/app/tools", as: ToolsResponse.self)
    }

    private func loadMe() async {
        if let m = try? await API.call("GET", "/api/app/me", as: Me.self) { me = m }
    }
}

struct HomeView: View {
    let tools: ToolsResponse?
    @State private var url = ""
    @State private var loading = false
    @State private var error: String?
    @State private var report: Presented?
    @State private var selected: Tool?
    @State private var cat = ""
    @State private var query = ""

    private let order = ["", "ai", "tech", "keyword", "content", "data"]
    private let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]

    private var filtered: [Tool] {
        (tools?.tools ?? []).filter { t in
            (cat.isEmpty || t.cat == cat) && (query.isEmpty || (t.title + " " + t.desc).contains(query))
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("سلام 👋").font(.system(size: 14, weight: .bold)).foregroundColor(Color(red: 167 / 255, green: 243 / 255, blue: 208 / 255))
                    Text("سئوی سایتت را در چند ثانیه بسنج").font(.system(size: 22, weight: .heavy)).foregroundColor(.white)
                    Text("امتحان سئو، سرعت، امنیت و آمادگی برای هوش مصنوعی؛ همه با یک آدرس.")
                        .font(.system(size: 13)).foregroundColor(Theme.greenSoft)
                    HStack(spacing: 6) {
                        TextField("example.com", text: $url)
                            .keyboardType(.URL).textInputAutocapitalization(.never).disableAutocorrection(true)
                            .environment(\.layoutDirection, .leftToRight)
                            .padding(.horizontal, 14).frame(height: 50)
                        Button(action: analyze) {
                            Group { if loading { ProgressView().tint(.white) } else { Text("تحلیل").font(.system(size: 15, weight: .bold)) } }
                                .foregroundColor(.white).frame(width: 90, height: 44)
                                .background(Theme.green).clipShape(RoundedRectangle(cornerRadius: 12))
                        }.disabled(loading)
                    }
                    .padding(.horizontal, 6).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 16)).padding(.top, 8)
                    if let e = error { Text(e).font(.system(size: 13, weight: .bold)).foregroundColor(.white) }
                }
                .padding(20).frame(maxWidth: .infinity, alignment: .leading)
                .background(LinearGradient(colors: [Color(red: 5 / 255, green: 150 / 255, blue: 105 / 255), Color(red: 6 / 255, green: 78 / 255, blue: 59 / 255)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(RoundedRectangle(cornerRadius: 26))

                TextField("جستجوی ابزار…", text: $query)
                    .padding(14).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(order, id: \.self) { c in
                            let on = c == cat
                            Button { cat = c } label: {
                                Text(c.isEmpty ? "همه" : (tools?.categories[c] ?? c)).font(.system(size: 13, weight: .bold))
                                    .foregroundColor(on ? .white : Theme.ink)
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(on ? (c.isEmpty ? Theme.green : Theme.cat(c)) : Color.white)
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(on ? Color.clear : Theme.line, lineWidth: 1))
                            }
                        }
                    }
                }

                if tools == nil {
                    ProgressView("در حال دریافت ابزارها…").frame(maxWidth: .infinity).padding(.top, 20)
                } else if filtered.isEmpty {
                    Text("ابزاری پیدا نشد.").font(.system(size: 14)).foregroundColor(Theme.muted)
                } else {
                    LazyVGrid(columns: cols, spacing: 10) {
                        ForEach(filtered) { t in
                            Button { open(t) } label: {
                                VStack(alignment: .leading, spacing: 8) {
                                    IconTile(slug: t.slug, cat: t.cat, size: 46)
                                    Text(t.title).font(.system(size: 14, weight: .bold)).foregroundColor(Theme.ink).lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                    Text(t.desc).font(.system(size: 11)).foregroundColor(Theme.muted).lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                    Spacer(minLength: 0)
                                }
                                .padding(14).frame(maxWidth: .infinity, minHeight: 150, alignment: .topLeading)
                                .background(Color.white).clipShape(RoundedRectangle(cornerRadius: 18))
                                .overlay(RoundedRectangle(cornerRadius: 18).stroke(Theme.line, lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .fullScreenCover(item: $report) { ReportView(report: $0) }
        .fullScreenCover(item: $selected) { ToolView(tool: $0) }
    }

    private func analyze() {
        let u = url.trimmingCharacters(in: .whitespaces)
        guard u.count >= 4 else { error = "آدرس سایت را وارد کنید."; return }
        loading = true; error = nil
        Task {
            do { report = try await API.call("POST", "/api/app/tool/analyze", body: ["url": u], as: Presented.self) }
            catch { self.error = error.localizedDescription }
            loading = false
        }
    }

    private func open(_ tool: Tool) {
        if tool.slug == "analyze", url.trimmingCharacters(in: .whitespaces).count >= 4 { analyze(); return }
        selected = tool
    }
}

struct ReportsView: View {
    let me: Me?
    let tools: ToolsResponse?
    @State private var selected: ReportToken?

    private func title(_ type: String) -> String {
        tools?.tools.first { $0.slug == Theme.slugOf(type) }?.title ?? "گزارش"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("گزارش‌های من").font(.system(size: 20, weight: .bold)).foregroundColor(Theme.ink)
                let rows = me?.reports ?? []
                if rows.isEmpty {
                    Text(me == nil ? "در حال دریافت…" : "هنوز گزارشی ندارید. یک ابزار را اجرا کنید.")
                        .font(.system(size: 14)).foregroundColor(Theme.muted)
                }
                ForEach(rows) { r in
                    Button { selected = ReportToken(id: r.token) } label: {
                        Card {
                            HStack(spacing: 12) {
                                IconTile(slug: Theme.slugOf(r.type), cat: Theme.catOf(r.type), size: 42)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(title(r.type)).font(.system(size: 14, weight: .bold)).foregroundColor(Theme.ink)
                                    Text(r.host).font(.system(size: 12)).foregroundColor(Theme.muted).lineLimit(1)
                                        .environment(\.layoutDirection, .leftToRight)
                                }
                                Spacer()
                                if let sc = r.score, ["analyze", "ai", "speed"].contains(r.type) {
                                    Text(Theme.fa(String(sc))).font(.system(size: 20, weight: .bold)).foregroundColor(Theme.score(sc))
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .fullScreenCover(item: $selected) { ReportLoader(token: $0.id) }
    }
}

struct AccountView: View {
    @EnvironmentObject var session: Session
    let me: Me?
    @State private var web: WebItem?
    @State private var confirmDelete = false
    @State private var confirmText = ""
    @State private var error: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("حساب من").font(.system(size: 19, weight: .bold)).foregroundColor(Theme.ink)
                Card {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("شماره‌ی موبایل").font(.system(size: 12)).foregroundColor(Theme.muted)
                        Text(me.map { Theme.fa($0.mobile) } ?? "…").font(.system(size: 17, weight: .bold)).foregroundColor(Theme.ink)
                        Text("اعتبار شما").font(.system(size: 12)).foregroundColor(Theme.muted).padding(.top, 10)
                        Text(me.map { Theme.fa(String($0.credits)) } ?? "…").font(.system(size: 30, weight: .bold)).foregroundColor(Theme.green)
                    }
                }
                outline("حریم خصوصی و قوانین", Theme.green) { if let u = URL(string: "https://3eo.ir/privacy") { web = WebItem(url: u) } }
                outline("خروج از حساب", Theme.ink) { session.logout() }
                outline("حذف حساب کاربری", Theme.red) { confirmDelete = true }
                if let e = error { Text(e).font(.system(size: 13)).foregroundColor(Theme.red) }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .fullScreenCover(item: $web) { WebSheet(item: $0) }
        .alert("حذف حساب کاربری", isPresented: $confirmDelete) {
            TextField("حذف", text: $confirmText)
            Button("انصراف", role: .cancel) { confirmText = "" }
            Button("حذف حساب", role: .destructive) { deleteAccount() }
        } message: {
            Text("همه‌ی اطلاعات و گزارش‌های حساب شما برای همیشه پاک می‌شود و برگشت‌پذیر نیست. برای تأیید، کلمه‌ی «حذف» را بنویسید.")
        }
    }

    private func outline(_ title: String, _ color: Color, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(.system(size: 15, weight: .bold)).foregroundColor(color)
                .frame(maxWidth: .infinity, minHeight: 44)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(color, lineWidth: 1))
        }
    }

    private func deleteAccount() {
        let text = confirmText.trimmingCharacters(in: .whitespaces)
        confirmText = ""
        Task {
            do {
                _ = try await API.call("POST", "/api/app/delete", body: ["confirm": text], as: Empty.self)
                session.logout()
            } catch { self.error = error.localizedDescription }
        }
    }
}
