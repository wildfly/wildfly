/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderServiceBuilder;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Name;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests wrt {@link org.jboss.as.naming.service.BinderServiceBuilder}. 
 * @author Eduardo Martins
 */
public class BinderServiceBuilderTestCase {
    private ServiceContainer container;
    private WritableServiceBasedNamingStore javaNamingStore;
    private WritableServiceBasedNamingStore jbossNamingStore;

    @Before
    public void setup() throws Exception {
        container = ServiceContainer.Factory.create();
        javaNamingStore = setupNamingStore(ContextNames.JAVA_CONTEXT_SERVICE_NAME);
        jbossNamingStore = setupNamingStore(ContextNames.JBOSS_CONTEXT_SERVICE_NAME);
    }

    private WritableServiceBasedNamingStore setupNamingStore(ServiceName serviceName) throws InterruptedException {
        final NamingStoreService namingStoreService = new NamingStoreService();
        final ServiceController controller = container.addService(serviceName, namingStoreService).install();
        awaitStability(controller);
        return (WritableServiceBasedNamingStore) namingStoreService.getValue();
    }

    private void awaitStability(ServiceController controller) throws InterruptedException {
        final StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            monitor.awaitStability();
        } finally {
            monitor.removeController(controller);
        }
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
        javaNamingStore = null;
        jbossNamingStore = null;
    }

    /**
     * Test for installing a binder service using the builder, adding also an alias.
     * @throws Exception
     */
    @Test
    public void testBinderServiceBuilder() throws Exception {
        // assert naming stores have no bindings
        final Name contextName = new CompositeName("");
        assertEquals(0, jbossNamingStore.listBindings(contextName).size());
        assertEquals(0, javaNamingStore.listBindings(contextName).size());
        // install a bind and alias using the binder service builder
        final String bindName = "name";
        final String bindValue = "value";
        ContextNames.BindInfo jbossBindInfo = ContextNames.bindInfoFor("java:jboss/"+bindName);
        final ServiceController controller = new BinderServiceBuilder(jbossBindInfo, container, bindValue)
                .addAliases(jbossBindInfo.alias(ContextNames.JAVA_CONTEXT_SERVICE_NAME))
                .install();
        awaitStability(controller);
        // assert bind and alias are in stores
        List<Binding> jbossBindings = jbossNamingStore.listBindings(contextName);
        assertEquals(1, jbossBindings.size());
        assertEquals(bindName, jbossBindings.get(0).getName());
        assertEquals(bindValue, jbossBindings.get(0).getObject());
        List<Binding> javaBindings = javaNamingStore.listBindings(contextName);
        assertEquals(1, javaBindings.size());
        assertEquals(bindName, javaBindings.get(0).getName());
        assertEquals(bindValue, javaBindings.get(0).getObject());
        // assert lookups
        final Name lookupName = new CompositeName(bindName);
        assertEquals(jbossNamingStore.lookup(lookupName), bindValue);
        assertEquals(javaNamingStore.lookup(lookupName), bindValue);
        // remove binder service
        controller.setMode(ServiceController.Mode.REMOVE);
        awaitStability(controller);
        // assert naming stores have no bindings
        assertEquals(0, jbossNamingStore.listBindings(contextName).size());
        assertEquals(0, javaNamingStore.listBindings(contextName).size());
    }
}
