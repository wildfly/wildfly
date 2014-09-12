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

import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.GroupServiceBuilder;
import org.wildfly.clustering.spi.LocalGroupServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactoryServiceInstaller extends CommandDispatcherFactoryServiceInstaller implements LocalGroupServiceInstaller {

    private static final GroupServiceBuilder<CommandDispatcherFactory> BUILDER = new GroupServiceBuilder<CommandDispatcherFactory>() {
        @Override
        public ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target, ServiceName name, String group, ModuleIdentifier module) {
            return LocalCommandDispatcherFactoryService.build(target, name, group);
        }
    };

    public LocalCommandDispatcherFactoryServiceInstaller() {
        super(BUILDER);
    }
}
