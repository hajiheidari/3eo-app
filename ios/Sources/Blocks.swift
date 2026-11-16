import SwiftUI

/// Draws the server's result "blocks" natively.
struct BlocksView: View {
    let blocks: [Block]
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(blocks) { b in one(b) }
        }
    }

    @ViewBuilder private func one(_ b: Block) -> some View {
        switch b.t {
        case "score":
            Card {
                VStack(spacing: 6) {
                    ScoreGauge(score: b.score)
                    Text(b.label).font(.system(size: 13, weight: .bold)).foregroundColor(Theme.muted)
                }.frame(maxWidth: .infinity)
            }
        case "counts":
            HStack(spacing: 8) {
                count(b.pass, "موفق", Theme.green); count(b.warn, "هشدار", Theme.amber); count(b.fail, "خطا", Theme.red)
            }
        case "kv":
            Card {
                VStack(alignment: .leading, spacing: 9) {
                    ForEach(Array(b.rows.enumerated()), id: \.offset) { i, r in
                        if i > 0 { Divider() }
                        HStack(alignment: .top) {
                            Text(r.first ?? "").font(.system(size: 13)).foregroundColor(Theme.muted)
                            Spacer(minLength: 12)
                            Text(Theme.fa(r.count > 1 ? r[1] : "")).font(.system(size: 14, weight: .bold)).foregroundColor(Theme.ink)
                                .multilineTextAlignment(.trailing)
                        }
                    }
                }
            }
        case "list":
            VStack(alignment: .leading, spacing: 8) {
                if !b.title.isEmpty { Text(b.title).font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink) }
                ForEach(b.items) { it in
                    Card {
                        HStack(alignment: .center) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(it.title).font(.system(size: 14, weight: .bold)).foregroundColor(Theme.ink)
                                    .environment(\.layoutDirection, it.ltr ? .leftToRight : .rightToLeft)
                                if !it.sub.isEmpty { Text(it.sub).font(.system(size: 12)).foregroundColor(Theme.muted) }
                            }
                            Spacer(minLength: 8)
                            if !it.tag.isEmpty {
                                Text(Theme.fa(it.tag)).font(.system(size: 12, weight: .bold)).foregroundColor(Theme.tone(it.tone))
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(Theme.tone(it.tone).opacity(0.12)).clipShape(Capsule())
                            }
                        }
                    }
                }
            }
        case "chips":
            VStack(alignment: .leading, spacing: 8) {
                if !b.title.isEmpty { Text(b.title).font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink) }
                Card { FlowLayout(spacing: 6) {
                    ForEach(b.items) { it in
                        Text(it.title).font(.system(size: 13)).foregroundColor(Theme.greenDark)
                            .padding(.horizontal, 10).padding(.vertical, 5)
                            .background(Theme.greenSoft).clipShape(Capsule())
                    }
                } }
            }
        case "copy":
            CopyCard(title: b.title, text: b.text, ltr: b.ltr)
        case "bars":
            VStack(alignment: .leading, spacing: 8) {
                if !b.title.isEmpty { Text(b.title).font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink) }
                Card {
                    VStack(spacing: 10) {
                        ForEach(b.items) { it in
                            VStack(spacing: 4) {
                                HStack {
                                    Text(it.label).font(.system(size: 13)).foregroundColor(Theme.ink)
                                    Spacer()
                                    Text(Theme.fa(it.text)).font(.system(size: 13, weight: .bold)).foregroundColor(Theme.muted)
                                }
                                GeometryReader { g in
                                    ZStack(alignment: .leading) {
                                        Capsule().fill(Theme.line)
                                        Capsule().fill(it.max == 100 ? Theme.score(Int(it.value)) : Theme.green)
                                            .frame(width: max(8, g.size.width * CGFloat(min(1, it.value / Swift.max(1, it.max)))))
                                    }
                                }.frame(height: 8)
                            }
                        }
                    }
                }
            }
        case "checks":
            ChecksView(categories: b.categories)
        case "p":
            Text(b.text).font(.system(size: 13)).foregroundColor(b.muted ? Theme.muted : Theme.ink)
        case "h":
            Text(b.text).font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink)
        default:
            EmptyView()
        }
    }

    private func count(_ n: Int, _ label: String, _ c: Color) -> some View {
        VStack(spacing: 2) {
            Text(Theme.fa(String(n))).font(.system(size: 22, weight: .bold)).foregroundColor(c)
            Text(label).font(.system(size: 12)).foregroundColor(Theme.muted)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 10)
        .background(Color.white).clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Theme.line, lineWidth: 1))
    }
}

struct CopyCard: View {
    let title: String
    let text: String
    let ltr: Bool
    @State private var copied = false
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 8) {
                Text(title).font(.system(size: 14, weight: .bold)).foregroundColor(Theme.ink)
                Text(text).font(.system(size: 13)).foregroundColor(Theme.ink).textSelection(.enabled)
                    .environment(\.layoutDirection, ltr ? .leftToRight : .rightToLeft)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(10)
                    .background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                Button {
                    UIPasteboard.general.string = text
                    copied = true
                } label: {
                    Text(copied ? "کپی شد ✓" : "کپی").font(.system(size: 14, weight: .bold)).foregroundColor(Theme.green)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.green, lineWidth: 1))
                }
            }
        }
    }
}

struct ChecksView: View {
    let categories: [Category]
    var body: some View {
        let issues = categories.flatMap { $0.checks }.filter { $0.status != "pass" }.sorted { $0.gain > $1.gain }
        VStack(alignment: .leading, spacing: 8) {
            if !issues.isEmpty {
                Text("مهم‌ترین کارها برای بهتر شدن").font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink)
                ForEach(Array(issues.prefix(8))) { IssueCard(check: $0) }
            }
            if !categories.isEmpty {
                Text("همه‌ی بخش‌ها").font(.system(size: 16, weight: .bold)).foregroundColor(Theme.ink).padding(.top, 10)
                ForEach(categories) { CategoryCard(cat: $0) }
            }
        }
    }
}

/// simple wrapping row layout for chips
struct FlowLayout: Layout {
    var spacing: CGFloat = 6
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let w = proposal.width ?? 300
        var x: CGFloat = 0, y: CGFloat = 0, rowH: CGFloat = 0
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if x + sz.width > w && x > 0 { x = 0; y += rowH + spacing; rowH = 0 }
            x += sz.width + spacing; rowH = max(rowH, sz.height)
        }
        return CGSize(width: w, height: y + rowH)
    }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.maxX, y = bounds.minY, rowH: CGFloat = 0
        let rtl = true
        for s in subviews {
            let sz = s.sizeThatFits(.unspecified)
            if rtl {
                if x - sz.width < bounds.minX && x < bounds.maxX { x = bounds.maxX; y += rowH + spacing; rowH = 0 }
                s.place(at: CGPoint(x: x - sz.width, y: y), proposal: ProposedViewSize(sz))
                x -= sz.width + spacing
            }
            rowH = max(rowH, sz.height)
        }
    }
}
