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
            ReportsView(me: me).tabItem { Label("گزارش‌ها", systemImage: "doc.text.fill") }.tag(1)
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
    @State private var report: Report?
    @State private var web: WebItem?
    @State private var opening = false

    private let order = ["ai", "tech", "keyword", "content", "data", "util"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Image("Logo").resizable().scaledToFit().frame(height: 40).padding(.bottom, 8)
                VStack(alignment: .leading, spacing: 10) {
                    Text("تحلیل سریع سایت").font(.system(size: 19, weight: .bold)).foregroundColor(.white)
                    Text("آدرس سایت را بدهید؛ در چند ثانیه امتحان سئو، سرعت پاسخ، امنیت و آمادگی برای هوش مصنوعی را ببینید.")
                        .font(.system(size: 13)).foregroundColor(Theme.greenSoft)
                    TextField("example.com", text: $url)
                        .keyboardType(.URL).textInputAutocapitalization(.never).disableAutocorrection(true)
                        .environment(\.layoutDirection, .leftToRight)
                        .padding(14).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
                    if let e = error {
                        Text(e).font(.system(size: 13, weight: .bold)).foregroundColor(.white)
                    }
                    PrimaryButton(title: loading ? "در حال تحلیل… (تا ۳۰ ثانیه)" : "تحلیل کن", loading: loading, dark: true) { analyze() }
                }
                .padding(18).background(Theme.green).clipShape(RoundedRectangle(cornerRadius: 20))

                if let t = tools {
                    ForEach(order, id: \.self) { cat in
                        let list = t.tools.filter { $0.cat == cat }
                        if !list.isEmpty {
                            Text(t.categories[cat] ?? cat).font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink).padding(.top, 16)
                            ForEach(list) { tool in
                                Button { open(tool) } label: {
                                    Card {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 3) {
                                                Text(tool.title).font(.system(size: 15, weight: .bold)).foregroundColor(Theme.ink)
                                                Text(tool.desc).font(.system(size: 12)).foregroundColor(Theme.muted).lineLimit(2)
                                            }
                                            Spacer()
                                            Text("‹").font(.system(size: 22, weight: .bold)).foregroundColor(Theme.green)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } else {
                    ProgressView("در حال دریافت ابزارها…").padding(.top, 20)
                }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .fullScreenCover(item: $report) { ReportView(report: $0) }
        .fullScreenCover(item: $web) { WebSheet(item: $0) }
    }

    private func analyze() {
        let u = url.trimmingCharacters(in: .whitespaces)
        guard u.count >= 4 else { error = "آدرس سایت را وارد کنید."; return }
        loading = true; error = nil
        Task {
            do { report = try await API.call("POST", "/api/app/analyze", body: ["url": u], as: Report.self) }
            catch { self.error = error.localizedDescription }
            loading = false
        }
    }

    private func open(_ tool: Tool) {
        if tool.slug == "analyze" { return }
        Task {
            do { web = WebItem(url: try await API.webURL("/tools/" + tool.slug)) }
            catch { self.error = error.localizedDescription }
        }
    }
}

struct ReportsView: View {
    let me: Me?
    @State private var selected: ReportToken?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("گزارش‌های من").font(.system(size: 19, weight: .bold)).foregroundColor(Theme.ink)
                let rows = (me?.reports ?? []).filter { $0.type == "analyze" }
                if rows.isEmpty {
                    Text(me == nil ? "در حال دریافت…" : "هنوز گزارشی ندارید. از صفحه‌ی خانه یک سایت را تحلیل کنید.")
                        .font(.system(size: 14)).foregroundColor(Theme.muted)
                }
                ForEach(rows) { r in
                    Button { selected = ReportToken(id: r.token) } label: {
                        Card {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(r.host).font(.system(size: 15, weight: .bold)).foregroundColor(Theme.ink)
                                    Text(Theme.fa(String(r.created_at.prefix(10)))).font(.system(size: 12)).foregroundColor(Theme.muted)
                                }
                                Spacer()
                                Text(Theme.fa(String(r.score ?? 0))).font(.system(size: 20, weight: .bold)).foregroundColor(Theme.score(r.score ?? 0))
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
                PrimaryButton(title: "پنل کامل در وب") { openWeb("/dashboard") }.padding(.top, 6)
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

    private func openWeb(_ path: String) {
        Task {
            do { web = WebItem(url: try await API.webURL(path)) }
            catch { self.error = error.localizedDescription }
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
