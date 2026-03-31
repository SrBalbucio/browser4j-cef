package balbucio.browser4j.security.drm;

public class DRMInjector {
    public static final String INJECTION_SCRIPT = 
        "if (!window.__drm_hooked) {" +
        "   window.__drm_hooked = true;" +
        "   window.__drm_detected_flag = false;" +
        "   const pingDRM = () => {" +
        "       if (!window.__drm_detected_flag) {" +
        "           window.__drm_detected_flag = true;" +
        "           if (window.bridge) {" +
        "               window.bridge({request: JSON.stringify({event: '__drm_detected', data: {}}), onSuccess: function(){}, onFailure: function(){}});" +
        "           }" +
        "       }" +
        "   };" +
        "   if (navigator.requestMediaKeySystemAccess) {" +
        "       const og = navigator.requestMediaKeySystemAccess.bind(navigator);" +
        "       navigator.requestMediaKeySystemAccess = function(keySystem, supportedConfigurations) {" +
        "           pingDRM();" +
        "           return og(keySystem, supportedConfigurations);" +
        "       };" +
        "   }" +
        "   window.addEventListener('encrypted', pingDRM, true);" +
        "}";

    public static final String EVALUATION_SCRIPT = 
        "return !!window.__drm_detected_flag || " +
        "Array.from(document.querySelectorAll('video, audio')).some(el => !!el.mediaKeys || el.onencrypted !== undefined);";
}
