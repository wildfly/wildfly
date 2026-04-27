/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.local;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.discovery.AttributeValue;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.impl.MutableDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A service which provides EJB client discovery information for local deployments.
 */
public class LocalEJBDiscoveryProviderService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "localDiscovery");

    private Consumer<DiscoveryProvider> discoveryProviderConsumer;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
    private final Supplier<DeploymentRepository> deploymentRepositorySupplier;
    private final List<Supplier<Registry>> clientMappingsRegistriesSupplier;

    private final Object serviceLock = new Object();
    private final MutableDiscoveryProvider mutableDiscoveryProvider = new MutableDiscoveryProvider();
    private volatile DeploymentRepositoryListener deploymentRepositoryListener;
    private final Set<EJBModuleIdentifier> ourModules = new HashSet<>();
    private volatile ServiceURL cachedServiceURL;

    public LocalEJBDiscoveryProviderService(final Consumer<DiscoveryProvider> discoveryProviderConsumer,
                                            final Supplier<ServerEnvironment> serverEnvironmentSupplier,
                                            final Supplier<DeploymentRepository> deploymentRepositorySuplier,
                                            final List<Supplier<Registry>> clientMappingsRegistriesSupplier) {
        this.discoveryProviderConsumer = discoveryProviderConsumer;
        this.serverEnvironmentSupplier = serverEnvironmentSupplier;
        this.deploymentRepositorySupplier = deploymentRepositorySuplier;
        this.clientMappingsRegistriesSupplier = clientMappingsRegistriesSupplier;
    }

    @Override
    public void start(StartContext context) throws StartException {
        String ourNodeName = serverEnvironmentSupplier.get().getNodeName();

        // define a listener to listen for deployments on this node
        deploymentRepositoryListener = new DeploymentRepositoryListener() {
            @Override
            public void listenerAdded(DeploymentRepository repository) {
                if (!repositoryIsSuspended()) {
                    // only add the initial list if the deployment repository is not in a suspended state
                    synchronized (serviceLock) {
                        for (DeploymentModuleIdentifier deploymentModuleIdentifier : repository.getStartedModules().keySet()) {
                            EJBModuleIdentifier ejbModuleIdentifier = toModuleIdentifier(deploymentModuleIdentifier);
                            ourModules.add(ejbModuleIdentifier);
                        }
                        cachedServiceURL = null;
                    }
                    EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Sending initial module availability to local discovery provider: server is not suspended");
                } else {
                    cachedServiceURL = null;
                    EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Sending empty module availability to local discovery provider: server is suspended");
                }
            }

            @Override
            public void deploymentAvailable(DeploymentModuleIdentifier deployment, ModuleDeployment moduleDeployment) {
            }

            @Override
            public void deploymentStarted(DeploymentModuleIdentifier deployment, ModuleDeployment moduleDeployment) {
                // only mark modules as available until module has started (WFLY-13009)
                synchronized (serviceLock) {
                    ourModules.add(toModuleIdentifier(deployment));
                    cachedServiceURL = null;
                }
            }

            @Override
            public void deploymentRemoved(DeploymentModuleIdentifier deployment) {
                synchronized (serviceLock) {
                    ourModules.remove(toModuleIdentifier(deployment));
                    cachedServiceURL = null;
                }
            }

            @Override
            public void deploymentSuspended(DeploymentModuleIdentifier deployment) {
                synchronized (serviceLock) {
                    ourModules.remove(toModuleIdentifier(deployment));
                    cachedServiceURL = null;
                }
            }

            @Override
            public void deploymentResumed(DeploymentModuleIdentifier deployment) {
                synchronized (serviceLock) {
                    ourModules.add(toModuleIdentifier(deployment));
                    cachedServiceURL = null;
                }
            }

            private boolean repositoryIsSuspended() {
                return deploymentRepositorySupplier.get().isSuspended();
            }
        };
        // register the listener with the deployment repository
        deploymentRepositorySupplier.get().addListener(deploymentRepositoryListener);

        // define a discovery provider based on the set of modules deployed on this node
        mutableDiscoveryProvider.setDiscoveryProvider((serviceType, filterSpec, result) -> {
            ServiceURL serviceURL = this.cachedServiceURL;
            if (serviceURL == null) {
                synchronized (serviceLock) {
                    serviceURL = this.cachedServiceURL;
                    if (serviceURL == null) {
                        ServiceURL.Builder b = new ServiceURL.Builder();
                        b.setUri(Affinity.LOCAL.getUri()).setAbstractType("ejb").setAbstractTypeAuthority("jboss");
                        b.addAttribute(EJBClientContext.FILTER_ATTR_NODE, AttributeValue.fromString(ourNodeName));

                        // in cluster membership only when the client mappings registries are available
                        if (clientMappingsRegistriesSupplier != null) {
                            for (Supplier<Registry> registrySupplier : clientMappingsRegistriesSupplier) {
                                Group<GroupMember> group = registrySupplier.get().getGroup();
                                if (!group.isSingleton()) {
                                    b.addAttribute(EJBClientContext.FILTER_ATTR_CLUSTER, AttributeValue.fromString(group.getName()));
                                }
                            }
                        }
                        for (EJBModuleIdentifier moduleIdentifier : ourModules) {
                            final String appName = moduleIdentifier.getAppName();
                            final String moduleName = moduleIdentifier.getModuleName();
                            final String distinctName = moduleIdentifier.getDistinctName();
                            if (distinctName.isEmpty()) {
                                if (appName.isEmpty()) {
                                    b.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE, AttributeValue.fromString(moduleName));
                                } else {
                                    b.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE, AttributeValue.fromString(appName + '/' + moduleName));
                                }
                            } else {
                                if (appName.isEmpty()) {
                                    b.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE_DISTINCT, AttributeValue.fromString(moduleName + '/' + distinctName));
                                } else {
                                    b.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE_DISTINCT, AttributeValue.fromString(appName + '/' + moduleName + '/' + distinctName));
                                }
                            }
                        }
                        serviceURL = this.cachedServiceURL = b.create();
                    }
                }
            }
            if (serviceURL.satisfies(filterSpec)) {
                result.addMatch(serviceURL);
            }
            result.complete();
            return DiscoveryRequest.NULL;
        });

        // provide the discovery provider to services that need it
        discoveryProviderConsumer.accept(mutableDiscoveryProvider);
    }

    @Override
    public void stop(StopContext context) {
        // cease to provide the discovery provider
        discoveryProviderConsumer.accept(null);
    }

    private EJBModuleIdentifier toModuleIdentifier(DeploymentModuleIdentifier identifier) {
        return new EJBModuleIdentifier(identifier.getApplicationName(), identifier.getModuleName(), identifier.getDistinctName());
    }
}
