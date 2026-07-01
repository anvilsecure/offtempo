package burp.handlers;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.*;
import burp.model.HttpRequestWithTimestamp;
import burp.ui.MainPanel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimingHttpHandler implements HttpHandler {

    private final MainPanel mainPanel;
    private final Map<Integer, HttpRequestWithTimestamp> existingRequestMap = new ConcurrentHashMap<>();
    private final Map<Integer, HttpRequestWithTimestamp> nonExistingRequestMap = new ConcurrentHashMap<>();

    public TimingHttpHandler(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Header driven: bypassess toggle
        if (requestToBeSent.hasHeader("X-OffTempo-Pool")) {
            String pool = requestToBeSent.headerValue("X-OffTempo-Pool");
            if ("A".equals(pool)) {
                existingRequestMap.put(requestToBeSent.messageId(), new HttpRequestWithTimestamp(requestToBeSent.messageId(), System.currentTimeMillis()));
            } else if ("B".equals(pool)) {
                nonExistingRequestMap.put(requestToBeSent.messageId(), new HttpRequestWithTimestamp(requestToBeSent.messageId(), System.currentTimeMillis()));
            }
            return RequestToBeSentAction.continueWith(requestToBeSent.withRemovedHeader("X-OffTempo-Pool"));
        }
        // Native Burp capture
        if (requestToBeSent.toolSource().isFromTool(ToolType.INTRUDER) && mainPanel.isCaptureEnabled()) {
            String selected = mainPanel.getSelectedType();
            if ("Pool A".equals(selected)) {
                existingRequestMap.put(requestToBeSent.messageId(), new HttpRequestWithTimestamp(requestToBeSent.messageId(), System.currentTimeMillis()));
            } else {
                nonExistingRequestMap.put(requestToBeSent.messageId(), new HttpRequestWithTimestamp(requestToBeSent.messageId(), System.currentTimeMillis()));
            }
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (responseReceived.toolSource().isFromTool(ToolType.INTRUDER) && mainPanel.isCaptureEnabled()) {
            int messageId = responseReceived.messageId();
            HttpRequestWithTimestamp request = existingRequestMap.remove(messageId);
            if (request != null) {
                mainPanel.addTiming("Pool A", request.getBurpMessageId(), System.currentTimeMillis() - request.getSendTimestamp());
            } else {
                request = nonExistingRequestMap.remove(messageId);
                if (request != null) {
                    mainPanel.addTiming("Pool B", request.getBurpMessageId(), System.currentTimeMillis() - request.getSendTimestamp());
                }
            }
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
