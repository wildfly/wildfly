/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.entity.cmp;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;


import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * This hack should ensure that we always have a clean database in between tests. This
 * should prevent an h2 race condition where table metadata is gone
 * before the table is removed.
 */
public class TableCleaner implements ServiceActivator {

    private static Logger log = Logger.getLogger("TableCleaner");

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        String deployment = ((ModuleClassLoader) getClass().getClassLoader()).getModule().getIdentifier().getName().replaceFirst("deployment.", "");
        log.info("Activating cleaner for deployment: " + deployment);
        serviceActivatorContext.getServiceTarget().addService(ServiceName.of("cleaner-launcher-installer-thing"), new CleanerService())
                .addDependency(ServiceName.parse("jboss.deployment.unit.\"" + deployment + "\".jdbc.store-manager.start-barrier"))
                .install();
    }

    public static class CleanerService implements Service<Object> {

        @Override
        public void start(StartContext context) throws StartException {
            context.getController().getParent().addListener(new CleanerListener());
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public Object getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }

    public static class CleanerListener implements ServiceListener{
        @Override
        public void listenerAdded(ServiceController serviceController) {
        }

        @Override
        public void transition(ServiceController serviceController, ServiceController.Transition transition) {
            if (ServiceController.State.DOWN == transition.getAfter().getState()) {
                try {
                    log.info("NUKING!");
                    Driver driver = (Driver) Class.forName("org.h2.Driver", true, this.getClass().getClassLoader()).newInstance();
                    Properties props = new Properties();
                    props.setProperty("user", "sa");
                    props.setProperty("password", "sa");
                    Connection conn = driver.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", props);
                    conn.createStatement().execute("DROP ALL OBJECTS");
                    conn.close();
                } catch (Throwable t) {
                    log.error("NO NUKE!!", t);
                    return;
                }

                log.info("NUKE SUCCESS!");
            }
        }

        @Override
        public void serviceRemoveRequested(ServiceController serviceController) {
        }

        @Override
        public void serviceRemoveRequestCleared(ServiceController serviceController) {
        }

        @Override
        public void dependencyFailed(ServiceController serviceController) {
        }

        @Override
        public void dependencyFailureCleared(ServiceController serviceController) {
        }

        @Override
        public void immediateDependencyUnavailable(ServiceController serviceController) {
        }

        @Override
        public void immediateDependencyAvailable(ServiceController serviceController) {
        }

        @Override
        public void transitiveDependencyUnavailable(ServiceController serviceController) {
        }

        @Override
        public void transitiveDependencyAvailable(ServiceController serviceController) {
        }
    }
}