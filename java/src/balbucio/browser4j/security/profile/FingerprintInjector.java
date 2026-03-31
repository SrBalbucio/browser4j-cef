package balbucio.browser4j.security.profile;

import org.cef.browser.CefBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerprintInjector {

    private static final Logger log = LoggerFactory.getLogger(FingerprintInjector.class);

    public static void inject(CefBrowser browser, FingerprintProfile profile) {
        if (profile == null) return;

        // Script para mofar Propriedades nativas JS do Navigator e Screen object.
        // Utiliza Object.defineProperty para forçar o sobreescrita de read-only fields e evitar spoofing detetável facilmente por JS.
        StringBuilder js = new StringBuilder();
        js.append("(function() {");
        js.append("try {");
        
        // Define Platform
        js.append("Object.defineProperty(navigator, 'platform', { get: () => '").append(profile.getPlatform()).append("' });");
        
        // Define Language
        js.append("Object.defineProperty(navigator, 'language', { get: () => '").append(profile.getAcceptLanguage().split(",")[0]).append("' });");
        js.append("Object.defineProperty(navigator, 'languages', { get: () => ['").append(profile.getAcceptLanguage().split(",")[0]).append("'] });");
        
        // Define Hardware Concurrency
        js.append("Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => ").append(profile.getHardwareConcurrency()).append(" });");
        
        // Define Device Memory
        js.append("Object.defineProperty(navigator, 'deviceMemory', { get: () => ").append(profile.getDeviceMemory()).append(" });");
        
        // Override Timezone Offset (Approximate by Date prototype override or Intl)
        // A direct mock of Intl.DateTimeFormat can be complex, but here's a placeholder comment for Intl
        // js.append("const originalDateTimeFormat = Intl.DateTimeFormat; ...");
        
        // Mock Screen dimensions
        js.append("Object.defineProperty(screen, 'width', { get: () => ").append(profile.getScreenWidth()).append(" });");
        js.append("Object.defineProperty(screen, 'availWidth', { get: () => ").append(profile.getScreenWidth()).append(" });");
        js.append("Object.defineProperty(screen, 'height', { get: () => ").append(profile.getScreenHeight()).append(" });");
        js.append("Object.defineProperty(screen, 'availHeight', { get: () => ").append(profile.getScreenHeight()).append(" });");
        
        js.append("} catch (e) { console.error('FingerprintInjector failed', e); }");
        js.append("})();");

        // Executar na aba global para o contexto da pagina
        browser.executeJavaScript(js.toString(), browser.getURL(), 0);
        log.debug("Injetado payload de Fingerprint no Javascript Engine.");
    }
}
