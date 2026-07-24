import SwiftUI
import WebKit

struct BrowserView: UIViewRepresentable {
    private static let fullscreenMessageName = "blackBeardFullscreen"

    @ObservedObject var controller: BrowserController

    func makeCoordinator() -> Coordinator {
        Coordinator(controller: controller)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.allowsInlineMediaPlayback = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

        let contentController = WKUserContentController()
        contentController.addUserScript(
            WKUserScript(
                source: BlockerRules.pageProtectionScript,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: false
            )
        )
        contentController.addUserScript(
            WKUserScript(
                source: BlockerRules.fullscreenTrackingScript,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: false
            )
        )
        contentController.add(context.coordinator, name: Self.fullscreenMessageName)
        configuration.userContentController = contentController

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic
        webView.isOpaque = true
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black

        controller.connect(webView)
        context.coordinator.installRulesAndLoad(in: webView)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler {
        private let controller: BrowserController
        private var fullscreenFrames = Set<String>()

        init(controller: BrowserController) {
            self.controller = controller
        }

        func userContentController(
            _ userContentController: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            guard message.name == BrowserView.fullscreenMessageName,
                  let payload = message.body as? [String: Any],
                  let frameID = payload["frameID"] as? String,
                  let fullscreen = payload["fullscreen"] as? Bool else {
                return
            }

            if fullscreen {
                fullscreenFrames.insert(frameID)
            } else {
                fullscreenFrames.remove(frameID)
            }

            controller.setFullscreen(!fullscreenFrames.isEmpty)
        }

        private func resetFullscreenState() {
            fullscreenFrames.removeAll()
            controller.setFullscreen(false)
        }

        func installRulesAndLoad(in webView: WKWebView) {
            guard let store = WKContentRuleListStore.default() else {
                controller.loadSelectedSite(in: webView)
                return
            }

            store.lookUpContentRuleList(forIdentifier: BlockerRules.identifier) { [weak self, weak webView] existingRules, _ in
                guard let self, let webView else { return }

                if let existingRules {
                    DispatchQueue.main.async {
                        webView.configuration.userContentController.add(existingRules)
                        self.controller.loadSelectedSite(in: webView)
                    }
                    return
                }

                store.compileContentRuleList(
                    forIdentifier: BlockerRules.identifier,
                    encodedContentRuleList: BlockerRules.json
                ) { [weak self, weak webView] compiledRules, _ in
                    DispatchQueue.main.async {
                        guard let self, let webView else { return }
                        if let compiledRules {
                            webView.configuration.userContentController.add(compiledRules)
                        }
                        self.controller.loadSelectedSite(in: webView)
                    }
                }
            }
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }

            if let scheme = url.scheme?.lowercased(), ["about", "blob", "data"].contains(scheme) {
                decisionHandler(.allow)
                return
            }

            let targetsMainFrame = navigationAction.targetFrame?.isMainFrame ?? true

            if targetsMainFrame && !controller.isAllowedTopLevelURL(url) {
                controller.recordBlockedNavigation()
                decisionHandler(.cancel)
                return
            }

            // A nil target frame is a request for a new window. Same-site links
            // stay in this one browser view; external popups are discarded.
            if navigationAction.targetFrame == nil {
                if controller.isAllowedTopLevelURL(url) {
                    webView.load(URLRequest(url: url))
                } else {
                    controller.recordBlockedNavigation()
                }
                decisionHandler(.cancel)
                return
            }

            decisionHandler(.allow)
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            guard let url = navigationAction.request.url,
                  controller.isAllowedTopLevelURL(url) else {
                controller.recordBlockedNavigation()
                return nil
            }

            webView.load(URLRequest(url: url))
            return nil
        }

        func webView(
            _ webView: WKWebView,
            runJavaScriptAlertPanelWithMessage message: String,
            initiatedByFrame frame: WKFrameInfo,
            completionHandler: @escaping () -> Void
        ) {
            controller.recordBlockedNavigation()
            completionHandler()
        }

        func webView(
            _ webView: WKWebView,
            runJavaScriptConfirmPanelWithMessage message: String,
            initiatedByFrame frame: WKFrameInfo,
            completionHandler: @escaping (Bool) -> Void
        ) {
            controller.recordBlockedNavigation()
            completionHandler(false)
        }

        func webView(
            _ webView: WKWebView,
            runJavaScriptTextInputPanelWithPrompt prompt: String,
            defaultText: String?,
            initiatedByFrame frame: WKFrameInfo,
            completionHandler: @escaping (String?) -> Void
        ) {
            controller.recordBlockedNavigation()
            completionHandler(nil)
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation?) {
            resetFullscreenState()
            controller.refreshState()
        }

        func webView(_ webView: WKWebView, didCommit navigation: WKNavigation?) {
            controller.refreshState()
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation?) {
            controller.refreshState()
        }

        func webView(
            _ webView: WKWebView,
            didFail navigation: WKNavigation?,
            withError error: Error
        ) {
            resetFullscreenState()
            controller.refreshState()
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation?,
            withError error: Error
        ) {
            resetFullscreenState()
            controller.refreshState()
        }

    }
}
