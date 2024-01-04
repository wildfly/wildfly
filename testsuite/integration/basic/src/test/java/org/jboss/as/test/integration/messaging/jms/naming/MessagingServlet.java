/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.naming;


import org.jboss.as.test.shared.TimeoutUtil;

import java.io.IOException;
import jakarta.annotation.Resource;

import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
@WebServlet("/JndiMessagingDeploymentTestCase")
public class MessagingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private JMSSender jmsSender;

    @Inject
    @JMSConnectionFactory("java:app/jms/nonXAconnectionFactory")
    private JMSContext context;

    @Resource(lookup = "java:app/jms/queue")
    private Queue queue;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String text = req.getParameter("text");
        String reply = sendAndReceiveMessage(text);
        resp.getWriter().write(reply);
    }

    private String sendAndReceiveMessage(String text) {
        JMSConsumer consumer = context.createConsumer(queue);
        jmsSender.sendMessage(text);
        return consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
    }
}
