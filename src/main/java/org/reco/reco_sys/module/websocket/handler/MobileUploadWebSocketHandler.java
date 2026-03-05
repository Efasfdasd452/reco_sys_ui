package org.reco.reco_sys.module.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MobileUploadWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token != null) {
            sessions.put(token, session);
            log.info("WebSocket 连接建立, token={}", token);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = extractToken(session);
        if (token != null) {
            sessions.remove(token);
            log.info("WebSocket 连接关闭, token={}", token);
        }
    }

    public void notifyUploadDone(String token, String fileUrl) {
        WebSocketSession session = sessions.get(token);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"UPLOAD_DONE\",\"url\":\"" + fileUrl + "\"}"));
                sessions.remove(token);
            } catch (Exception e) {
                log.error("WebSocket 消息发送失败, token={}", token, e);
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
