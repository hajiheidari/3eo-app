import SwiftUI

@main
struct ThreeEOApp: App {
    @StateObject private var session = Session.shared

    var body: some Scene {
        WindowGroup {
            Group {
                if session.token == nil {
                    LoginView()
                } else {
                    MainTabs()
                }
            }
            .environmentObject(session)
            .environment(\.layoutDirection, .rightToLeft)
            .tint(Theme.green)
        }
    }
}
