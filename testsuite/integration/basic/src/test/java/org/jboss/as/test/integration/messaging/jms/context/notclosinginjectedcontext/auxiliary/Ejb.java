/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
