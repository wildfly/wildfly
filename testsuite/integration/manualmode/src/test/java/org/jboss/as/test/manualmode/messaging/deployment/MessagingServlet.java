/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.messaging.deployment;

import org.jboss.as.test.shared.TimeoutUtil;


import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@WebServlet("/DependentMessagingDeploymentTestCase")
public class MessagingServlet extends HttpServlet {

    @Resource(lookup = "java:/jms/DependentMessagingDeploymentTestCase/myQueue")
    private Queue queue;

    @Resource(lookup = "java:/jms/DependentMessagingDeploymentTestCase/myTopic")
    private Topic topic;

    @Inject
    private JMSContext context;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean useTopic = req.getParameterMap().containsKey("topic");
        final Destination destination = useTopic ? topic : queue;
        final String text = req.getParameter("text");

        String reply = sendAndReceiveMessage(destination, text);

        resp.getWriter().write(reply);
    }

    private String sendAndReceiveMessage(Destination destination, String text) {
            Destination replyTo = context.createTemporaryQueue();

            JMSConsumer consumer = context.createConsumer(replyTo);

            context.createProducer()
                    .setJMSReplyTo(replyTo)
                    .send(destination, text);

            return consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
    }
}
