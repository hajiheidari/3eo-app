import SwiftUI

/// One tool: native form built from the server's field spec; the result is drawn natively below.
struct ToolView: View {
    let tool: Tool
    @Environment(\.dismiss) private var dismiss
    @State private var values: [String: String] = [:]
    @State private var picked: [String: Set<String>] = [:]
    @State private var loading = false
    @State private var error: String?
    @State private var result: Presented?
    @State private var showShare = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(L.t("‹  بازگشت")) { dismiss() }
                Spacer()
                Button(L.t("اشتراک")) { showShare = true }.opacity((result?.share ?? "").isEmpty ? 0 : 1)
            }
            .font(.vz(15, true))
            .padding(14).background(Color.white)
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        hero
                        if let c = tool.cost, !c.isEmpty {
                            let paid = tool.paid ?? false
                            Text(L.t("هزینه: ") + c).font(.vz(13, true)).foregroundColor(paid ? Theme.amber : Theme.green)
                                .padding(.horizontal, 12).padding(.vertical, 8).frame(maxWidth: .infinity, alignment: .leading)
                                .background((paid ? Theme.amber : Theme.green).opacity(0.12)).clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        Card {
                            VStack(alignment: .leading, spacing: 14) {
                                ForEach(tool.fields) { f in field(f) }
                            }
                        }
                        if let e = error {
                            Text(e).font(.vz(13, true)).foregroundColor(Theme.red)
                        }
                        Button(action: run) {
                            HStack {
                                if loading { ProgressView().tint(.white) }
                                Text(loading ? L.t("در حال انجام… (تا ۴۵ ثانیه)") : L.t("اجرا")).font(.vz(16, true))
                            }
                            .frame(maxWidth: .infinity, minHeight: 50).foregroundColor(.white)
                            .background(LinearGradient(colors: [Theme.cat(tool.cat), Theme.cat(tool.cat).opacity(0.75)], startPoint: .leading, endPoint: .trailing))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }.disabled(loading)
                        if let r = result {
                            Text(L.t("نتیجه")).font(.vz(18, true)).foregroundColor(Theme.ink).id("result")
                            BlocksView(blocks: r.blocks)
                        }
                    }
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading)
                }
                .onChange(of: result?.id) { _ in withAnimation { proxy.scrollTo("result", anchor: .top) } }
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, Theme.dir)
        .sheet(isPresented: $showShare) { ShareSheet(items: ["\(L.isFa ? "گزارش" : "Report") \(result?.title ?? "") \(result?.host ?? "")\n\(result?.share ?? "")"]) }
        .onAppear(perform: seed)
    }

    private var hero: some View {
        let c = Theme.cat(tool.cat)
        return HStack(alignment: .top, spacing: 12) {
            Image("ic_" + tool.slug).renderingMode(.template).resizable().scaledToFit()
                .foregroundColor(.white).padding(11).frame(width: 52, height: 52)
                .background(Color.white.opacity(0.2)).clipShape(RoundedRectangle(cornerRadius: 14))
            VStack(alignment: .leading, spacing: 6) {
                Text(tool.title).font(.vz(17, true)).foregroundColor(.white)
                Text(tool.desc).font(.vz(13, false)).foregroundColor(.white.opacity(0.9))
            }
            Spacer(minLength: 0)
        }
        .padding(18).frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [c, c.opacity(0.7)], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder private func field(_ f: ToolField) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(f.label).font(.vz(13, true)).foregroundColor(Theme.muted)
            switch f.type {
            case "select":
                Picker(f.label, selection: Binding(get: { values[f.key] ?? "" }, set: { values[f.key] = $0 })) {
                    ForEach(f.options ?? [], id: \.self) { o in Text(o.count > 1 ? o[1] : "").tag(o.first ?? "") }
                }
                .pickerStyle(.menu).frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8).frame(minHeight: 44)
                .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            case "multi":
                FlowLayout(spacing: 6) {
                    ForEach(f.options ?? [], id: \.self) { o in
                        let v = o.first ?? ""
                        let on = picked[f.key]?.contains(v) ?? false
                        Button {
                            var s = picked[f.key] ?? []
                            if on { s.remove(v) } else { s.insert(v) }
                            picked[f.key] = s
                        } label: {
                            Text(o.count > 1 ? o[1] : "").font(.vz(13, false)).foregroundColor(on ? .white : Theme.ink)
                                .padding(.horizontal, 12).padding(.vertical, 7)
                                .background(on ? Theme.green : Color(red: 241 / 255, green: 245 / 255, blue: 249 / 255))
                                .clipShape(Capsule())
                        }
                    }
                }
            case "textarea":
                TextEditor(text: Binding(get: { values[f.key] ?? "" }, set: { values[f.key] = $0 }))
                    .frame(minHeight: 110).padding(6)
                    .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            default:
                TextField(f.hint ?? "", text: Binding(get: { values[f.key] ?? "" }, set: { values[f.key] = $0 }))
                    .keyboardType(f.type == "url" ? .URL : .default)
                    .textInputAutocapitalization(.never).disableAutocorrection(true)
                    .environment(\.layoutDirection, f.type == "url" ? .leftToRight : .rightToLeft)
                    .padding(14).background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            }
        }
    }

    private func seed() {
        for f in tool.fields {
            if f.type == "select" {
                if values[f.key] == nil { values[f.key] = f.def ?? (f.options?.first?.first ?? "") }
            } else if f.type == "multi", picked[f.key] == nil {
                picked[f.key] = Set((f.def ?? "").split(separator: ",").map(String.init))
            }
        }
    }

    private func run() {
        var body: [String: Any] = [:]
        for f in tool.fields {
            if f.type == "multi" { body[f.key] = Array(picked[f.key] ?? []) }
            else { body[f.key] = (values[f.key] ?? "").trimmingCharacters(in: .whitespacesAndNewlines) }
        }
        loading = true; error = nil
        Task {
            do { result = try await API.call("POST", "/api/app/tool/" + tool.slug, body: body, as: Presented.self) }
            catch { self.error = error.localizedDescription }
            loading = false
        }
    }
}
