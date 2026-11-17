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
                Text(L.t("ابزارهای سئو، همیشه همراه شما"))
                    .font(.vz(15, false)).foregroundColor(Theme.muted).padding(.top, 6)
                Card {
                    VStack(alignment: .leading, spacing: 12) {
                        if step == 0 { mobileStep } else { codeStep }
                        if let e = error {
                            Text(e).font(.vz(13, false)).foregroundColor(Theme.red)
                        }
                    }
                }
                .padding(.top, 28)
                LangButton().padding(.top, 16)
            }
            .padding(24)
        }
        .background(Theme.bg.ignoresSafeArea())
    }

    private var mobileStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L.t("ورود یا ثبت‌نام")).font(.vz(18, true)).foregroundColor(Theme.ink)
            Text(L.t("شماره موبایل خود را وارد کنید تا کد تأیید برایتان پیامک شود."))
                .font(.vz(14, false)).foregroundColor(Theme.muted)
            TextField(L.t("مثلاً ۰۹۱۲۳۴۵۶۷۸۹"), text: $mobile)
                .keyboardType(.phonePad)
                .padding(14)
                .background(Color(white: 0.97))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            PrimaryButton(title: L.t("دریافت کد تأیید"), loading: loading) { sendCode() }
            Text(L.t("با ورود، قوانین و حریم خصوصی 3eo.ir را می‌پذیرید."))
                .font(.vz(12, false)).foregroundColor(Theme.muted)
        }
    }

    private var codeStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L.t("کد تأیید")).font(.vz(18, true)).foregroundColor(Theme.ink)
            Text(L.isFa ? "کد ۶ رقمی ارسال‌شده به \(Theme.fa(mobile)) را وارد کنید." : "6-digit code sent to \(mobile) — enter it below.")
                .font(.vz(14, false)).foregroundColor(Theme.muted)
            TextField(L.t("کد ۶ رقمی"), text: $code)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.vz(22, false))
                .padding(14)
                .background(Color(white: 0.97))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.line, lineWidth: 1))
            PrimaryButton(title: L.t("ورود"), loading: loading) { verify() }
            Button(L.t("ویرایش شماره")) { step = 0; error = nil }
                .font(.vz(14, true)).frame(maxWidth: .infinity)
        }
    }

    private func sendCode() {
        let m = mobile.trimmingCharacters(in: .whitespaces)
        guard m.count >= 10 else { error = L.t("شماره موبایل را کامل وارد کنید."); return }
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
        guard c.count >= 6 else { error = L.t("کد ۶ رقمی را کامل وارد کنید."); return }
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
