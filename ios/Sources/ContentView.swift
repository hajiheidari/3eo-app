import SwiftUI
import UIKit

struct CRecent: Decodable, Identifiable {
    let id: Int
    let kind: String
    let status: String
    let title: String
    let name: String
}
struct CCost: Decodable { let topics: Int, outline: Int, article: Int }
struct CConfig: Decodable {
    let recent: [CRecent]
    let free: Bool
    let credits: Int
    let cost: CCost
    let lengths: [Int]
    let tones: [[String]]
}
struct CStart: Decodable { let id: Int }
struct CTopic: Decodable, Identifiable {
    let title: String
    let keyword: String
    let intent: String
    let angle: String
    var id: String { title }
}
struct CArticle: Decodable { let title: String; let meta: String; let body: String; let words: Int }
struct CJob: Decodable {
    let kind: String
    let status: String
    let title: String
    let error: String
    let free: Bool
    let topics: [CTopic]?
    let outline: String?
    let article: CArticle?
}

struct CSheet: Identifiable {
    let id = UUID()
    var jobId: Int = 0
    var topic = ""
    var keyword = ""
    var form = false
}

private func cField<V: View>(_ v: V) -> some View {
    v.font(.vz(14, false)).padding(12).background(Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line)).clipShape(RoundedRectangle(cornerRadius: 12))
}

private func cButton(_ t: String, _ filled: Bool) -> some View {
    Text(t).font(.vz(14, true)).foregroundColor(filled ? .white : Theme.green)
        .frame(maxWidth: .infinity).frame(height: 46)
        .background(filled ? Theme.green : Color.white)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(filled ? Color.clear : Theme.green.opacity(0.5)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
}

struct ContentHomeView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var cfg: CConfig?
    @State private var seed = ""
    @State private var aud = ""
    @State private var topic = ""
    @State private var kw = ""
    @State private var error: String?
    @State private var sheet: CSheet?

    private func cost(_ n: Int) -> String { (cfg?.free ?? false) ? "رایگان" : Theme.fa(String(n)) + " اعتبار" }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }.font(.vz(15, true))
                Spacer()
                Text("تولید محتوا").font(.vz(16, true)).foregroundColor(Theme.ink)
                Spacer()
                Text("").frame(width: 60)
            }.padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let c = cfg {
                        Text("از پیدا کردن موضوع تا مقاله‌ی کامل فارسی و سئوشده. اعتبار شما: " + Theme.fa(String(c.credits))).font(.vz(12, false)).foregroundColor(Theme.muted)
                        if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("۱) پیشنهاد موضوع").font(.vz(15, true)).foregroundColor(Theme.ink)
                                cField(TextField("حوزه یا کلمه‌ی کلیدی (مثلاً سئو سایت)", text: $seed))
                                cField(TextField("مخاطب (اختیاری)", text: $aud))
                                Button { topics() } label: { cButton("پیشنهاد ۱۰ موضوع · " + cost(c.cost.topics), true) }
                            }
                        }
                        Card {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("یا موضوع خودتان را دارید؟").font(.vz(15, true)).foregroundColor(Theme.ink)
                                cField(TextField("موضوع مقاله", text: $topic))
                                cField(TextField("کلمه‌ی کلیدی اصلی", text: $kw))
                                Button { sheet = CSheet(topic: topic, keyword: kw, form: true) } label: { cButton("ساخت ساختار مقاله · " + cost(c.cost.outline), false) }
                            }
                        }
                        Text("کارهای اخیر").font(.vz(17, true)).foregroundColor(Theme.ink).padding(.top, 8)
                        if c.recent.isEmpty { Text("هنوز کاری ندارید.").font(.vz(13, false)).foregroundColor(Theme.muted) }
                        ForEach(c.recent) { r in
                            Button { sheet = CSheet(jobId: r.id) } label: {
                                Card {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(Theme.bidi(r.title)).font(.vz(14, true)).foregroundColor(Theme.ink)
                                            Text(r.name).font(.vz(11, false)).foregroundColor(Theme.muted)
                                        }
                                        Spacer()
                                        Text(r.status == "done" ? "آماده" : (r.status == "failed" ? "ناموفق" : "در حال انجام…")).font(.vz(12, true))
                                            .foregroundColor(r.status == "done" ? Theme.green : (r.status == "failed" ? Theme.red : Theme.amber))
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
        .environment(\.layoutDirection, .rightToLeft)
        .task { await load() }
        .fullScreenCover(item: $sheet, onDismiss: { Task { await load() } }) { s in ContentJobView(start: s, cfg: cfg) }
    }

    private func load() async {
        if let c = try? await API.call("GET", "/api/app/content", as: CConfig.self) { cfg = c }
    }

    private func topics() {
        let b: [String: Any] = ["seed": seed, "audience": aud]
        Task {
            do {
                let r = try await API.call("POST", "/api/app/content/topics", body: b, as: CStart.self)
                error = nil
                sheet = CSheet(jobId: r.id)
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct ContentJobView: View {
    let start: CSheet
    let cfg: CConfig?
    @Environment(\.dismiss) private var dismiss
    @State private var jobId = 0
    @State private var job: CJob?
    @State private var failedMsg: String?
    @State private var form = false
    @State private var topic = ""
    @State private var keyword = ""
    @State private var aud = ""
    @State private var notes = ""
    @State private var tone = "friendly"
    @State private var words = 900
    @State private var outline = ""
    @State private var error: String?
    @State private var copied = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("‹  بازگشت") { dismiss() }.font(.vz(15, true))
                Spacer()
            }.padding(14).background(Color.white)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if form { formView } else if let f = failedMsg {
                        Text("انجام نشد؛ اعتبار برگشت داده شد.").font(.vz(15, true)).foregroundColor(Theme.red)
                        Text(f).font(.vz(12, false)).foregroundColor(Theme.muted)
                    } else if let j = job { result(j) } else {
                        Text("در حال نوشتن… معمولاً چند دقیقه طول می‌کشد. می‌توانید بیرون بروید؛ نتیجه در «کارهای اخیر» می‌ماند.")
                            .font(.vz(14, false)).foregroundColor(Theme.ink)
                        ProgressView().frame(maxWidth: .infinity).padding(.top, 10)
                    }
                    if let e = error { Text(e).font(.vz(13, true)).foregroundColor(Theme.red) }
                }
                .padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        .environment(\.layoutDirection, .rightToLeft)
        .onAppear {
            if start.form { form = true; topic = start.topic; keyword = start.keyword; if cfg?.lengths.contains(900) == false, let l = cfg?.lengths.first { words = l } }
            else { jobId = start.jobId }
        }
        .task(id: jobId) { await poll() }
    }

    private var formView: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ساختار مقاله").font(.vz(17, true)).foregroundColor(Theme.ink)
            cField(TextField("موضوع مقاله", text: $topic))
            cField(TextField("کلمه‌ی کلیدی اصلی", text: $keyword))
            cField(TextField("مخاطب (اختیاری)", text: $aud))
            Text("لحن نوشته").font(.vz(12, false)).foregroundColor(Theme.muted)
            Picker("", selection: $tone) {
                ForEach(cfg?.tones ?? [], id: \.self) { t in Text(t.count > 1 ? t[1] : "").tag(t.first ?? "") }
            }.pickerStyle(.menu).tint(Theme.green)
            Text("طول مقاله").font(.vz(12, false)).foregroundColor(Theme.muted)
            Picker("", selection: $words) {
                ForEach(cfg?.lengths ?? [900], id: \.self) { w in Text("حدود " + Theme.fa(String(w)) + " کلمه").tag(w) }
            }.pickerStyle(.segmented)
            cField(TextField("نکات و واقعیت‌های معتبر شما (اختیاری)", text: $notes, axis: .vertical).lineLimit(3...6))
            Button { makeOutline() } label: { cButton("ساخت ساختار · " + ((cfg?.free ?? false) ? "رایگان" : Theme.fa(String(cfg?.cost.outline ?? 0)) + " اعتبار"), true) }
        }
    }

    @ViewBuilder private func result(_ j: CJob) -> some View {
        if j.kind == "topics" {
            Text("یکی را انتخاب کنید تا ساختار مقاله‌اش ساخته شود:").font(.vz(13, false)).foregroundColor(Theme.muted)
            ForEach(j.topics ?? []) { t in
                Button { topic = t.title; keyword = t.keyword; form = true } label: {
                    Card {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(Theme.bidi(t.title)).font(.vz(15, true)).foregroundColor(Theme.ink)
                            Text(Theme.bidi(t.keyword) + " · " + t.intent).font(.vz(12, true)).foregroundColor(Theme.green)
                            Text(Theme.bidi(t.angle)).font(.vz(12, false)).foregroundColor(Theme.muted)
                        }
                    }
                }.buttonStyle(.plain)
            }
        } else if j.kind == "outline" {
            Text("ساختار را می‌توانید ویرایش کنید (سرتیترها با «## » شروع می‌شوند)، بعد مقاله نوشته می‌شود.").font(.vz(12, false)).foregroundColor(Theme.muted)
            cField(TextEditor(text: $outline).frame(minHeight: 260).scrollContentBackground(.hidden))
                .onAppear { if outline.isEmpty { outline = j.outline ?? "" } }
            Button { makeArticle() } label: { cButton("نوشتن مقاله · " + (j.free ? "رایگان" : Theme.fa(String(cfg?.cost.article ?? 0)) + " اعتبار"), true) }
        } else if let a = j.article {
            Text(Theme.bidi(a.title)).font(.vz(20, true)).foregroundColor(Theme.ink)
            Text("حدود " + Theme.fa(String(a.words)) + " کلمه").font(.vz(12, false)).foregroundColor(Theme.muted)
            if !a.meta.isEmpty {
                Card {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("توضیح متا").font(.vz(12, true)).foregroundColor(Theme.green)
                        Text(a.meta).font(.vz(13, false)).foregroundColor(Theme.ink)
                    }
                }
            }
            Text(a.body).font(.vz(15, false)).foregroundColor(Theme.ink).lineSpacing(5).textSelection(.enabled)
            Button {
                UIPasteboard.general.string = a.title + "\n\n" + a.body
                copied = true
            } label: { cButton(copied ? "کپی شد ✓" : "کپی کل مقاله", true) }
        }
    }

    private func poll() async {
        guard jobId > 0, !form else { return }
        for _ in 0..<120 {
            guard let j = try? await API.call("GET", "/api/app/content/\(jobId)", as: CJob.self) else {
                try? await Task.sleep(nanoseconds: 4_000_000_000); continue
            }
            if j.status == "done" { job = j; return }
            if j.status == "failed" { failedMsg = j.error; return }
            try? await Task.sleep(nanoseconds: 5_000_000_000)
            if Task.isCancelled { return }
        }
    }

    private func reset(_ id: Int) {
        job = nil; failedMsg = nil; outline = ""; error = nil; form = false; jobId = id
    }

    private func makeOutline() {
        let b: [String: Any] = ["topic": topic, "keyword": keyword, "audience": aud, "notes": notes, "tone": tone, "words": words]
        Task {
            do { let r = try await API.call("POST", "/api/app/content/outline", body: b, as: CStart.self); reset(r.id) }
            catch { self.error = error.localizedDescription }
        }
    }

    private func makeArticle() {
        let id = jobId
        Task {
            do { let r = try await API.call("POST", "/api/app/content/\(id)/article", body: ["outline": outline], as: CStart.self); reset(r.id) }
            catch { self.error = error.localizedDescription }
        }
    }
}
