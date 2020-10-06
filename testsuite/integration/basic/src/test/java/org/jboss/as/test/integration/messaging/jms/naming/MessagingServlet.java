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
package org.jboss.as.test.integration.messaging.jms.naming;


import org.jboss.as.test.shared.TimeoutUtil;

import java.io.IOException;
import javax.annotation.Resource;

import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
