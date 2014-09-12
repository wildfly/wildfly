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
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.GroupServiceBuilder;
import org.wildfly.clustering.spi.GroupServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceNameFactory;
import org.wildfly.clustering.spi.GroupServiceNames;

/**
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryServiceInstaller implements GroupServiceInstaller {
    private final Logger logger = Logger.getLogger(this.getClass());

    private final GroupServiceBuilder<CommandDispatcherFactory> builder;

    protected CommandDispatcherFactoryServiceInstaller(GroupServiceBuilder<CommandDispatcherFactory> builder) {
        this.builder = builder;
    }

    private static ContextNames.BindInfo createBinding(String group) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, GroupServiceNames.COMMAND_DISPATCHER.toString(), group).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String group) {
        return Arrays.asList(GroupServiceNames.COMMAND_DISPATCHER.getServiceName(group), createBinding(group).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String group, ModuleIdentifier module) {
        ServiceName name = GroupServiceNames.COMMAND_DISPATCHER.getServiceName(group);
        ContextNames.BindInfo bindInfo = createBinding(group);

        this.logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceBuilder<CommandDispatcherFactory> builder = this.builder.build(target, name, group, module).setInitialMode(ServiceController.Mode.ON_DEMAND);
        ServiceBuilder<ManagedReferenceFactory> binderBuilder = new BinderServiceBuilder(target).build(bindInfo, name, CommandDispatcherFactory.class);

        return Arrays.asList(builder.install(), binderBuilder.install());
    }
}
