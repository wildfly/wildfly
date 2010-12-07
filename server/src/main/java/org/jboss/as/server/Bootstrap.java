/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import java.util.List;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.threads.AsyncFuture;

/**
 * The application server bootstrap interface.  Get a new instance via {@link Factory#newInstance()}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Bootstrap {

    /**
     * Bootstrap a new server instance.  The list of updates should begin with extensions, followed by
     * subsystem adds, followed by deployments.  The boot action of each update will be executed; this is
     * the only time that this will happen.  This method will not block; the return value may be used to
     * wait for the result (with an optional timeout) or register an asynchronous callback.
     *
     * @param configuration the server configuration
     * @param bootUpdates the update list to apply
     * @param extraServices additional services to start and stop with the server instance
     * @return the future server controller
     */
    AsyncFuture<ServerController> start(Configuration configuration, List<Object> bootUpdates, List<ServiceActivator> extraServices);

    /**
     * The configuration for server bootstrap.
     */
    final class Configuration {
        private String name;
        private int portOffset;
        private ServerEnvironment serverEnvironment;
        private ModuleLoader moduleLoader;
        private long startTime = Module.getStartTime();

        /**
         * Get the server name; must be specified.
         *
         * @return the server name
         */
        public String getName() {
            return name;
        }

        /**
         * Set the server name; must be specified.
         *
         * @return the server name
         */
        public void setName(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            this.name = name;
        }

        /**
         * Get the port offset.
         *
         * @return the port offset
         */
        public int getPortOffset() {
            return portOffset;
        }

        /**
         * Set the port offset.
         *
         * @param portOffset the port offset
         */
        public void setPortOffset(final int portOffset) {
            if (portOffset < 0) {
                throw new IllegalArgumentException("portOffset may not be less than 0");
            }
            this.portOffset = portOffset;
        }

        /**
         * Get the server environment.
         *
         * @return the server environment
         */
        public ServerEnvironment getServerEnvironment() {
            return serverEnvironment;
        }

        /**
         * Set the server environment.
         *
         * @param serverEnvironment the server environment
         */
        public void setServerEnvironment(final ServerEnvironment serverEnvironment) {
            this.serverEnvironment = serverEnvironment;
        }

        /**
         * Get the application server module loader.
         *
         * @return the module loader
         */
        public ModuleLoader getModuleLoader() {
            return moduleLoader;
        }

        /**
         * Set the application server module loader.
         *
         * @param moduleLoader the module loader
         */
        public void setModuleLoader(final ModuleLoader moduleLoader) {
            this.moduleLoader = moduleLoader;
        }

        /**
         * Get the server start time to report in the logs.
         *
         * @return the server start time
         */
        public long getStartTime() {
            return startTime;
        }

        /**
         * Set the server start time to report in the logs.
         *
         * @param startTime the server start time
         */
        public void setStartTime(final long startTime) {
            this.startTime = startTime;
        }
    }

    /**
     * The factory for creating new instances of {@link Bootstrap}.
     */
    final class Factory {

        private Factory() {
        }

        /**
         * Create a new instance.
         *
         * @return the new bootstrap instance
         */
        public static Bootstrap newInstance() {
            return new BootstrapImpl();
        }
    }
}
