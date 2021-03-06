package com.codesquad.blackjack.socket;

import com.codesquad.blackjack.domain.player.User;
import com.codesquad.blackjack.dto.ChatDto;
import com.codesquad.blackjack.security.HttpSessionUtils;
import com.codesquad.blackjack.security.WebSocketSessionUtils;
import com.codesquad.blackjack.service.GameService;
import com.codesquad.blackjack.web.SessionController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class BlackjackHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(BlackjackHandler.class);

    private Map<Long, GameSession> gameSessions = new HashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionController sessionController;

    @Autowired
    private GameService gameService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        long gameId = WebSocketSessionUtils.gameIdFromSession(session);
        GameSession gameSession = findByGameId(gameId);

        sessionController.readyToGame(session, gameSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        long gameId = getGameId(session);
        GameSession gameSession = findByGameId(gameId);

        String payload = message.getPayload();
        log.info("payload : {}", payload);


        if(payload.contains("START GAME")) {
            sessionController.startGame(gameSession);
            return;
        }

        if(payload.contains("USERTURN")) {
            log.debug("USERTURN ??????");
            sessionController.playerTurnGame(gameSession, 100);
            return;
        }

        if(payload.contains("BETTING")) {
            int turn = Integer.parseInt(payload.split(":")[1]);
            sessionController.playerSelect(gameSession, turn);
            return;
        }

        if(payload.contains("continue")) {
            sessionController.startGame(gameSession);
            return;
        }


        if(payload.contains("DEALERTURN")) {
            log.debug("DEALERTURN ??????");
            sessionController.dealerTurnGame(gameSession);
            return;
        }

        //??????
        ChatDto receivedChat = objectMapper.readValue(payload, ChatDto.class);
        TextMessage chatToSend = new TextMessage(objectMapper.writeValueAsString(receivedChat));
        for (WebSocketSession gameSessionSession : gameSession.getSessions()) {
            gameSessionSession.sendMessage(chatToSend);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("afterConnectionClosed : " + session + " + " + status);
        long gameId = getGameId(session);
        gameSessions.remove(gameId);

        // ????????? ?????? ????????? ??? ?????????????????? ?????? ??????. ????????? ???????????? ?????? ???????????? ???????????? ????????? ??????.

    }

    public GameSession findByGameId(long gameId) {
        if (gameSessions.containsKey(gameId)) {
            return gameSessions.get(gameId);
        }
        gameSessions.put(gameId, new GameSession(gameId));
        return gameSessions.get(gameId);
    }

    private String getUserId(WebSocketSession session) {
        //addInterceptors??? ?????? WebsocketSession??? httpSession??? ?????? ????????? ????????????, Map??? ??? ????????? ????????????
        Map<String, Object> httpSession = session.getAttributes();
        //httpSession??? ????????? ???????????? ????????? ????????????
        User loginUser = (User) httpSession.get(HttpSessionUtils.USER_SESSION_KEY);

        return loginUser.getUserId();
    }

    private long getGameId(WebSocketSession session) {
        Map<String, Object> httpSesion = session.getAttributes();

        return (long) httpSesion.get(WebSocketSessionUtils.GAME_SESSION_KEY);
    }
}
