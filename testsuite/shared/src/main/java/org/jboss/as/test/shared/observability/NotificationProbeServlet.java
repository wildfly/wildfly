/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.naming.InitialContext;

import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;

/** Records resource lifecycle notifications and sends probe messages to JMS queues. */
public class NotificationProbeServlet extends HttpServlet {
    private static final List<String> NOTIFICATIONS = new CopyOnWriteArrayList<>();
    private NotificationHandlerRegistry registry;
    private NotificationHandler handler;
    private NotificationFilter filter;

    /** Registers the notification handler used by the integration test probe. */
    @Override
    public void init() throws ServletException {
        try {
            registry = (NotificationHandlerRegistry) CurrentServiceContainer.getServiceContainer()
                    .getService(ServiceName.parse("org.wildfly.management.notification-handler-registry"))
                    .getValue();
            handler = this::recordNotification;
            filter = this::isResourceLifecycleNotification;
            registry.registerNotificationHandler(NotificationHandlerRegistry.ANY_ADDRESS, handler, filter);
        } catch (Exception e) {
            throw new ServletException("Could not register notification probe", e);
        }
    }

    /** Unregisters the notification handler when the probe is destroyed. */
    @Override
    public void destroy() {
        if (registry != null) {
            registry.unregisterNotificationHandler(NotificationHandlerRegistry.ANY_ADDRESS, handler, filter);
        }
    }

    /** Optionally sends a JMS message, clears recorded notifications, and returns them. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getParameter("send") != null) {
            sendMessage(request.getParameter("send"));
        }
        if ("true".equals(request.getParameter("clear"))) {
            NOTIFICATIONS.clear();
        }
        response.setContentType("text/plain");
        response.getWriter().print(String.join(System.lineSeparator(), NOTIFICATIONS));
    }

    /** Sends one probe message to the named queue. */
    private void sendMessage(String queueName) throws IOException {
        try {
            InitialContext initialContext = new InitialContext();
            jakarta.jms.ConnectionFactory connectionFactory =
                    (jakarta.jms.ConnectionFactory) initialContext.lookup("java:/JmsXA");
            Queue queue = (Queue) initialContext.lookup("java:/jms/queue/" + queueName);
            try (JMSContext context = connectionFactory.createContext()) {
                JMSProducer producer = context.createProducer();
                producer.send(queue, "message");
            }
        } catch (Exception e) {
            throw new IOException("Could not send messages to " + queueName, e);
        }
    }

    /** Stores the notification type and source address for later assertions. */
    private void recordNotification(Notification notification) {
        NOTIFICATIONS.add(notification.getType() + "|" + notification.getSource().toCLIStyleString());
    }

    /** Accepts only resource-added and resource-removed notifications. */
    private boolean isResourceLifecycleNotification(Notification notification) {
        return "resource-added".equals(notification.getType()) || "resource-removed".equals(notification.getType());
    }
}
