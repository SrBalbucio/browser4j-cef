package balbucio.browser4j.network.interception;

import balbucio.browser4j.network.api.Response;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public class MockResourceHandler extends CefResourceHandlerAdapter {
    private final Response mockedResponse;
    private int offset = 0;

    public MockResourceHandler(Response mockedResponse) {
        this.mockedResponse = mockedResponse;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef response_length, StringRef redirectUrl) {
        response.setStatus(mockedResponse.getStatusCode());
        response.setMimeType(mockedResponse.getContentType());
        response_length.set(mockedResponse.getBody().length);
    }

    @Override
    public boolean readResponse(byte[] data_out, int bytes_to_read, IntRef bytes_read, CefCallback callback) {
        int length = mockedResponse.getBody().length;
        if (offset < length) {
            int toCopy = Math.min(bytes_to_read, length - offset);
            System.arraycopy(mockedResponse.getBody(), offset, data_out, 0, toCopy);
            offset += toCopy;
            bytes_read.set(toCopy);
            return true;
        }
        bytes_read.set(0);
        return false;
    }
}
