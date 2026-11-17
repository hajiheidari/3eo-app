import SwiftUI

/// switches the app language (shows the other language's name) and rebuilds the UI
struct LangButton: View {
    @EnvironmentObject var session: Session

    var body: some View {
        Button {
            session.setLang(L.isFa ? "en" : "fa")
        } label: {
            Text(L.isFa ? "English" : "فارسی").font(.vz(15, true)).foregroundColor(Theme.ink)
                .frame(maxWidth: .infinity, minHeight: 44)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Theme.ink, lineWidth: 1))
        }
    }
}
