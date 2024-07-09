/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.messaging.deployment;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@JMSDestinationDefinitions(
        value = {
            @JMSDestinationDefinition(
                    name = "java:/queue/HELLOWORLDMDBQueue",
                    interfaceName = "jakarta.jms.Queue",
                    destinationName = "HelloWorldMDBQueue"
            ),
            @JMSDestinationDefinition(
                    name = "java:/topic/HELLOWORLDMDBTopic",
                    interfaceName = "jakarta.jms.Topic",
                    destinationName = "HelloWorldMDBTopic"
            )
        }
)
@WebServlet("/HelloWorldMDBServletClient")
public class HelloWorldMDBServletClient extends HttpServlet {

    private static final long serialVersionUID = -8314035702649252239L;

    private static final int MSG_COUNT = 5;

    @Inject
    private transient JMSContext context;

    @Resource(lookup = "java:/queue/HELLOWORLDMDBQueue")
    private transient Queue queue;

    @Resource(lookup = "java:/topic/HELLOWORLDMDBTopic")
    private transient Topic topic;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<h1>Quickstart: Example demonstrates the use of <strong>Jakarta Messaging 3.1</strong> and <strong>Jakarta Enterprise Beans 4.0 Message-Driven Bean</strong> in a JakartaEE server.</h1>");
            boolean useTopic = req.getParameterMap().keySet().contains("topic");
            final Destination destination = useTopic ? topic : queue;

            out.write("<p>Sending messages to <em>" + destination + "</em></p>");
            out.write("<h2>The following messages will be sent to the destination:</h2>");
            for (int i = 0; i < MSG_COUNT; i++) {
                String text = "This is message " + (i + 1);
                context.createProducer().send(destination, text);
                out.write("<p id=\"message_" + i + "\">Message (" + i + "): " + text + "</p>");
            }
            out.println("<p><i>Go to your JakartaEE server console or server log to see the result of messages processing.</i></p>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
