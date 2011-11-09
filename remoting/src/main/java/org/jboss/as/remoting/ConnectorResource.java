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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.SOCKET_BINDING;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConnectorResource extends SimpleResourceDefinition {

    static final ConnectorResource INSTANCE = new ConnectorResource();

    static final SimpleAttributeDefinition SOCKET_BINDING_ATTRIBUTE = new SimpleAttributeDefinition(SOCKET_BINDING, ModelType.STRING, false);
    static final SimpleAttributeDefinition AUTHENTICATION_PROVIER_ATTRIBUTE = new NamedValueAttributeDefinition(AUTHENTICATION_PROVIDER, Attribute.NAME, null, ModelType.STRING, true);

    private ConnectorResource() {
        super(PathElement.pathElement(CommonAttributes.CONNECTOR),
                RemotingExtension.getResourceDescriptionResolver(CONNECTOR),
                ConnectorAdd.INSTANCE,
                ConnectorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(SOCKET_BINDING_ATTRIBUTE, null);
        resourceRegistration.registerReadOnlyAttribute(AUTHENTICATION_PROVIER_ATTRIBUTE, null);
    }


}
