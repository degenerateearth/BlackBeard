import SwiftUI

struct ContentView: View {
    @StateObject private var browser = BrowserController()

    var body: some View {
        Group {
            if browser.selectedSite == nil {
                landingScreen
            } else {
                BrowserView(controller: browser)
                    .ignoresSafeArea(.container, edges: .top)
                    .safeAreaInset(edge: .bottom, spacing: 0) {
                        if !browser.isFullscreen {
                            browserBar
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                        }
                    }
                    .animation(.easeInOut(duration: 0.18), value: browser.isFullscreen)
            }
        }
        .background(Color.black)
        .preferredColorScheme(.dark)
    }

    private var landingScreen: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: 18) {
                    Spacer(minLength: 28)

                    Image("BlackBeardLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: min(proxy.size.width * 0.42, 190))
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .accessibilityHidden(true)

                    VStack(spacing: 4) {
                        Text("BlackBeard")
                            .font(.largeTitle.weight(.bold))
                        Text("Choose your course")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    VStack(spacing: 12) {
                        ForEach(BrowserSite.allCases) { site in
                            Button {
                                browser.select(site)
                            } label: {
                                HStack {
                                    Text(site.title)
                                        .font(.headline)
                                    Spacer()
                                    Image(systemName: "arrow.up.right")
                                        .font(.subheadline.weight(.semibold))
                                }
                                .foregroundStyle(Color(red: 0.78, green: 0.61, blue: 0.32))
                                .padding(.horizontal, 20)
                                .frame(maxWidth: 420, minHeight: 54)
                                .background(Color.white.opacity(0.035))
                                .overlay {
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .stroke(Color(red: 0.53, green: 0.39, blue: 0.20), lineWidth: 1)
                                }
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .buttonStyle(.plain)
                            .accessibilityHint("Opens \(site.url.host ?? site.title) in the protected browser")
                        }
                    }
                    .padding(.horizontal, 24)

                    Spacer(minLength: 28)
                }
                .frame(maxWidth: .infinity)
                .frame(minHeight: proxy.size.height)
            }
            .background(Color.black)
        }
        .ignoresSafeArea(.container, edges: .bottom)
    }

    private var browserBar: some View {
        HStack(spacing: 10) {
            browserButton("chevron.backward", label: "Back", enabled: browser.canGoBack) {
                browser.goBack()
            }

            browserButton("chevron.forward", label: "Forward", enabled: browser.canGoForward) {
                browser.goForward()
            }

            Spacer(minLength: 6)

            Label("BlackBeard", systemImage: "shield.lefthalf.filled")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
                .accessibilityLabel("BlackBeard protected browser")

            if browser.blockedNavigationCount > 0 {
                Text("\(browser.blockedNavigationCount)")
                    .font(.caption2.monospacedDigit().weight(.bold))
                    .foregroundStyle(.black)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(.green, in: Capsule())
                    .accessibilityLabel("\(browser.blockedNavigationCount) blocked popups or redirects")
            }

            Spacer(minLength: 6)

            browserButton("square.grid.2x2", label: "Main menu", enabled: true) {
                browser.returnToMenu()
            }

            browserButton(browser.isLoading ? "xmark" : "arrow.clockwise", label: browser.isLoading ? "Stop" : "Reload", enabled: true) {
                browser.isLoading ? browser.stopLoading() : browser.reload()
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Divider()
        }
    }

    private func browserButton(
        _ systemName: String,
        label: String,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.body.weight(.semibold))
                .frame(width: 34, height: 34)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(enabled ? Color.primary : Color.secondary.opacity(0.35))
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}
