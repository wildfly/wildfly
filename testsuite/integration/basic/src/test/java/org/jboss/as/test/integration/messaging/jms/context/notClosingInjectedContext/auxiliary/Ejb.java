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

package org.jboss.as.test.integration.messaging.jms.context.notClosingInjectedContext.auxiliary;

import org.jboss.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Ejb sends messages via two injected jmsContexts.
 *
 * @author Gunter Zeilinger <gunterze@gmail.com>, Jiri Ondrusek <jondruse@redhat.com>
 * @since Sep 2018
 */
@Stateless
public class Ejb {
    private static final Logger LOGGER = Logger.getLogger(Ejb.class);

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
            jmsContext.createProducer().send(lookup(Mdb.JNDI_NAME), text);
            LOGGER.info("Sent:" + text);
        } catch (RuntimeException e) {
            LOGGER.error("FAILED to send:" + text);
            throw e;
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
