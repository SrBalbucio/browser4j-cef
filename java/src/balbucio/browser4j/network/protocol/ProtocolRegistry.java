package balbucio.browser4j.network.protocol;

import balbucio.browser4j.network.api.Response;
import org.cef.CefApp;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ProtocolRegistry {
    private final Map<String, ProtocolHandler> handlers = new ConcurrentHashMap<>();
    private final CefApp cefApp;

    public ProtocolRegistry(CefApp cefApp) {
        this.cefApp = cefApp;
    }

    public void register(String scheme, ProtocolHandler handler) {
        handlers.put(scheme, handler);

        cefApp.registerSchemeHandlerFactory(scheme, "", new CefSchemeHandlerFactory() {
            @Override
            public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
                ProtocolHandler schemeHandler = handlers.get(schemeName);
                if (schemeHandler == null) return null;
                
                return new org.cef.handler.CefResourceHandlerAdapter() {
                    private byte[] data;
                    private int bytesRead = 0;
                    private Response responseData;

                    @Override
                    public boolean processRequest(CefRequest req, org.cef.callback.CefCallback callback) {
                        responseData = schemeHandler.handle(req.getURL());
                        if (responseData != null) {
                            if (responseData.getBody() != null) {
                                this.data = responseData.getBody();
                            } else {
                                this.data = new byte[0];
                            }
                            callback.Continue();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void getResponseHeaders(org.cef.network.CefResponse response, org.cef.misc.IntRef response_length, org.cef.misc.StringRef redirectUrl) {
                        if (responseData != null) {
                            response.setStatus(responseData.getStatusCode());
                            response.setMimeType(responseData.getContentType());
                            
                            if (responseData.getHeaders() != null) {
                                responseData.getHeaders().forEach((k, v) -> response.setHeaderByName(k, v, true));
                            }
                        }
                        response_length.set(data == null ? 0 : data.length);
                    }

                    @Override
                    public boolean readResponse(byte[] data_out, int bytes_to_read, org.cef.misc.IntRef bytes_read, org.cef.callback.CefCallback callback) {
                        if (data == null || bytesRead >= data.length) {
                            bytes_read.set(0);
                            return false;
                        }

                        int length = Math.min(data.length - bytesRead, bytes_to_read);
                        System.arraycopy(data, bytesRead, data_out, 0, length);
                        bytesRead += length;
                        bytes_read.set(length);
                        return true;
                    }
                };
            }
        });
    }
}
