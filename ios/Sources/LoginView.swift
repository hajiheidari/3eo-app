import SwiftUI

struct LoginView: View {
    @EnvironmentObject var session: Session
    @State private var mobile = ""
    @State private var code = ""
    @State private var step = 0
    @State private var loading = false
    @State private var error: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Image("Logo").resizable().scaledToFit().frame(width: 190).padding(.top, 56)
                Text("ابزارهای سئو، همیشه همراه شما")
                    .font(.system(size: 15)).foregroundColor(Theme.muted).padding(.top, 6)
                Card {
                    VStack(alignment: .leading, spacing: 12) {
                        if step == 0 { mobileStep } else { codeStep }
                        if let e = error {
                            Text(e).font(.system(size: 13)).foregroundColor(Theme.red)
                        }
                    }
                }
                .padding(.top, 28)
            }
            .padding(24)
        }
        .background(Theme.bg.ignoresSafeArea())
    }

    private var mobileStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ورود یا ثبت‌نام").font(.system(size: 18, weight: .bold)).foregroundColor(Theme.ink)
            Text("شماره موبایل خود را وارد کنید تا کد تأیید برایتان پیامک شود.")
                .font(.system(size: 14)).foregroundColor(Theme.muted)
            TextField("مثلاً ۰۹۱۲۳۴۵۶۷۸۹", text: $mobile)
                .keyboardType(.phonePad)
                .padding(14)
                .background(Color(white: 0.97))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            PrimaryButton(title: "دریافت کد تأیید", loading: loading) { sendCode() }
            Text("با ورود، قوانین و حریم خصوصی 3eo.ir را می‌پذیرید.")
                .font(.system(size: 12)).foregroundColor(Theme.muted)
        }
    }

    private var codeStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("کد تأیید").font(.system(size: 18, weight: .bold)).foregroundColor(Theme.ink)
            Text("کد ۶ رقمی ارسال‌شده به \(Theme.fa(mobile)) را وارد کنید.")
                .font(.system(size: 14)).foregroundColor(Theme.muted)
            TextField("کد ۶ رقمی", text: $code)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.system(size: 22))
                .padding(14)
                .background(Color(white: 0.97))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            PrimaryButton(title: "ورود", loading: loading) { verify() }
            Button("ویرایش شماره") { step = 0; error = nil }
                .font(.system(size: 14, weight: .bold)).frame(maxWidth: .infinity)
        }
    }

    private func sendCode() {
        let m = mobile.trimmingCharacters(in: .whitespaces)
        guard m.count >= 10 else { error = "شماره موبایل را کامل وارد کنید."; return }
        loading = true; error = nil
        Task {
            do {
                _ = try await API.call("POST", "/api/app/login/send", body: ["mobile": m], as: Empty.self)
                step = 1
            } catch { self.error = error.localizedDescription }
            loading = false
        }
    }

    private func verify() {
        let c = code.trimmingCharacters(in: .whitespaces)
        guard c.count >= 6 else { error = "کد ۶ رقمی را کامل وارد کنید."; return }
        loading = true; error = nil
        Task {
            do {
                let r = try await API.call("POST", "/api/app/login/verify", body: ["mobile": mobile.trimmingCharacters(in: .whitespaces), "code": c], as: TokenResponse.self)
                session.set(r.token)
            } catch { self.error = error.localizedDescription }
            loading = false
        }
    }
}
