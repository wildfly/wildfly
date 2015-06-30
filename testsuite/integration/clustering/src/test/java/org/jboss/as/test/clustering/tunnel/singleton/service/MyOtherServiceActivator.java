/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.tunnel.singleton.service;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.test.clustering.cluster.singleton.service.Environment;
import org.jboss.as.test.clustering.cluster.singleton.service.MyService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.server.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_1;

/**
 * @author Tomas Hofman
 */
public class MyOtherServiceActivator implements ServiceActivator {

    private static final String CONTAINER_NAME = "server";
    private static final String CACHE_NAME = "default";
    public static final String PREFERRED_NODE = NODE_1;

    @Override
    public void activate(ServiceActivatorContext context) {
        MyOtherService service = new MyOtherService();

        ServiceController<?> factoryService = context.getServiceRegistry().getRequiredService(SingletonServiceBuilderFactory.SERVICE_NAME.append(CONTAINER_NAME, CACHE_NAME));
        SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) factoryService.getValue();
        factory.createSingletonServiceBuilder(MyOtherService.SERVICE_NAME, service)
                .electionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(PREFERRED_NODE)))
                .build(context.getServiceTarget())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.getEnvInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
