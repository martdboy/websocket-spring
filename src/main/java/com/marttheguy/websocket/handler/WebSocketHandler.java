package com.marttheguy.websocket.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebSocketHandler extends TextWebSocketHandler {

    // Conjunto sincronizado para armazenar as sessões dos clientes
    private static final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, String> userNames = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Adicionar nova sessão ao conjunto
        sessions.add(session);
        System.out.println("Nova conexão estabelecida: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Mensagem recebida de " + session.getId() + ": " + payload);

        if (payload.startsWith("join:")) {
            String userName = payload.substring(5);
            userNames.put(session.getId(), userName);
            broadcastUserList();
        } else {
            // Enviar a mensagem recebida para todas as sessões (broadcast)
            for (WebSocketSession clientSession : sessions) {
                if (clientSession.isOpen()) {
                    clientSession.sendMessage(new TextMessage(payload));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // Remover sessão fechada do conjunto
        sessions.remove(session);
        userNames.remove(session.getId());
        broadcastUserList();
        System.out.println("Conexão fechada: " + session.getId());
    }

    private void broadcastUserList() throws Exception {
        String userListMessage = "users:" + String.join(",", userNames.values());
        for (WebSocketSession clientSession : sessions) {
            if (clientSession.isOpen()) {
                clientSession.sendMessage(new TextMessage(userListMessage));
            }
        }
    }
}
