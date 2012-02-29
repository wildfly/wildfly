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

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.threads.AsyncFuture;

/**
 * This is the task used by the Host Controller and passed to a Server instance
 * in order to bootstrap it from a remote source process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Mike M. Clark
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ServerStartTask implements ServerTask, Serializable, ObjectInputValidation {

    private static final long serialVersionUID = -1037124182656400874L;

    private final String serverName;
    private final int portOffset;
    private final String hostControllerName;
    private final String home;
    private final List<ServiceActivator> startServices;
    private final List<ModelNode> updates;
    private final Properties properties = new Properties();

    public ServerStartTask(final String hostControllerName, final String serverName, final int portOffset,
                           final List<ServiceActivator> startServices, final List<ModelNode> updates, final Map<String, String> launchProperties) {
        if (serverName == null || serverName.length() == 0) {
            throw new IllegalArgumentException("Server name \"" + serverName + "\" is invalid; cannot be null or blank");
        }
        if (hostControllerName == null || hostControllerName.length() == 0) {
            throw new IllegalArgumentException("Host Controller name \"" + hostControllerName + "\" is invalid; cannot be null or blank");
        }
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.startServices = startServices;
        this.updates = updates;

        this.hostControllerName = hostControllerName;

        this.home = SecurityActions.getSystemProperty("jboss.home.dir");
        String serverBaseDir = SecurityActions.getSystemProperty("jboss.domain.servers.dir") + File.separatorChar + serverName;
        properties.setProperty(ServerEnvironment.SERVER_NAME, serverName);
        properties.setProperty(ServerEnvironment.HOME_DIR, home);
        properties.setProperty(ServerEnvironment.SERVER_BASE_DIR, serverBaseDir);
        properties.setProperty(ServerEnvironment.CONTROLLER_TEMP_DIR, SecurityActions.getSystemProperty("jboss.domain.temp.dir"));
        properties.setProperty(ServerEnvironment.DOMAIN_BASE_DIR, SecurityActions.getSystemProperty(ServerEnvironment.DOMAIN_BASE_DIR));
        properties.setProperty(ServerEnvironment.DOMAIN_CONFIG_DIR, SecurityActions.getSystemProperty(ServerEnvironment.DOMAIN_CONFIG_DIR));

        // Set the optional properties
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_DATA_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_LOG_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_TEMP_DIR, properties);
    }

    @Override
    public AsyncFuture<ServiceContainer> run(final List<ServiceActivator> runServices) {
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        final ProductConfig productConfig = new ProductConfig(Module.getBootModuleLoader(), home);
        // Create server environment on the server, so that the system properties are getting initialized on the right side
        final ServerEnvironment providedEnvironment = new ServerEnvironment(hostControllerName, properties, SecurityActions.getSystemEnvironment(), null, ServerEnvironment.LaunchType.DOMAIN, RunningMode.NORMAL, productConfig);
        final Bootstrap.Configuration configuration = new Bootstrap.Configuration(providedEnvironment);
        final ExtensionRegistry extensionRegistry = configuration.getExtensionRegistry();
        final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = new Bootstrap.ConfigurationPersisterFactory() {
            @Override
            public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
                ExtensibleConfigurationPersister persister = new AbstractConfigurationPersister(new StandaloneXml(configuration.getModuleLoader(), executorService, extensionRegistry)) {

                    private final PersistenceResource pr = new PersistenceResource() {

                        @Override
                        public void commit() {
                        }

                        @Override
                        public void rollback() {
                        }
                    };

                    @Override
                    public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
                        return pr;
                    }

                    @Override
                    public List<ModelNode> load() throws ConfigurationPersistenceException {
                        return updates;
                    }
                };
                extensionRegistry.setWriterRegistry(persister);
                return persister;
            }
        };
        configuration.setConfigurationPersisterFactory(configurationPersisterFactory);
        return bootstrap.bootstrap(configuration, startServices);
    }

    @Override
    public void validateObject() throws InvalidObjectException {
        if (serverName == null) {
            throw new InvalidObjectException("serverName is null");
        }
        if(hostControllerName == null) {
            throw new InvalidObjectException("hostControllerName is null");
        }
        if (portOffset < 0) {
            throw new InvalidObjectException("portOffset is out of range");
        }
        if (updates == null) {
            throw new InvalidObjectException("updates is null");
        }
        if (startServices == null) {
            throw new InvalidObjectException("startServices is null");
        }
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        ois.registerValidation(this, 100);
    }

    static void setPropertyIfFound(final Map<String, String> launchProperties, final String key, final Properties properties) {
        if (launchProperties.containsKey(key)) {
            properties.setProperty(key, launchProperties.get(key));
        }
    }
}
