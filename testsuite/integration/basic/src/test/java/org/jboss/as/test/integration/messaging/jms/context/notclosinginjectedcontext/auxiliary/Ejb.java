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

package org.jboss.as.test.integration.messaging.jms.context.notclosinginjectedcontext.auxiliary;

import org.jboss.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.as.test.shared.TimeoutUtil;

/**
 * Ejb sends messages via two injected jmsContexts.
 *
 * @author Gunter Zeilinger <gunterze@gmail.com>, Jiri Ondrusek <jondruse@redhat.com>
 * @since Sep 2018
 */
@Stateless
public class Ejb {
    private static final Logger LOGGER = Logger.getLogger(Ejb.class);

    private static final long TIMEOUT = TimeoutUtil.adjust(60000);
    private static final long PAUSE = TimeoutUtil.adjust(200);
    @Inject
    private JMSContext jmsCtx1;

    @Inject
    private JMSContext jmsCtx2;

    public void send(String text) {
        send(text, jmsCtx1);
        send(text, jmsCtx2);
    }

    private void send(String text, JMSContext jmsContext) {
        try {
            LOGGER.info("Sending: " + text);
            long start = System.currentTimeMillis();
            Queue queue = lookup(Mdb.JNDI_NAME);
            while(queue == null && (System.currentTimeMillis() - start < TIMEOUT)) {
                queue = lookup(Mdb.JNDI_NAME);
                Thread.sleep(PAUSE);
            }
            jmsContext.createProducer().send(queue, text);
            LOGGER.info("Sent:" + text);
        } catch (RuntimeException e) {
            LOGGER.error("FAILED to send:" + text);
            throw e;
        } catch (InterruptedException ex) {
            LOGGER.error("FAILED to send:" + text);
            throw new RuntimeException(ex);
        }
    }

    private Queue lookup(String jndiName) {
        try {
            return InitialContext.doLookup(jndiName);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
