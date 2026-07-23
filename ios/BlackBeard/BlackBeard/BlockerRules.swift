import Foundation

enum BlockerRules {
    static let identifier = "BlackBeardBlockerRules-v2"

    // Site-specific rules cover the rotating networks observed on Cineby.
    // Common ad and popup rules apply to every BlackBeard destination.
    // Top-level redirects are independently stopped by BrowserView.
    static let json = #"""
    [
      {
        "trigger": {
          "url-filter": "^https?://([^/]+\\.)?blirtonethe\\.com/.*",
          "if-domain": ["cineby.at", "*.cineby.at"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": "^https?://b\\.5ei1zlm7w7ut9bezxfj5\\.cfd/.*",
          "if-domain": ["cineby.at", "*.cineby.at"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": "^https?://([^/]+\\.)?(popads\\.net|popcash\\.net|propellerads\\.com|onclicka\\.com|adsterra\\.com|doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com)/.*",
          "if-domain": ["cineby.at", "*.cineby.at", "aether.bar", "*.aether.bar", "popcornmovies.io", "*.popcornmovies.io"]
        },
        "action": { "type": "block" }
      },
      {
        "trigger": {
          "url-filter": ".*",
          "resource-type": ["popup"],
          "if-domain": ["cineby.at", "*.cineby.at", "aether.bar", "*.aether.bar", "popcornmovies.io", "*.popcornmovies.io"]
        },
        "action": { "type": "block" }
      }
    ]
    """#

    static let pageProtectionScript = #"""
    (() => {
      const originalOpen = window.open;

      Object.defineProperty(window, "open", {
        configurable: false,
        enumerable: true,
        writable: false,
        value: function () { return null; }
      });

      const cleanLinks = (root) => {
        if (!root || !root.querySelectorAll) return;
        root.querySelectorAll('a[target="_blank"], a[target="_new"]').forEach((link) => {
          link.removeAttribute("target");
          link.removeAttribute("rel");
        });
      };

      cleanLinks(document);

      const observationRoot = document.documentElement || document;
      new MutationObserver((mutations) => {
        for (const mutation of mutations) {
          for (const node of mutation.addedNodes) cleanLinks(node);
        }
      }).observe(observationRoot, { childList: true, subtree: true });

      document.addEventListener("click", (event) => {
        const link = event.target && event.target.closest
          ? event.target.closest('a[target="_blank"], a[target="_new"]')
          : null;
        if (link) link.removeAttribute("target");
      }, true);
    })();
    """#
}
