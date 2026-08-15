import Foundation

enum BlockerRules {
    static let identifier = "BlackBeardBlockerRules-v4"

    // Site-specific rules cover the rotating networks used by the movie sites.
    // Common ad and popup rules apply to every BlackBeard destination.
    // Top-level redirects are independently stopped by BrowserView.
    static let json = #"""
    [
      {
        "trigger": {
          "url-filter": "^https?://([^/]+\\.)?blirtonethe\\.com/.*",
          "if-domain": ["cinejoy.to", "*.cinejoy.to"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": "^https?://b\\.5ei1zlm7w7ut9bezxfj5\\.cfd/.*",
          "if-domain": ["cinejoy.to", "*.cinejoy.to"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": "^https?://([^/]+\\.)?(popads\\.net|popcash\\.net|propellerads\\.com|onclicka\\.com|adsterra\\.com|doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|butyrhopers\\.com|cutchbatete\\.com|rostelshute\\.shop|khalatisort\\.cyou|mrdreamzone\\.com|woolderstrolld\\.qpon)/.*",
          "if-domain": ["cinejoy.to", "*.cinejoy.to", "aether.bar", "*.aether.bar", "popcornmovies.io", "*.popcornmovies.io"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": "^https?://([^/]+\\.)?popcornmovies\\.io/api/ads.*",
          "if-domain": ["popcornmovies.io", "*.popcornmovies.io"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": ".*",
          "resource-type": ["popup"],
          "if-domain": ["cinejoy.to", "*.cinejoy.to", "aether.bar", "*.aether.bar", "popcornmovies.io", "*.popcornmovies.io"]
        },
        "action": { "type": "block" }
      }
    ]
    """#

    static let pageProtectionScript = #"""
    (() => {
      Object.defineProperty(window, "open", {
        configurable: false,
        enumerable: true,
        writable: false,
        value: function () { return null; }
      });

      const replaceDialog = (name, value) => {
        try {
          Object.defineProperty(window, name, {
            configurable: false,
            enumerable: true,
            writable: false,
            value
          });
        } catch (_) {
          window[name] = value;
        }
      };

      replaceDialog("alert", function () {});
      replaceDialog("confirm", function () { return false; });
      replaceDialog("prompt", function () { return null; });

      const cleanLinks = (root) => {
        if (!root || !root.querySelectorAll) return;
        root.querySelectorAll('a[target="_blank"], a[target="_new"]').forEach((link) => {
          link.removeAttribute("target");
          link.removeAttribute("rel");
        });
      };

      const isCompactAdOverlay = (element) => {
        if (!element || element.nodeType !== 1) return false;
        if (element.matches("[data-shb]")) return true;
        if (element.tagName !== "IFRAME") return false;

        const style = getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        const zIndex = Number.parseInt(style.zIndex, 10);
        return (style.position === "fixed" || style.position === "absolute")
          && Number.isFinite(zIndex)
          && zIndex >= 1000000
          && rect.width >= 100
          && rect.height >= 80
          && rect.width <= 620
          && rect.height <= 620;
      };

      const removeOverlay = (element) => {
        if (!isCompactAdOverlay(element)) return;
        element.remove();
      };

      const sweep = (root) => {
        if (!root || !root.querySelectorAll) return;
        cleanLinks(root);
        removeOverlay(root);
        root.querySelectorAll("[data-shb], iframe").forEach(removeOverlay);
      };

      sweep(document);

      const observationRoot = document.documentElement || document;
      new MutationObserver((mutations) => {
        for (const mutation of mutations) {
          if (mutation.type === "attributes") {
            removeOverlay(mutation.target);
          } else {
            for (const node of mutation.addedNodes) sweep(node);
          }
        }
      }).observe(observationRoot, {
        attributes: true,
        attributeFilter: ["class", "style", "data-shb"],
        childList: true,
        subtree: true
      });

      document.addEventListener("click", (event) => {
        const link = event.target && event.target.closest
          ? event.target.closest('a[target="_blank"], a[target="_new"]')
          : null;
        if (link) link.removeAttribute("target");
      }, true);
    })();
    """#

    static let fullscreenTrackingScript = #"""
    (() => {
      const handler = window.webkit
        && window.webkit.messageHandlers
        && window.webkit.messageHandlers.blackBeardFullscreen;

      if (!handler) return;

      const frameID = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      let lastReportedState = null;
      let reportScheduled = false;

      const report = (fullscreen) => {
        const nextState = Boolean(fullscreen);
        if (nextState === lastReportedState) return;
        lastReportedState = nextState;
        handler.postMessage({ frameID, fullscreen: nextState });
      };

      const isVisible = (element, minimumWidth, minimumHeight) => {
        const rect = element.getBoundingClientRect();
        const style = getComputedStyle(element);
        return rect.width >= minimumWidth
          && rect.height >= minimumHeight
          && style.display !== "none"
          && style.visibility !== "hidden"
          && Number.parseFloat(style.opacity || "1") > 0;
      };

      const hasActiveVideo = () => Array.from(document.querySelectorAll("video"))
        .some((video) => !video.paused && !video.ended && isVisible(video, 180, 100));

      const hasEmbeddedPlayer = () => {
        const host = location.hostname.toLowerCase();
        const path = location.pathname;
        const isPlayerPage =
          ((host === "aether.bar" || host.endsWith(".aether.bar")) && path.startsWith("/media/"))
          || ((host === "popcornmovies.io" || host.endsWith(".popcornmovies.io"))
            && path.startsWith("/watch/"));
        return isPlayerPage
          && Array.from(document.querySelectorAll("iframe"))
            .some((frame) => isVisible(frame, 240, 130));
      };

      const reportDocumentState = () => {
        report(Boolean(
          document.fullscreenElement
          || document.webkitFullscreenElement
          || hasActiveVideo()
          || hasEmbeddedPlayer()
        ));
      };

      const scheduleReport = () => {
        if (reportScheduled) return;
        reportScheduled = true;
        requestAnimationFrame(() => {
          reportScheduled = false;
          reportDocumentState();
        });
      };

      document.addEventListener("fullscreenchange", scheduleReport, true);
      document.addEventListener("webkitfullscreenchange", scheduleReport, true);
      ["play", "playing", "pause", "ended", "emptied", "abort"].forEach((eventName) => {
        document.addEventListener(eventName, scheduleReport, true);
      });

      // iPhone's native video player uses these WebKit-specific events.
      document.addEventListener("webkitbeginfullscreen", () => report(true), true);
      document.addEventListener("webkitendfullscreen", scheduleReport, true);

      new MutationObserver(scheduleReport).observe(
        document.documentElement || document,
        { childList: true, subtree: true, attributes: true, attributeFilter: ["class", "style", "src"] }
      );

      window.addEventListener("pagehide", () => report(false), true);
      window.addEventListener("resize", scheduleReport, true);
      scheduleReport();
    })();
    """#
}
