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

import java.util.Collection;
import java.util.EnumSet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.clustering.singleton.election.NamePreference;
import org.jboss.as.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.jboss.as.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceController.Transition;

@WebListener
public class MyServiceContextListener implements ServletContextListener {

    public static final String PREFERRED_NODE = "node-udp-1";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        MyService service = new MyService();
        SingletonService<Environment> singleton = new SingletonService<Environment>(service, MyService.SERVICE_NAME);
        singleton.setElectionPolicy(new PreferredSingletonElectionPolicy(new NamePreference(PREFERRED_NODE + "/" + SingletonService.DEFAULT_CONTAINER), new SimpleSingletonElectionPolicy()));
        singleton.setClassResolver(new SimpleClassResolver(this.getClass().getClassLoader()));
        ServiceController<Environment> controller = singleton.build(CurrentServiceContainer.getServiceContainer())
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.getEnvInjector())
            .install()
        ;
        controller.setMode(ServiceController.Mode.ACTIVE);
        wait(controller, EnumSet.of(ServiceController.State.DOWN, ServiceController.State.STARTING), ServiceController.State.UP);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ServiceController<?> controller = CurrentServiceContainer.getServiceContainer().getRequiredService(MyService.SERVICE_NAME);
        controller.setMode(ServiceController.Mode.REMOVE);
        wait(controller, EnumSet.of(ServiceController.State.UP, ServiceController.State.STOPPING, ServiceController.State.DOWN), ServiceController.State.REMOVED);
    }

    private static <T> void wait(ServiceController<T> controller, Collection<ServiceController.State> expectedStates, ServiceController.State targetState) {
        if (controller.getState() != targetState) {
            ServiceListener<T> listener = new NotifyingServiceListener<T>();
            controller.addListener(listener);
            try {
                synchronized (controller) {
                    while (expectedStates.contains(controller.getState())) {
                        System.out.println(String.format("Service controller state is %s, waiting for transition to %s", controller.getState(), targetState));
                        controller.wait();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            controller.removeListener(listener);
            ServiceController.State state = controller.getState();
            if (state != targetState) {
                throw new IllegalStateException(String.format("Failed to wait for state to transition to %s.  Current state is %s", targetState, state), controller.getStartException());
            }
        }
    }

    private static class NotifyingServiceListener<T> extends AbstractServiceListener<T> {
        @Override
        public void transition(ServiceController<? extends T> controller, Transition transition) {
            synchronized (controller) {
                controller.notify();
            }
        }
    }
}
