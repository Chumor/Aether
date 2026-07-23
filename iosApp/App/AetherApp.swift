import SwiftUI
import UIKit
import AetherShared

@main
struct AetherIOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea()
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .background:
                SharedApplicationLifecycle.shared.enterBackground()
            case .active:
                SharedApplicationLifecycle.shared.enterForeground()
            default:
                break
            }
        }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        FullscreenComposeViewController(
            content: MainViewControllerKt.MainViewController(runtimeHost: AetherRuntimeHost.shared)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class FullscreenComposeViewController: UIViewController {
    private let content: UIViewController

    init(content: UIViewController) {
        self.content = content
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear

        addChild(content)
        content.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(content.view)
        NSLayoutConstraint.activate([
            content.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            content.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            content.view.topAnchor.constraint(equalTo: view.topAnchor),
            content.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        content.didMove(toParent: self)
    }
}
