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

import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelType;
import org.xnio.Option;
import org.xnio.OptionMap;

import java.util.ListIterator;
import java.util.Set;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConnectorResource extends SimpleResourceDefinition {

    static final ConnectorResource INSTANCE = new ConnectorResource();

    static final SimpleAttributeDefinition AUTHENTICATION_PROVIDER = new NamedValueAttributeDefinition(CommonAttributes.AUTHENTICATION_PROVIDER, Attribute.NAME, null, ModelType.STRING, true);
    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinition(CommonAttributes.SOCKET_BINDING, ModelType.STRING, false);

    private ConnectorResource() {
        super(PathElement.pathElement(CommonAttributes.CONNECTOR),
                RemotingExtension.getResourceDescriptionResolver(CONNECTOR),
                ConnectorAdd.INSTANCE,
                ConnectorRemove.INSTANCE);
    }

    protected static OptionMap getOptions(OperationContext context, PathAddress pathAddress) {
        final OptionMap optionMap;
        Resource resource = context.getRootResource().navigate(pathAddress);
        Set<Resource.ResourceEntry> entries = resource.getChildren(CommonAttributes.PROPERTY);
        if (entries.size() > 0) {
            OptionMap.Builder builder = OptionMap.builder();
            final ClassLoader loader = SecurityActions.getClassLoader(ConnectorResource.class);
            for (Resource.ResourceEntry entry : entries) {
                String name = entry.getName();
                if (!name.contains(".")){
                    name = "org.xnio.Options."+name;
                }
                final Option option = Option.fromString(name, loader);
                builder.set(option, option.parseValue(entry.getModel().get(CommonAttributes.VALUE).asString(), loader));
            }
            optionMap = builder.getMap();
        } else {
            optionMap = OptionMap.EMPTY;
        }
        return optionMap;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(AUTHENTICATION_PROVIDER, SOCKET_BINDING);
        resourceRegistration.registerReadWriteAttribute(AUTHENTICATION_PROVIDER, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, writeHandler);
    }
}
