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

package org.wildfly.clustering.server.group;

import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.GroupServiceBuilder;
import org.wildfly.clustering.spi.GroupServiceNameFactory;
import org.wildfly.clustering.spi.GroupServiceNames;

/**
 * @author Paul Ferraro
 */
public class GroupServiceInstaller implements org.wildfly.clustering.spi.GroupServiceInstaller {
    private final Logger logger = Logger.getLogger(this.getClass());

    private final GroupServiceBuilder<Group> builder;

    protected GroupServiceInstaller(GroupServiceBuilder<Group> builder) {
        this.builder = builder;
    }

    private static ContextNames.BindInfo createBinding(String group) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, GroupServiceNames.GROUP.toString(), group).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String group) {
        return Arrays.asList(GroupServiceNames.GROUP.getServiceName(group), createBinding(group).getBinderServiceName());
    }

    @Override
    public void install(ServiceTarget target, String group, ModuleIdentifier module) {
        ServiceName name = GroupServiceNames.GROUP.getServiceName(group);
        ContextNames.BindInfo bindInfo = createBinding(group);

        this.logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        this.builder.build(target, name, group, module).setInitialMode(ON_DEMAND).install();

        new BinderServiceBuilder(target).build(bindInfo, name, Group.class).install();
    }
}
