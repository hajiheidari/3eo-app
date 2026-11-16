import SwiftUI

struct ScoreGauge: View {
    let score: Int
    var body: some View {
        ZStack {
            Circle().stroke(Theme.line, lineWidth: 12)
            Circle().trim(from: 0, to: CGFloat(score) / 100)
                .stroke(Theme.score(score), style: StrokeStyle(lineWidth: 12, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text(Theme.fa(String(score))).font(.vz(40, true)).foregroundColor(Theme.ink)
                Text("از ۱۰۰").font(.vz(13, false)).foregroundColor(Theme.muted)
            }
        }
        .frame(width: 170, height: 170)
    }
}

struct Pill: View {
    let label: String
    let n: Int
    let color: Color
    var body: some View {
        Text("\(label): \(Theme.fa(String(n)))")
            .font(.vz(13, true)).foregroundColor(color)
            .padding(.horizontal, 12).padding(.vertical, 5)
            .overlay(Capsule().stroke(color, lineWidth: 1))
    }
}

struct IssueCard: View {
    let check: Check
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 6) {
                Text(Theme.bidi((check.status == "warn" ? "⚠  " : "✕  ") + check.title))
                    .font(.vz(15, true)).foregroundColor(Theme.status(check.status))
                if !check.detail.isEmpty {
                    Text(Theme.bidi(check.detail)).font(.vz(13, false)).foregroundColor(Theme.muted)
                }
                if !check.fix.isEmpty {
                    Text(Theme.bidi("راه‌حل: " + check.fix))
                        .font(.vz(13, false)).foregroundColor(Theme.greenDark)
                        .padding(10).frame(maxWidth: .infinity, alignment: .leading)
                        .background(Theme.greenSoft).clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }
}

struct CategoryCard: View {
    let cat: Category
    @State private var open = false
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 8) {
                Button { withAnimation { open.toggle() } } label: {
                    HStack {
                        Text(cat.title).font(.vz(15, true)).foregroundColor(Theme.ink)
                        Spacer()
                        Text(Theme.fa(String(cat.score)) + "٪  " + (open ? "▴" : "▾"))
                            .font(.vz(15, true)).foregroundColor(Theme.score(cat.score))
                    }
                }
                if open {
                    ForEach(cat.checks) { c in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(Theme.bidi((c.status == "pass" ? "✓  " : (c.status == "warn" ? "⚠  " : "✕  ")) + c.title))
                                .font(.vz(14, false)).foregroundColor(Theme.status(c.status))
                            if !c.detail.isEmpty {
                                Text(Theme.bidi(c.detail)).font(.vz(12, false)).foregroundColor(Theme.muted)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
    }
}

struct ReportView: View {
    let report: Presented
    @State private var showShare = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }
                Spacer()
                Text(report.title).font(.vz(16, true)).foregroundColor(Theme.ink)
                Spacer()
                Button("اشتراک") { showShare = true }.opacity((report.share ?? "").isEmpty ? 0 : 1)
            }
            .font(.vz(15, true))
            .padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        IconTile(slug: Theme.slugOf(report.type ?? ""), cat: Theme.catOf(report.type ?? ""), size: 44)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(report.title).font(.vz(17, true)).foregroundColor(Theme.ink)
                            Text(report.host).font(.vz(13, false)).foregroundColor(Theme.muted)
                                .environment(\.layoutDirection, .leftToRight)
                        }
                    }
                    BlocksView(blocks: report.blocks)
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .sheet(isPresented: $showShare) { ShareSheet(items: ["گزارش \(report.title) \(report.host)\n\(report.share ?? "")"]) }
    }
}

struct ReportLoader: View {
    let token: String
    @State private var report: Presented?
    @State private var error: String?
    var body: some View {
        Group {
            if let r = report {
                ReportView(report: r)
            } else if let e = error {
                Text(e).foregroundColor(Theme.red).padding()
            } else {
                ProgressView("در حال دریافت گزارش…")
            }
        }
        .task {
            do { report = try await API.call("GET", "/api/app/report/" + token, as: Presented.self) }
            catch { self.error = error.localizedDescription }
        }
    }
}
