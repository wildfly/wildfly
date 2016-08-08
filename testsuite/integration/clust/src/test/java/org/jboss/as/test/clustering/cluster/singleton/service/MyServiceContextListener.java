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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.clustering.singleton.election.NamePreference;
import org.jboss.as.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.jboss.as.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

@WebListener
public class MyServiceContextListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(MyServiceContextListener.class);

    public static final String PREFERRED_NODE = NODE_2;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        MyService service = new MyService();
        SingletonService<Environment> singleton = new SingletonService<Environment>(service, MyService.SERVICE_NAME);
        singleton.setElectionPolicy(new PreferredSingletonElectionPolicy(new NamePreference(PREFERRED_NODE + "/" + SingletonService.DEFAULT_CONTAINER), new SimpleSingletonElectionPolicy()));
        ServiceController<Environment> controller = singleton.build(ServiceContainerHelper.getCurrentServiceContainer())
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.getEnvInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install()
        ;
        try {
            ServiceContainerHelper.start(controller);
        } catch (StartException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        log.info(String.format("Initiating removal of %s", MyService.SERVICE_NAME.getCanonicalName()));
        ServiceContainerHelper.remove(ServiceContainerHelper.getCurrentServiceContainer().getRequiredService(MyService.SERVICE_NAME));
        log.info(String.format("Removal of %s complete", MyService.SERVICE_NAME.getCanonicalName()));
        this.verifyRemoved(MyService.SERVICE_NAME.append("service"));
        this.verifyRemoved(MyService.SERVICE_NAME.append("singleton"));
    }

    private void verifyRemoved(ServiceName name) {
        ServiceController<?> controller = ServiceContainerHelper.getCurrentServiceContainer().getService(name);
        if ((controller != null) && (controller.getState() != ServiceController.State.REMOVED)) {
            throw new IllegalStateException(String.format("%s state = %s", name.getCanonicalName(), controller.getState()));
        }
    }
}
