/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.http.server;

import static io.undertow.util.Headers.CONTENT_TYPE;
import static io.undertow.util.Headers.LOCATION;
import static io.undertow.util.HttpString.tryFromString;
import static io.undertow.util.Methods.DELETE;
import static io.undertow.util.Methods.GET;
import static io.undertow.util.Methods.POST;
import static io.undertow.util.StatusCodes.CREATED;
import static io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
import static io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
import static io.undertow.util.StatusCodes.NOT_FOUND;
import static io.undertow.util.StatusCodes.NO_CONTENT;
import static io.undertow.util.StatusCodes.OK;
import static java.lang.String.format;
import static org.jboss.as.controller.notification.NotificationFilter.ALL;
import static org.jboss.as.domain.http.server.Common.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Common.LINK;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationSupport;
import org.jboss.dmr.ModelNode;
import org.xnio.streams.ChannelInputStream;
import org.xnio.streams.ChannelOutputStream;

/**
 *
 * The HTTP notification handler.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) Red Hat, inc
 */
public class NotificationApiHandler implements HttpHandler {

    /**
     * Maximum number of notifications held *per handler*
     * TODO make it configurable.
     */
    public static final int MAX_NOTIFICATIONS = 1024;

    static final String PATH = "/management/notification";
    private static final String HANDLER_PREFIX = "handler";
    private static final String NOTIFICATIONS = "notifications";
    public static final String RESOURCES = "resources";

    private final NotificationSupport notificationSupport;
    /**
     * Map of HttpNotificationHandler holding the notifications for a given handlerID
     */
    private final Map<String, HttpNotificationHandler> handlers = new HashMap<>();
    /**
     * counter to generate unique ID for the registered handlers
     */
    private final AtomicLong handlerCounter = new AtomicLong();

    public NotificationApiHandler(NotificationSupport notificationSupport) {
        this.notificationSupport = notificationSupport;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            doHandle(exchange);
        } catch (Exception e) {
            sendResponse(exchange, INTERNAL_SERVER_ERROR);
        }
    }
    private void doHandle(final HttpServerExchange http) throws IOException {
        final String path = http.getRequestPath();
        // /management/notification
        if (path.equals(PATH) || path.equals(PATH + "/")) {
            handleNotificationRegistration(http);
        } else {
            String[] splits = path.split("/");
            if (splits.length == 4) {
                // /management/notification/${handlerID}
                final String handlerID = splits[3];
                handleNotificationHandlerResource(http, handlerID);
            } else if (splits.length == 5 && splits[4].equals(NOTIFICATIONS)) {
                // /management/notification/${handlerID}/notifications
                final String handlerID = splits[3];
                handleNotificationsResource(http, handlerID);
            } else {
                sendResponse(http, NOT_FOUND);
            }
        }
    }

    /**
     * POST /management/notification => create a handler resource that listens to a list of resources
     */
    private void handleNotificationRegistration(final HttpServerExchange http) throws IOException {
        if (POST.equals(http.getRequestMethod())) {
            String handlerID = generateHandlerID();
            ModelNode operation;
            try (InputStream in = new ChannelInputStream(http.getRequestChannel())) {
                operation = ModelNode.fromJSONStream(in);
            }
            registerNotificationHandler(handlerID, operation);
            http.getResponseHeaders().add(LOCATION, getHandlerURL(handlerID));
            http.getResponseHeaders().add(tryFromString(LINK), getHandlerNotificationsURL(handlerID));
            sendResponse(http, CREATED);
        } else {
            sendResponse(http, METHOD_NOT_ALLOWED);
        }
    }

    /**
     * GET    /management/notification/${handlerID} => returns a representation of the handler
     * POST   /management/notification/${handlerID} => update the resources that the handler listens to
     * DELETE /management/notification/${handlerID} => unregister the handler and delete it
     */
    private void handleNotificationHandlerResource(final HttpServerExchange http, final String handlerID) throws IOException {
        HttpString method = http.getRequestMethod();
        if (GET.equals(method)) {
            ModelNode addresses = getAddressesListeningTo(handlerID);
            if (addresses == null) {
                sendResponse(http, NOT_FOUND);
            } else {
                http.getResponseHeaders().add(tryFromString(LINK), getHandlerNotificationsURL(handlerID));
                sendResponse(http, OK, addresses.toJSONString(true), APPLICATION_JSON);
            }
        } else if (POST.equals(method)) {
            ModelNode operation = ModelNode.fromJSONStream(http.getInputStream());
            unregisterNotificationHandler(handlerID);
            registerNotificationHandler(handlerID, operation);
            sendResponse(http, OK);
        } else if (DELETE.equals(method)) {
            boolean unregistered = unregisterNotificationHandler(handlerID);
            if (unregistered) {
                sendResponse(http, NO_CONTENT);
            } else {
                sendResponse(http, NOT_FOUND);
            }
        } else {
            sendResponse(http, METHOD_NOT_ALLOWED);
        }
    }

    /**
     * POST /management/notification/${handlerID}/notifications => returns a representation of the notifications received by the handler
     */
    private void handleNotificationsResource(final HttpServerExchange http, final String handlerID) throws IOException {
        if (POST.equals(http.getRequestMethod())) {
            ModelNode notifications = fetchNotifications(handlerID);
            if (notifications == null) {
                sendResponse(http, NOT_FOUND);
            } else {
                sendResponse(http, OK, notifications.isDefined()? notifications.toJSONString(true) : null, APPLICATION_JSON);
            }
        } else {
            sendResponse(http, METHOD_NOT_ALLOWED);
        }
    }

    private String generateHandlerID() {
        return HANDLER_PREFIX + handlerCounter.incrementAndGet();
    }

    private ModelNode getAddressesListeningTo(final String handlerID) {
        if (!handlers.containsKey(handlerID)) {
            return null;
        }
        final ModelNode node = new ModelNode();
        HttpNotificationHandler handler = handlers.get(handlerID);
        for (PathAddress address : handler.getListeningAddresses()) {
            node.add(address.toModelNode());
        }
        return node;
    }

    private ModelNode fetchNotifications(final String handlerID) {
        if (!handlers.containsKey(handlerID)) {
            return null;
        }
        ModelNode node = new ModelNode();
        HttpNotificationHandler handler = handlers.get(handlerID);
        for (ModelNode notification : handler.getNotifications()) {
            node.add(notification);
        }
        handler.clear();
        return node;
    }

    private void registerNotificationHandler(final String handlerID, final ModelNode operation) {
        final Set<PathAddress> addresses = new HashSet<>();
        for (ModelNode resource : operation.get(RESOURCES).asList()) {
            addresses.add(PathAddress.pathAddress(resource));
        }
        final HttpNotificationHandler handler = new HttpNotificationHandler(handlerID, addresses);
        for (PathAddress address : addresses) {
            notificationSupport.registerNotificationHandler(address, handler, ALL);
        }
        handlers.put(handlerID, handler);
    }

    private boolean unregisterNotificationHandler(final String handlerID) {
        HttpNotificationHandler handler = handlers.remove(handlerID);
        if (handler != null) {
            for (PathAddress address : handler.getListeningAddresses()) {
                notificationSupport.unregisterNotificationHandler(address, handler, ALL);
            }
        }
        return handler != null;
    }

    private void sendResponse(final HttpServerExchange http, final int responseCode) {
        http.setResponseCode(responseCode);
        http.endExchange();
    }

    private void sendResponse(final HttpServerExchange http, final int responseCode, final String body, final String contentType) throws IOException {
        if (body == null) {
            sendResponse(http, responseCode);
            return;
        }
        http.getResponseHeaders().add(CONTENT_TYPE, contentType);

        try (OutputStream out = new ChannelOutputStream(http.getResponseChannel());
             PrintWriter print = new PrintWriter(out)) {
            print.write(body);
            print.flush();
            out.flush();
        }
        http.endExchange();
    }

    private static String getHandlerURL(final String handlerID) {
        return PATH + "/" + handlerID;
    }

    private static String getHandlerNotificationsURL(final String handlerID) {
        return format("%s/%s/%s; rel=%s",
                PATH,
                handlerID,
                NOTIFICATIONS,
                NOTIFICATIONS);
    }

    private static class HttpNotificationHandler implements NotificationHandler {

        private final String handlerID;
        private final Set<PathAddress> addresses;
        private final Queue<ModelNode> notifications = new ArrayBlockingQueue<>(MAX_NOTIFICATIONS);

        public HttpNotificationHandler(final String handlerID, final Set<PathAddress> addresses) {
            this.handlerID = handlerID;
            this.addresses = addresses;
        }

        public Set<PathAddress> getListeningAddresses() {
            return addresses;
        }

        public List<ModelNode> getNotifications() {
            return new ArrayList<>(notifications);
        }

        public void clear() {
            notifications.clear();
        }

        @Override
        public void handleNotification(final Notification notification) {
            if (notifications.size() == MAX_NOTIFICATIONS) {
                notifications.poll();
            }
            notifications.add(notification.toModelNode());
        }

        @Override
        public String toString() {
            return "HttpNotificationHandler[" +
                    "handlerID='" + handlerID + '\'' +
                    ", addresses=" + addresses +
                    "]@" + System.identityHashCode(this);
        }
    }
}