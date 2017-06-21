/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.ClusterTopologyListener;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.discovery.AttributeValue;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.impl.MutableDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;

/**
 * The EJB server association service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AssociationService implements Service<AssociationService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association");

    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjector = new InjectedValue<>();
    private final InjectedValue<RegistryCollector> registryCollectorInjector = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendControllerInjector = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> serverEnvironmentServiceInjector = new InjectedValue<>();

    private final Object serviceLock = new Object();
    private final Set<String> ourClusters = new HashSet<>();
    private final Set<EJBModuleIdentifier> ourModules = new HashSet<>();
    private volatile ServiceURL cachedServiceURL;

    private final MutableDiscoveryProvider mutableDiscoveryProvider = new MutableDiscoveryProvider();

    private String ourNodeName;
    private AssociationImpl value;
    private ListenerHandle handle1;
    private ListenerHandle handle2;

    public AssociationService() {
    }

    public void start(final StartContext context) throws StartException {
        // todo suspendController
        //noinspection unchecked
        value = new AssociationImpl(deploymentRepositoryInjector.getValue(), registryCollectorInjector.getValue());

        // register the fact that the local receiver can handle invocations targeted at its node name
        final ServiceURL.Builder builder = new ServiceURL.Builder();
        builder.setAbstractType("ejb").setAbstractTypeAuthority("jboss");
        builder.setUri(Affinity.LOCAL.getUri());
        ourNodeName = serverEnvironmentServiceInjector.getValue().getNodeName();
        builder.addAttribute(EJBClientContext.FILTER_ATTR_NODE, AttributeValue.fromString(ourNodeName));

        // track deployments at an association level for local dispatchers to utilize
        handle1 = value.registerModuleAvailabilityListener(new ModuleAvailabilityListener() {

            public void moduleAvailable(final List<EJBModuleIdentifier> modules) {
                synchronized (serviceLock) {
                    ourModules.addAll(modules);
                    cachedServiceURL = null;
                }
            }

            public void moduleUnavailable(final List<EJBModuleIdentifier> modules) {
                synchronized (serviceLock) {
                    ourModules.removeAll(modules);
                    cachedServiceURL = null;
                }
            }
        });
        handle2 = value.registerClusterTopologyListener(new ClusterTopologyListener() {
            public void clusterTopology(final List<ClusterInfo> clusterInfoList) {
                synchronized (serviceLock) {
                    for (ClusterInfo clusterInfo : clusterInfoList) {
                        for (NodeInfo nodeInfo : clusterInfo.getNodeInfoList()) {
                            if (nodeInfo.getNodeName().equals(ourNodeName)) {
                                ourClusters.add(clusterInfo.getClusterName());
                            }
                        }
                    }
                    cachedServiceURL = null;
                }
            }

            public void clusterRemoval(final List<String> clusterNames) {
                synchronized (serviceLock) {
                    ourClusters.removeAll(clusterNames);
                    cachedServiceURL = null;
                }
            }

            public void clusterNewNodesAdded(final ClusterInfo clusterInfo) {
                synchronized (serviceLock) {
                    for (NodeInfo nodeInfo : clusterInfo.getNodeInfoList()) {
                        if (nodeInfo.getNodeName().equals(ourNodeName)) {
                            ourClusters.add(clusterInfo.getClusterName());
                        }
                    }
                    cachedServiceURL = null;
                }
            }

            public void clusterNodesRemoved(final List<ClusterRemovalInfo> clusterRemovalInfoList) {
                synchronized (serviceLock) {
                    for (ClusterRemovalInfo removalInfo : clusterRemovalInfoList) {
                        if (removalInfo.getNodeNames().contains(ourNodeName)) {
                            ourClusters.remove(removalInfo.getClusterName());
                        }
                    }
                    cachedServiceURL = null;
                }
            }
        });
        // do this last
        mutableDiscoveryProvider.setDiscoveryProvider((serviceType, filterSpec, result) -> {
            ServiceURL serviceURL = this.cachedServiceURL;
            if (serviceURL == null) {
                synchronized (serviceLock) {
                    serviceURL = this.cachedServiceURL;
                    if (serviceURL == null) {
                        ServiceURL.Builder b = new ServiceURL.Builder();
                        b.setUri(Affinity.LOCAL.getUri()).setAbstractType("ejb").setAbstractTypeAuthority("jboss");
                        b.addAttribute(EJBClientContext.FILTER_ATTR_NODE, AttributeValue.fromString(ourNodeName));
                        for (String cluster : ourClusters) {
                            b.addAttribute(EJBClientContext.FILTER_ATTR_CLUSTER, AttributeValue.fromString(cluster));
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
    }

    public void stop(final StopContext context) {
        value = null;
        handle1.close();
        handle2.close();
        handle1 = null;
        handle2 = null;
        mutableDiscoveryProvider.setDiscoveryProvider(DiscoveryProvider.EMPTY);
        synchronized (serviceLock) {
            cachedServiceURL = null;
            ourModules.clear();
            ourClusters.clear();
        }
    }

    public AssociationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentServiceInjector() {
        return serverEnvironmentServiceInjector;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjector() {
        return deploymentRepositoryInjector;
    }

    public InjectedValue<RegistryCollector> getRegistryCollectorInjector() {
        return registryCollectorInjector;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjector() {
        return suspendControllerInjector;
    }

    public DiscoveryProvider getLocalDiscoveryProvider() {
        return mutableDiscoveryProvider;
    }

    public Association getAssociation() {
        return value;
    }

    void setExecutor(Executor executor) {
        if(value != null) {
            value.setExecutor(executor);
        }
    }
}

