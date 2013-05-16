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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.server.mgmt.domain.HostControllerClient;
import org.jboss.as.server.mgmt.domain.HostControllerConnectionService;
import org.jboss.as.server.mgmt.domain.ServerBootOperationsService;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.wildfly.security.manager.WildFlySecurityManager;

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
    private final int initialOperationID;
    private final Properties properties = new Properties();

    public ServerStartTask(final String hostControllerName, final String serverName, final int portOffset, final int initialOperationID,
                           final List<ServiceActivator> startServices, final List<ModelNode> updates, final Map<String, String> launchProperties) {
        assert serverName != null && serverName.length() > 0  : "Server name \"" + serverName + "\" is invalid; cannot be null or blank";
        assert hostControllerName != null && hostControllerName.length() > 0 : "Host Controller name \"" + hostControllerName + "\" is invalid; cannot be null or blank";

        this.serverName = serverName;
        this.portOffset = portOffset;
        this.startServices = startServices;
        this.updates = updates;
        this.initialOperationID = initialOperationID;
        this.hostControllerName = hostControllerName;

        this.home = WildFlySecurityManager.getPropertyPrivileged("jboss.home.dir", null);
        String serverBaseDir = WildFlySecurityManager.getPropertyPrivileged("jboss.domain.servers.dir", null) + File.separatorChar + serverName;
        properties.setProperty(ServerEnvironment.SERVER_NAME, serverName);
        properties.setProperty(ServerEnvironment.HOME_DIR, home);
        properties.setProperty(ServerEnvironment.SERVER_BASE_DIR, serverBaseDir);
        properties.setProperty(ServerEnvironment.CONTROLLER_TEMP_DIR, WildFlySecurityManager.getPropertyPrivileged("jboss.domain.temp.dir", null));
        properties.setProperty(ServerEnvironment.DOMAIN_BASE_DIR, WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.DOMAIN_BASE_DIR, null));
        properties.setProperty(ServerEnvironment.DOMAIN_CONFIG_DIR, WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.DOMAIN_CONFIG_DIR, null));

        // Provide any other properties that standalone Main.determineEnvironment() would read
        // from system properties and pass in to ServerEnvironment
        setPropertyIfFound(launchProperties, ServerEnvironment.JAVA_EXT_DIRS, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.QUALIFIED_HOST_NAME, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.HOST_NAME, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.NODE_NAME, properties);
        @SuppressWarnings("deprecation")
        String deprecated = ServerEnvironment.MODULES_DIR;
        setPropertyIfFound(launchProperties, deprecated, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.BUNDLES_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_DATA_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_CONTENT_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_LOG_DIR, properties);
        setPropertyIfFound(launchProperties, ServerEnvironment.SERVER_TEMP_DIR, properties);
    }

    @Override
    public AsyncFuture<ServiceContainer> run(final List<ServiceActivator> runServices) {
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        final ProductConfig productConfig = new ProductConfig(Module.getBootModuleLoader(), home, properties);
        // Create server environment on the server, so that the system properties are getting initialized on the right side
        final ServerEnvironment providedEnvironment = new ServerEnvironment(hostControllerName, properties,

                WildFlySecurityManager.getSystemEnvironmentPrivileged(), null, null, ServerEnvironment.LaunchType.DOMAIN, RunningMode.NORMAL, productConfig);
        DomainServerCommunicationServices.updateOperationID(initialOperationID);

        // TODO perhaps have ConfigurationPersisterFactory as a Service
        final List<ServiceActivator> services = new ArrayList<ServiceActivator>(startServices);
        final ServerBootOperationsService service = new ServerBootOperationsService();
        // ModelController.boot() will block on this future in order to get the boot updates.
        final Future<ModelNode> bootOperations = service.getFutureResult();
        final ServiceActivator activator = new ServiceActivator() {
            @Override
            public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
                final ServiceTarget target = serviceActivatorContext.getServiceTarget();
                target.addService(ServiceName.JBOSS.append("server-boot-operations"), service)
                        .addDependency(Services.JBOSS_AS)
                        .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.getServerController())
                        .addDependency(HostControllerConnectionService.SERVICE_NAME, HostControllerClient.class, service.getClientInjector())
                        .addDependency(Services.JBOSS_SERVER_EXECUTOR, Executor.class, service.getExecutorInjector())
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
            }
        };
        services.add(activator);

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
                        try {
                            final ModelNode operations = bootOperations.get();
                            return operations.asList();
                        } catch (Exception e) {
                            throw new ConfigurationPersistenceException(e);
                        }
                    }
                };
                extensionRegistry.setWriterRegistry(persister);
                return persister;
            }
        };
        configuration.setConfigurationPersisterFactory(configurationPersisterFactory);
        return bootstrap.bootstrap(configuration, services);
    }

    @Override
    public void validateObject() throws InvalidObjectException {
        if (serverName == null) {
            throw ServerMessages.MESSAGES.invalidObject("serverName");
        }
        if(hostControllerName == null) {
            throw ServerMessages.MESSAGES.invalidObject("hostControllerName");
        }
        if (portOffset < 0) {
            throw ServerMessages.MESSAGES.invalidPortOffset();
        }
        if (updates == null) {
            throw ServerMessages.MESSAGES.invalidObject("updates");
        }
        if (startServices == null) {
            throw ServerMessages.MESSAGES.invalidObject("startServices");
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
