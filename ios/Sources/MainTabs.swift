import SwiftUI

struct ReportToken: Identifiable { let id: String }

struct MainTabs: View {
    @EnvironmentObject var session: Session
    @State private var tab = 0
    @State private var me: Me?
    @State private var tools: ToolsResponse?

    var body: some View {
        ZStack {
            HomeView(tools: tools).opacity(tab == 0 ? 1 : 0).allowsHitTesting(tab == 0)
            ReportsView(me: me, tools: tools).opacity(tab == 1 ? 1 : 0).allowsHitTesting(tab == 1)
            AccountView(me: me).opacity(tab == 2 ? 1 : 0).allowsHitTesting(tab == 2)
            ClientsView().opacity(tab == 3 ? 1 : 0).allowsHitTesting(tab == 3)
            BlogView().opacity(tab == 4 ? 1 : 0).allowsHitTesting(tab == 4)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { bar }
        .task { await load() }
        .onChange(of: tab) { _ in Task { await loadMe() } }
    }

    private var bar: some View {
        HStack(alignment: .center, spacing: 0) {
            barItem("گزارش‌ها", "doc.text.fill", 1)
            barItem("حرفه‌ای", "briefcase.fill", 3)
            Button { tab = 0 } label: {
                Image("Logo").resizable().scaledToFit().frame(height: 40)
                    .padding(.horizontal, 18).padding(.vertical, 6)
                    .frame(maxWidth: .infinity)
            }
            barItem("وبلاگ", "square.and.pencil", 4)
            barItem("حساب من", "person.fill", 2)
        }
        .padding(.top, 6).padding(.bottom, 4)
        .background(Color.white.shadow(color: .black.opacity(0.08), radius: 4, y: -1).ignoresSafeArea(edges: .bottom))
    }

    private func barItem(_ title: String, _ icon: String, _ idx: Int) -> some View {
        Button { tab = idx } label: {
            VStack(spacing: 2) {
                Image(systemName: icon).font(.system(size: 20))
                Text(title).font(.vz(11, true))
            }
            .foregroundColor(tab == idx ? Theme.green : Theme.muted)
            .frame(maxWidth: .infinity)
        }
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
                    Text("سلام 👋").font(.vz(14, true)).foregroundColor(Color(red: 167 / 255, green: 243 / 255, blue: 208 / 255))
                    Text("سئوی سایتت را در چند ثانیه بسنج").font(.vz(22, true)).foregroundColor(.white)
                    Text("امتحان سئو، سرعت، امنیت و آمادگی برای هوش مصنوعی؛ همه با یک آدرس.")
                        .font(.vz(13, false)).foregroundColor(Theme.greenSoft)
                    HStack(spacing: 6) {
                        TextField("example.com", text: $url)
                            .keyboardType(.URL).textInputAutocapitalization(.never).disableAutocorrection(true)
                            .environment(\.layoutDirection, .leftToRight)
                            .padding(.horizontal, 14).frame(height: 50)
                        Button(action: analyze) {
                            Group { if loading { ProgressView().tint(.white) } else { Text("تحلیل").font(.vz(15, true)) } }
                                .foregroundColor(.white).frame(width: 90, height: 44)
                                .background(Theme.green).clipShape(RoundedRectangle(cornerRadius: 12))
                        }.disabled(loading)
                    }
                    .padding(.horizontal, 6).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 16)).padding(.top, 8)
                    if let e = error { Text(e).font(.vz(13, true)).foregroundColor(.white) }
                }
                .padding(20).frame(maxWidth: .infinity, alignment: .leading)
                .background(LinearGradient(colors: [Color(red: 5 / 255, green: 150 / 255, blue: 105 / 255), Color(red: 6 / 255, green: 78 / 255, blue: 59 / 255)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(RoundedRectangle(cornerRadius: 26))

                TextField("جستجوی ابزار…", text: $query)
                    .padding(14).background(Color.white).clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                    ForEach(order, id: \.self) { c in
                        let on = c == cat
                        Button { cat = c } label: {
                            Text(c.isEmpty ? "همه" : (tools?.categories[c] ?? c)).font(.vz(13, true))
                                .foregroundColor(on ? .white : Theme.ink).lineLimit(1).minimumScaleFactor(0.8)
                                .frame(maxWidth: .infinity).padding(.vertical, 9)
                                .background(on ? (c.isEmpty ? Theme.green : Theme.cat(c)) : Color.white)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(on ? Color.clear : Theme.line, lineWidth: 1))
                        }
                    }
                }

                if tools == nil {
                    ProgressView("در حال دریافت ابزارها…").frame(maxWidth: .infinity).padding(.top, 20)
                } else if filtered.isEmpty {
                    Text("ابزاری پیدا نشد.").font(.vz(14, false)).foregroundColor(Theme.muted)
                } else {
                    LazyVGrid(columns: cols, spacing: 10) {
                        ForEach(filtered) { t in
                            Button { open(t) } label: {
                                VStack(alignment: .leading, spacing: 8) {
                                    IconTile(slug: t.slug, cat: t.cat, size: 46)
                                    Text(t.title).font(.vz(14, true)).foregroundColor(Theme.ink).lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                    Text(t.desc).font(.vz(11, false)).foregroundColor(Theme.muted).lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                    Spacer(minLength: 0)
                                    let paid = t.paid ?? false
                                    Text(paid ? "سهمیه‌ی روزانه + اعتبار" : "رایگان").font(.vz(11, true))
                                        .foregroundColor(paid ? Theme.amber : Theme.green)
                                        .padding(.horizontal, 8).padding(.vertical, 3)
                                        .background((paid ? Theme.amber : Theme.green).opacity(0.12)).clipShape(Capsule())
                                }
                                .padding(14).frame(maxWidth: .infinity, minHeight: 165, alignment: .topLeading)
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
                Text("گزارش‌های من").font(.vz(20, true)).foregroundColor(Theme.ink)
                let rows = me?.reports ?? []
                if rows.isEmpty {
                    Text(me == nil ? "در حال دریافت…" : "هنوز گزارشی ندارید. یک ابزار را اجرا کنید.")
                        .font(.vz(14, false)).foregroundColor(Theme.muted)
                }
                ForEach(rows) { r in
                    Button { selected = ReportToken(id: r.token) } label: {
                        Card {
                            HStack(spacing: 12) {
                                IconTile(slug: Theme.slugOf(r.type), cat: Theme.catOf(r.type), size: 42)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(title(r.type)).font(.vz(14, true)).foregroundColor(Theme.ink)
                                    Text(r.host).font(.vz(12, false)).foregroundColor(Theme.muted).lineLimit(1)
                                        .environment(\.layoutDirection, .leftToRight)
                                }
                                Spacer()
                                if let sc = r.score, ["analyze", "ai", "speed"].contains(r.type) {
                                    Text(Theme.fa(String(sc))).font(.vz(20, true)).foregroundColor(Theme.score(sc))
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
                Text("حساب من").font(.vz(19, true)).foregroundColor(Theme.ink)
                Card {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("شماره‌ی موبایل").font(.vz(12, false)).foregroundColor(Theme.muted)
                        Text(me.map { Theme.fa($0.mobile) } ?? "…").font(.vz(17, true)).foregroundColor(Theme.ink)
                        Text("اعتبار شما").font(.vz(12, false)).foregroundColor(Theme.muted).padding(.top, 10)
                        Text(me.map { Theme.fa(String($0.credits)) } ?? "…").font(.vz(30, true)).foregroundColor(Theme.green)
                    }
                }
                outline("حریم خصوصی و قوانین", Theme.green) { if let u = URL(string: "https://3eo.ir/privacy") { web = WebItem(url: u) } }
                outline("خروج از حساب", Theme.ink) { session.logout() }
                outline("حذف حساب کاربری", Theme.red) { confirmDelete = true }
                if let e = error { Text(e).font(.vz(13, false)).foregroundColor(Theme.red) }
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
            Text(title).font(.vz(15, true)).foregroundColor(color)
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
