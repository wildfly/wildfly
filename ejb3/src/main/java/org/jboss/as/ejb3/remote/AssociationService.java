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

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.ejb.server.Association;
import org.jboss.ejb.server.ListenerHandle;
import org.jboss.ejb.server.ModuleAvailabilityListener;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.registry.Registry;
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
    @SuppressWarnings("rawtypes")
    private final List<Map.Entry<Value<ProtocolSocketBinding>, Value<Registry>>> clientMappingsRegistries = new LinkedList<>();
    private final InjectedValue<SuspendController> suspendControllerInjector = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> serverEnvironmentServiceInjector = new InjectedValue<>();

    private final Object serviceLock = new Object();
    private final Set<EJBModuleIdentifier> ourModules = new HashSet<>();
    private volatile ServiceURL cachedServiceURL;

    private final MutableDiscoveryProvider mutableDiscoveryProvider = new MutableDiscoveryProvider();

    private volatile AssociationImpl value;
    private volatile ListenerHandle moduleAvailabilityListener;

    @Override
    public void start(final StartContext context) throws StartException {
        // todo suspendController
        //noinspection unchecked
        List<Map.Entry<ProtocolSocketBinding, Registry<String, List<ClientMapping>>>> clientMappingsRegistries = this.clientMappingsRegistries.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.clientMappingsRegistries.size());
        for (Map.Entry<Value<ProtocolSocketBinding>, Value<Registry>> entry : this.clientMappingsRegistries) {
            clientMappingsRegistries.add(new SimpleImmutableEntry<>(entry.getKey().getValue(), entry.getValue().getValue()));
        }
        value = new AssociationImpl(deploymentRepositoryInjector.getValue(), clientMappingsRegistries);

        String ourNodeName = serverEnvironmentServiceInjector.getValue().getNodeName();

        // track deployments at an association level for local dispatchers to utilize
        moduleAvailabilityListener = value.registerModuleAvailabilityListener(new ModuleAvailabilityListener() {

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
                        for (Map.Entry<ProtocolSocketBinding, Registry<String, List<ClientMapping>>> entry : clientMappingsRegistries) {
                            Group group = entry.getValue().getGroup();
                            if (!group.isSingleton()) {
                                b.addAttribute(EJBClientContext.FILTER_ATTR_CLUSTER, AttributeValue.fromString(group.getName()));
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
    }

    @Override
    public void stop(final StopContext context) {
        value.close();
        value = null;
        moduleAvailabilityListener.close();
        moduleAvailabilityListener = null;
        mutableDiscoveryProvider.setDiscoveryProvider(DiscoveryProvider.EMPTY);
        synchronized (serviceLock) {
            cachedServiceURL = null;
            ourModules.clear();
        }
    }

    @Override
    public AssociationService getValue() {
        return this;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentServiceInjector() {
        return serverEnvironmentServiceInjector;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjector() {
        return deploymentRepositoryInjector;
    }

    public Map.Entry<Injector<ProtocolSocketBinding>, Injector<Registry>> addConnectorInjectors(String connectorName) {
        InjectedValue<ProtocolSocketBinding> info = new InjectedValue<>();
        InjectedValue<Registry> registry = new InjectedValue<>();
        this.clientMappingsRegistries.add(new AbstractMap.SimpleImmutableEntry<>(info, registry));
        return new AbstractMap.SimpleImmutableEntry<>(info, registry);
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
        this.value.setExecutor(executor);
    }

    void sendTopologyUpdateIfLastNodeToLeave() {
        this.value.sendTopologyUpdateIfLastNodeToLeave();
    }
}

