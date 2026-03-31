package balbucio.browser4j.browser.api;

/**
 * JS script provider for DOM mutation observation.
 */
public class DOMMutationObserverInjector {
    public static final String INJECTION_SCRIPT =
            "(function(){" +
            "  if (window.__browser4j_dom_mutation_observer_installed) return;" +
            "  window.__browser4j_dom_mutation_observer_installed = true;" +
            "  const serializeNode = (node) => {" +
            "    if (!node) return null;" +
            "    return {" +
            "      tag: node.tagName || null," +
            "      id: node.id || null," +
            "      class: node.className || null," +
            "      outerHTML: node.outerHTML || null" +
            "    };" +
            "  };" +
            "  const serializeNodeList = (nodes) => Array.from(nodes||[]).map(n => n && n.outerHTML ? n.outerHTML : null).filter(v => v!==null);" +
            "  const report = (mutations) => {" +
            "    try {" +
            "      const data = { mutations: mutations.map(m => ({" +
            "        type: m.type," +
            "        target: serializeNode(m.target)," +
            "        attributeName: m.attributeName || null," +
            "        oldValue: m.oldValue || null," +
            "        added: serializeNodeList(m.addedNodes)," +
            "        removed: serializeNodeList(m.removedNodes)" +
            "      }))};" +
            "      if (window.bridge) {" +
            "        const token = window.__browser4j_bridge_token || null;" +
            "        const payload = { event: 'dom_mutation', data: data, bridgeToken: token };" +
            "        window.bridge({request: JSON.stringify(payload), onSuccess: function(){}, onFailure: function(){}});" +
            "      }" +
            "    } catch (e) {console.warn('DOMMutationObserver failed', e);}" +
            "  };" +
            "  const obs = new MutationObserver((records) => {report(records);});" +
            "  if (document && document.documentElement) {" +
            "    obs.observe(document.documentElement, { childList:true, subtree:true, attributes:true, characterData:true, attributeOldValue:true, characterDataOldValue:true });" +
            "  }" +
            "})();";
}