import SwiftUI

@main
struct ThreeEOApp: App {
    @StateObject private var session = Session.shared

    var body: some Scene {
        WindowGroup {
            Group {
                if Demo.on {
                    demoRoot
                } else if session.token == nil {
                    LoginView()
                } else {
                    MainTabs()
                }
            }
            .environmentObject(session)
            .environment(\.layoutDirection, .rightToLeft)
            .tint(Theme.green)
            .font(.vz(15, false))
        }
    }

    @ViewBuilder private var demoRoot: some View {
        switch Demo.screen {
        case "report": ReportView(report: (try? Demo.respond("/api/app/report/a1", as: Presented.self)) ?? Presented.empty)
        case "rank": RankView()
        case "monitor": MonitorView()
        case "content": ContentJobView(start: CSheet(jobId: 1), cfg: nil)
        default: MainTabs()
        }
    }
}
