/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.jmx.CommonAttributes.JMX;
import static org.jboss.as.jmx.CommonAttributes.REMOTING_CONNECTOR;

/**
 *
 * @author Stuart Douglas
 */
public class RemotingConnectorResource extends SimpleResourceDefinition {

    static final PathElement REMOTE_CONNECTOR_CONFIG_PATH = PathElement.pathElement(REMOTING_CONNECTOR, JMX);
    static final SimpleAttributeDefinition USE_MANAGEMENT_ENDPOINT = new SimpleAttributeDefinitionBuilder(CommonAttributes.USE_MANAGMENT_ENDPOINT, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true)).build();
    static final RemotingConnectorResource INSTANCE = new RemotingConnectorResource();

    private RemotingConnectorResource() {
        super(REMOTE_CONNECTOR_CONFIG_PATH,
                JMXExtension.getResourceDescriptionResolver(CommonAttributes.REMOTING_CONNECTOR),
                RemotingConnectorAdd.INSTANCE,
                RemotingConnectorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(USE_MANAGEMENT_ENDPOINT);
        resourceRegistration.registerReadWriteAttribute(USE_MANAGEMENT_ENDPOINT, null, writeHandler);
    }
}
