/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.activationconfig;

import static org.jboss.as.test.integration.ejb.mdb.activationconfig.JMSHelper.reply;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 */
@MessageDriven (activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "java:/ConnectionFactory"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = MDBWithLookupActivationConfigProperties.QUEUE_JNDI_NAME),
})
public class MDBWithLookupActivationConfigProperties implements MessageListener {
    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/MDBWithLookupActivationConfigProperties";

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public void onMessage(Message message) {
        reply(connectionFactory, message);
    }
}
