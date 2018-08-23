/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.ActiveServiceSupplier;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.service.SingletonPolicy;

/**
 * @author Paul Ferraro
 */
public class NodeServicePolicyActivator implements ServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "default-policy");

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceTarget target = context.getServiceTarget();
        SingletonPolicy policy = new ActiveServiceSupplier<SingletonPolicy>(context.getServiceRegistry(), ServiceName.parse(SingletonDefaultRequirement.POLICY.getName())).setTimeout(Duration.ofSeconds(30)).get();
        ServiceBuilder<?> builder = policy.createSingletonServiceConfigurator(SERVICE_NAME).build(target);
        Consumer<Node> member = builder.provides(SERVICE_NAME);
        Supplier<Group> group = builder.requires(ServiceName.parse("org.wildfly.clustering.default-group"));
        Service service = new FunctionalService<>(member, Group::getLocalMember, group);
        builder.setInstance(service).install();
    }
}
