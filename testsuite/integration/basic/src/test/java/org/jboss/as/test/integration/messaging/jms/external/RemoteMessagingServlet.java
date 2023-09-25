/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.external;

import static org.jboss.as.test.integration.messaging.jms.external.ExternalMessagingDeploymentTestCase.QUEUE_LOOKUP;
import static org.jboss.as.test.integration.messaging.jms.external.ExternalMessagingDeploymentTestCase.TOPIC_LOOKUP;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSPasswordCredential;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jboss.as.test.shared.TimeoutUtil;

/**
 *
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
@JMSConnectionFactoryDefinition(
        resourceAdapter = "remote-artemis",
        name = "java:global/definedFactory",
        user = "guest",
        password = "guest"
)
@WebServlet("/ClientMessagingDeploymentTestCase")
public class RemoteMessagingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource(lookup = QUEUE_LOOKUP)
    private Queue queue;

    @Resource(lookup = TOPIC_LOOKUP)
    private Topic topic;

    @Inject
    @JMSConnectionFactory("java:/JmsXA")
    @JMSPasswordCredential(userName = "guest", password = "guest")
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
