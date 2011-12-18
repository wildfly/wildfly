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

package org.jboss.as.server;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.xml.namespace.QName;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.controller.persistence.BackupXmlConfigurationPersister;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.threads.AsyncFuture;

/**
 * The application server bootstrap interface.  Get a new instance via {@link Factory#newInstance()}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 */
public interface Bootstrap {

    /**
     * Bootstrap a new server instance.  The list of updates should begin with extensions, followed by
     * subsystem adds, followed by deployments.  The boot action of each update will be executed; this is
     * the only time that this will happen.  This method will not block; the return value may be used to
     * wait for the result (with an optional timeout) or register an asynchronous callback.
     *
     * @param configuration the server configuration
     * @param extraServices additional services to start and stop with the server instance
     * @return the future service container
     */
    AsyncFuture<ServiceContainer> bootstrap(Configuration configuration, List<ServiceActivator> extraServices);

    /**
     * Calls {@link #bootstrap(Configuration, List)} to bootstrap the container. The value for the returned future
     * becomes available when all installed services have been started/failed.
     *
     * @param configuration the server configuration
     * @param extraServices additional services to start and stop with the server instance
     * @return the future service container
     */
    AsyncFuture<ServiceContainer> startup(Configuration configuration, List<ServiceActivator> extraServices);

    /**
     * The configuration for server bootstrap.
     */
    final class Configuration {

        private final ServerEnvironment serverEnvironment;
        private final ExtensionRegistry extensionRegistry;
        private ModuleLoader moduleLoader = Module.getBootModuleLoader();
        private ConfigurationPersisterFactory configurationPersisterFactory;
        private long startTime = Module.getStartTime();

        public Configuration(final ServerEnvironment serverEnvironment) {
            assert serverEnvironment == null : "serverEnvironment is null";
            this.serverEnvironment = serverEnvironment;
            this.extensionRegistry = new ExtensionRegistry(serverEnvironment.getLaunchType().getProcessType());
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
        }

        /**
         * Get the server environment.
         *
         * @return the server environment. Will not be {@code null}
         */
        public ServerEnvironment getServerEnvironment() {
            return serverEnvironment;
        }

        /**
         * Get the extension registry.
         *
         * @return the extension registry. Will not be {@code null}
         */
        public ExtensionRegistry getExtensionRegistry() {
            return extensionRegistry;
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
         * Get the factory for the configuration persister to use.
         *
         * @return the configuration persister factory
         */
        public synchronized ConfigurationPersisterFactory getConfigurationPersisterFactory() {
            if (configurationPersisterFactory == null) {
//                if (serverEnvironment == null) {
//                    final ModuleLoader localModuleLoader = this.moduleLoader;
//                    configurationPersisterFactory = new ConfigurationPersisterFactory() {
//                        @Override
//                        public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
//                            return new NullConfigurationPersister(new StandaloneXml(localModuleLoader, executorService));
//                        }
//                    };
//                }
//                else {
                    configurationPersisterFactory = new ConfigurationPersisterFactory() {
                        @Override
                        public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
                            QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
                            StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
                            BackupXmlConfigurationPersister persister = new BackupXmlConfigurationPersister(serverEnvironment.getServerConfigurationFile(), rootElement, parser, parser);
                            persister.registerAdditionalRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), "server"), parser);
                            extensionRegistry.setWriterRegistry(persister);
                            return persister;
                        }
                    };

//                }
            }
            return configurationPersisterFactory;
        }

        /**
         * Set the configuration persister factory to use.
         *
         * @param configurationPersisterFactory the configuration persister factory
         */
        public synchronized void setConfigurationPersisterFactory(final ConfigurationPersisterFactory configurationPersisterFactory) {
            this.configurationPersisterFactory = configurationPersisterFactory;
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

    /** A factory for the {@link ExtensibleConfigurationPersister} to be used by this server */
    interface ConfigurationPersisterFactory {
        /**
         *
         * @param serverEnvironment the server environment. Cannot be {@code null}
         * @param executorService an executor service the configuration persister can use.
         *                        May be {@code null} if asynchronous work is not supported
         * @return the configuration persister. Will not be {@code null}
         */
        ExtensibleConfigurationPersister createConfigurationPersister(final ServerEnvironment serverEnvironment, final ExecutorService executorService);
    }

    /**
     * The factory for creating new instances of {@link org.jboss.as.server.Bootstrap}.
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
