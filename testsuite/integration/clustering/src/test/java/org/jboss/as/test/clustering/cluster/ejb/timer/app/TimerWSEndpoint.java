/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/" + TimerWSEndpoint.ENDPOINT)
public class TimerWSEndpoint {

    public static final String ENDPOINT = "TimerWS";

    private static Logger log = Logger.getLogger(TimerWSEndpoint.class);

    private static ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, Integer> offset = new ConcurrentHashMap<>();

    private static List<TimerRecord> messages = Collections.synchronizedList(new ArrayList<>());

    @EJB
    private TimerServiceBean timerServiceBean;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        log.debugf("onOpen connection with %s", session.getId());
        sessions.put(session.getId(), session);
        offset.put(session.getId(), 0);
        if (!messages.isEmpty()) {
            flush(session);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        log.debugf("onMessage: %s", message);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        log.debugf("onClose connection with %s", session.getId());
        sessions.remove(session.getId());
        offset.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("error ocurred in websocket endpoint", throwable);
    }

    public static synchronized void send(TimerRecord timerRecord) {
        messages.add(timerRecord);
        sessions.values().forEach(TimerWSEndpoint::flush);
    }

    public static synchronized void flush(Session session) {
        Jsonb jsonb = JsonbBuilder.create();
        Integer index = offset.get(session.getId());
        for (int i = index; i < messages.size(); i++) {
            try {
                log.infof("send message to connection with %s", session.getId());
                session.getBasicRemote().sendText(jsonb.toJson(messages.get(i)));
            } catch (JsonbException | IOException e) {
                log.error("An error happend during communication", e);
            }
        }
        offset.put(session.getId(), messages.size());
    }
}