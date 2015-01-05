/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.dispatcher;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.modules.ModuleIdentifier;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.GroupBuilderFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.GroupBuilderProvider;
import org.wildfly.clustering.spi.GroupServiceNameFactory;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Provides the requisite builders for creating a {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} created from a specified factory..
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryBuilderProvider implements GroupBuilderProvider {

    private final GroupBuilderFactory<CommandDispatcherFactory> factory;

    public CommandDispatcherFactoryBuilderProvider(GroupBuilderFactory<CommandDispatcherFactory> factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Builder<?>> getBuilders(String group, ModuleIdentifier module) {
        Builder<CommandDispatcherFactory> builder = this.factory.createBuilder(group, module);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, GroupServiceName.COMMAND_DISPATCHER.toString(), group).getAbsoluteName());
        Builder<ManagedReferenceFactory> bindingBuilder = new BinderServiceBuilder<>(binding, builder.getServiceName(), CommandDispatcherFactory.class);
        return Arrays.asList(builder, bindingBuilder);
    }
}
