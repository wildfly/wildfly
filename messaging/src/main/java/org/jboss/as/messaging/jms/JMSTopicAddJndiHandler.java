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

package org.jboss.as.messaging.jms;

import java.util.Locale;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.TopicControl;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.management.ManagementService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.messaging.MessagingDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * Handler for "add-jndi" operation on a JMS topic resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicAddJndiHandler extends AbstractAddJndiHandler {

    public static final JMSTopicAddJndiHandler INSTANCE = new JMSTopicAddJndiHandler();

    private JMSTopicAddJndiHandler() {
    }

    @Override
    protected void addJndiNameToControl(String toAdd, String resourceName, HornetQServer server, OperationContext context) {
        ManagementService mgmt = server.getManagementService();
        TopicControl control = TopicControl.class.cast(mgmt.getResource(ResourceNames.JMS_TOPIC + resourceName));
        try {
            control.addJNDI(toAdd);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getAddJndiOperation(locale, "jms-topic");
    }
}
