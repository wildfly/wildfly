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

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.core.management.BridgeControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.PathAddress;

/**
 * Handler for runtime operations that interact with a ActiveMQ {@link org.apache.activemq.artemis.api.core.management.BridgeControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BridgeControlHandler extends AbstractActiveMQComponentControlHandler<BridgeControl> {

    public static final BridgeControlHandler INSTANCE = new BridgeControlHandler();

    @Override
    protected BridgeControl getActiveMQComponentControl(ActiveMQServer activeMQServer, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return BridgeControl.class.cast(activeMQServer.getManagementService().getResource(ResourceNames.BRIDGE + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.BRIDGE;
    }
}
