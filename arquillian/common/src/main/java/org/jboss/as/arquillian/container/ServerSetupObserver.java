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

package org.jboss.as.arquillian.container;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;

/**
 * @author Stuart Douglas
 */
public class ServerSetupObserver {


    @Inject
    private Instance<ManagementClient> managementClient;

    @Inject
    private Instance<ClassContext> classContextInstance;

    private final List<ServerSetupTask> current = new ArrayList<ServerSetupTask>();
    private final Map<String, ManagementClient> active = new HashMap<String, ManagementClient>();
    private Map<String, Integer> deployed;
    boolean afterClassRun = false;

    public synchronized void handleBeforeDeployment(@Observes BeforeDeploy event, Container container) throws Throwable {
        if (deployed == null) {
            deployed = new HashMap<String, Integer>();
            current.clear();
            afterClassRun = false;
        }
        if (deployed.containsKey(container.getName())) {
            deployed.put(container.getName(), deployed.get(container.getName()) + 1);
        } else {
            deployed.put(container.getName(), 1);
        }
        if (active.containsKey(container.getName())) {
            return;
        }

        final ClassContext classContext = classContextInstance.get();
        if (classContext == null) {
            return;
        }

        final Class<?> currentClass = classContext.getActiveId();
        final ContainerClassHolder holder = new ContainerClassHolder(container.getName(), currentClass);

        ServerSetup setup = currentClass.getAnnotation(ServerSetup.class);
        if (setup == null) {
            return;
        }
        final Class<? extends ServerSetupTask>[] classes = setup.value();
        if (current.isEmpty()) {
            for (Class<? extends ServerSetupTask> clazz : classes) {
                Constructor<? extends ServerSetupTask> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                current.add(ctor.newInstance());
            }
        } else {
            //this should never happen
            for (int i = 0; i < current.size(); ++i) {
                if (classes[i] != current.get(i).getClass()) {
                    throw new RuntimeException("Mismatched ServerSetupTask current is " + current + " but " + currentClass + " is expecting " + Arrays.asList(classes));
                }
            }
        }

        final ManagementClient client = managementClient.get();
        try {
            for (ServerSetupTask instance : current) {
                instance.setup(client, container.getName());
            }
        } catch (Throwable e) {
            //setup failed, so clear the current map
            current.clear();
            throw e;
        }

        active.put(container.getName(), client);
    }

    public synchronized void afterTestClass(@Observes AfterClass afterClass) throws Exception {
        if (current.isEmpty()) {
            return;
        }
        //clean up if there are no more deployments on the server
        //otherwise we clean up after the last deployment is removed
        final Iterator<Map.Entry<String, Integer>> it = deployed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> container = it.next();
            if (container.getValue() == 0) {
                if (active.containsKey(container.getKey())) {
                    ManagementClient client = active.get(container.getKey());
                    for (int i = current.size() - 1; i >= 0; i--) {
                        current.get(i).tearDown(client, container.getKey());
                    }
                    active.remove(container.getKey());
                    it.remove();
                }
            }
        }
        afterClassRun = true;
        if (deployed.isEmpty()) {
            deployed = null;
            current.clear();
            afterClassRun = false;
        }
    }

    public synchronized void handleAfterUndeploy(@Observes AfterUnDeploy afterDeploy, final Container container) throws Exception {
        if(deployed == null) {
            return;
        }
        int count = deployed.get(container.getName());
        deployed.put(container.getName(), --count);
        if (count == 0 && afterClassRun) {
            for (int i = current.size() - 1; i >= 0; i--) {
                current.get(i).tearDown(managementClient.get(), container.getName());
            }
            active.remove(container.getName());
            deployed.remove(container.getName());
        }
        if (deployed.isEmpty()) {
            deployed = null;
            current.clear();
            afterClassRun = false;
        }
    }


    private static final class ContainerClassHolder {
        private final Class<?> testClass;
        private final String name;

        private ContainerClassHolder(final String name, final Class<?> testClass) {
            this.name = name;
            this.testClass = testClass;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ContainerClassHolder that = (ContainerClassHolder) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (testClass != null ? !testClass.equals(that.testClass) : that.testClass != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = testClass != null ? testClass.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }


}
