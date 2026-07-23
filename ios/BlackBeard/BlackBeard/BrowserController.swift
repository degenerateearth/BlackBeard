import Foundation
import WebKit

enum BrowserSite: String, CaseIterable, Identifiable {
    case cineby
    case aether
    case popcorn

    var id: String { rawValue }

    var title: String {
        switch self {
        case .cineby: "Cineby"
        case .aether: "Aether"
        case .popcorn: "Popcorn"
        }
    }

    var url: URL {
        switch self {
        case .cineby: URL(string: "https://cineby.at")!
        case .aether: URL(string: "https://aether.bar/")!
        case .popcorn: URL(string: "https://popcornmovies.io/")!
        }
    }

    private var rootHost: String {
        switch self {
        case .cineby: "cineby.at"
        case .aether: "aether.bar"
        case .popcorn: "popcornmovies.io"
        }
    }

    func includes(host: String) -> Bool {
        let normalizedHost = host.lowercased()
        return normalizedHost == rootHost || normalizedHost.hasSuffix(".\(rootHost)")
    }
}

final class BrowserController: ObservableObject {
    @Published private(set) var selectedSite: BrowserSite?
    @Published private(set) var canGoBack = false
    @Published private(set) var canGoForward = false
    @Published private(set) var isLoading = false
    @Published private(set) var isFullscreen = false
    @Published private(set) var blockedNavigationCount = 0

    weak var webView: WKWebView?

    func select(_ site: BrowserSite) {
        blockedNavigationCount = 0
        selectedSite = site
    }

    func connect(_ webView: WKWebView) {
        self.webView = webView
        refreshState()
    }

    func refreshState() {
        guard let webView else { return }
        canGoBack = webView.canGoBack
        canGoForward = webView.canGoForward
        isLoading = webView.isLoading
    }

    func recordBlockedNavigation() {
        blockedNavigationCount += 1
    }

    func setFullscreen(_ fullscreen: Bool) {
        isFullscreen = fullscreen
    }

    func goBack() {
        webView?.goBack()
    }

    func goForward() {
        webView?.goForward()
    }

    func reload() {
        guard let webView else { return }
        if webView.url == nil {
            loadSelectedSite(in: webView)
        } else {
            webView.reload()
        }
    }

    func stopLoading() {
        webView?.stopLoading()
        refreshState()
    }

    func returnToMenu() {
        webView?.stopLoading()
        webView = nil
        canGoBack = false
        canGoForward = false
        isLoading = false
        isFullscreen = false
        selectedSite = nil
    }

    func loadSelectedSite(in webView: WKWebView) {
        guard let selectedSite else { return }
        webView.load(URLRequest(url: selectedSite.url))
    }

    func isAllowedTopLevelURL(_ url: URL) -> Bool {
        guard url.scheme?.lowercased() == "https",
              let host = url.host,
              let selectedSite else {
            return false
        }

        return selectedSite.includes(host: host)
    }
}
