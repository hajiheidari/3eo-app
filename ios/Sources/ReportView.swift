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
                Text(Theme.fa(String(score))).font(.system(size: 40, weight: .bold)).foregroundColor(Theme.ink)
                Text("از ۱۰۰").font(.system(size: 13)).foregroundColor(Theme.muted)
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
            .font(.system(size: 13, weight: .bold)).foregroundColor(color)
            .padding(.horizontal, 12).padding(.vertical, 5)
            .overlay(Capsule().stroke(color, lineWidth: 1))
    }
}

struct IssueCard: View {
    let check: Check
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 6) {
                Text((check.status == "warn" ? "⚠  " : "✕  ") + check.title)
                    .font(.system(size: 15, weight: .bold)).foregroundColor(Theme.status(check.status))
                if !check.detail.isEmpty {
                    Text(check.detail).font(.system(size: 13)).foregroundColor(Theme.muted)
                }
                if !check.fix.isEmpty {
                    Text("راه‌حل: " + check.fix)
                        .font(.system(size: 13)).foregroundColor(Theme.greenDark)
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
                        Text(cat.title).font(.system(size: 15, weight: .bold)).foregroundColor(Theme.ink)
                        Spacer()
                        Text(Theme.fa(String(cat.score)) + "٪  " + (open ? "▴" : "▾"))
                            .font(.system(size: 15, weight: .bold)).foregroundColor(Theme.score(cat.score))
                    }
                }
                if open {
                    ForEach(cat.checks) { c in
                        VStack(alignment: .leading, spacing: 2) {
                            Text((c.status == "pass" ? "✓  " : (c.status == "warn" ? "⚠  " : "✕  ")) + c.title)
                                .font(.system(size: 14)).foregroundColor(Theme.status(c.status))
                            if !c.detail.isEmpty {
                                Text(c.detail).font(.system(size: 12)).foregroundColor(Theme.muted)
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
    let report: Report
    @State private var showShare = false
    @State private var web: WebItem?
    @Environment(\.dismiss) private var dismiss

    private var issues: [Check] {
        report.categories.flatMap { $0.checks }.filter { $0.status != "pass" }.sorted { $0.gain > $1.gain }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }
                Spacer()
                Button("اشتراک‌گذاری") { showShare = true }
            }
            .font(.system(size: 15, weight: .bold))
            .padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    Card {
                        VStack(spacing: 8) {
                            Text(report.host).font(.system(size: 17, weight: .bold)).foregroundColor(Theme.ink)
                            ScoreGauge(score: report.score)
                            HStack {
                                Pill(label: "موفق", n: report.counts.pass, color: Theme.green)
                                Pill(label: "هشدار", n: report.counts.warn, color: Theme.amber)
                                Pill(label: "خطا", n: report.counts.fail, color: Theme.red)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    if !issues.isEmpty {
                        Text("مهم‌ترین کارها برای بهتر شدن امتیاز")
                            .font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink).padding(.top, 12)
                        ForEach(Array(issues.prefix(8))) { IssueCard(check: $0) }
                    }
                    Text("همه‌ی بخش‌ها").font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink).padding(.top, 14)
                    ForEach(report.categories) { CategoryCard(cat: $0) }
                    Button("گزارش کامل در وب") {
                        if let u = URL(string: report.share) { web = WebItem(url: u) }
                    }
                    .font(.system(size: 15, weight: .bold)).frame(maxWidth: .infinity, minHeight: 44)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green, lineWidth: 1))
                    .padding(.top, 12)
                }
                .padding(16)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .sheet(isPresented: $showShare) { ShareSheet(items: ["گزارش سئوی \(report.host)\n\(report.share)"]) }
        .fullScreenCover(item: $web) { WebSheet(item: $0) }
    }
}

struct ReportLoader: View {
    let token: String
    @State private var report: Report?
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
            do { report = try await API.call("GET", "/api/app/report/" + token, as: Report.self) }
            catch { self.error = error.localizedDescription }
        }
    }
}
