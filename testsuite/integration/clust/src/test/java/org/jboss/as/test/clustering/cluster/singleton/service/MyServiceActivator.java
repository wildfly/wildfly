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

package org.jboss.as.test.clustering.cluster.singleton.service;

import static org.jboss.as.test.clustering.ClusteringTestConstants.NODE_2;

import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.clustering.singleton.election.NamePreference;
import org.jboss.as.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.jboss.as.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class MyServiceActivator implements ServiceActivator {

    public static final String PREFERRED_NODE = NODE_2;

    @Override
    public void activate(ServiceActivatorContext context) {
        this.install(MyService.DEFAULT_SERVICE_NAME, 1, context);
        this.install(MyService.QUORUM_SERVICE_NAME, 2, context);
    }

    private void install(ServiceName name, int quorum, ServiceActivatorContext context) {
        InjectedValue<ServerEnvironment> env = new InjectedValue<ServerEnvironment>();
        MyService service = new MyService(env);
        SingletonService<Environment> singleton = new SingletonService<Environment>(service, name, quorum);
        singleton.setElectionPolicy(new PreferredSingletonElectionPolicy(new SimpleSingletonElectionPolicy(), new NamePreference(PREFERRED_NODE + "/" + SingletonService.DEFAULT_CONTAINER)));
        singleton.build(context.getServiceTarget())
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install()
        ;
    }
}
