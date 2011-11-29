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

package org.jboss.as.connector;

import org.jboss.msc.service.ServiceName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

/**
 * A ConnectorServices.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ConnectorServices {

    private static Map<String, Set<ServiceName>> resourceAdapterServiceNames = new HashMap<String, Set<ServiceName>>();
    private static Map<String, Set<Integer>> resourceAdapterIdentifiers = new HashMap<String, Set<Integer>>();

    private static Map<String, Set<ServiceName>> deploymentServiceNames = new HashMap<String, Set<ServiceName>>();
    private static Map<String, Set<Integer>> deploymentIdentifiers = new HashMap<String, Set<Integer>>();

    /**
     * A map whose key corresponds to a ra name and whose value is a identifier with which the RA
     * is registered in the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     */
    private static Map<String, String> resourceAdapterRepositoryIdentifiers = new HashMap<String, String>();

    public static final ServiceName CONNECTOR_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "config");

    public static final ServiceName BEAN_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "bean_validation", "config");

    public static final ServiceName ARCHIVE_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "archive_validation", "config");

    public static final ServiceName BOOTSTRAP_CONTEXT_SERVICE = ServiceName.JBOSS.append("connector",
            "bootstrapcontext");

    public static final ServiceName TRANSACTION_INTEGRATION_SERVICE = ServiceName.JBOSS.append("connector",
            "transactionintegration");

    public static final ServiceName WORKMANAGER_SERVICE = ServiceName.JBOSS.append("connector", "workmanager");

    public static final ServiceName RESOURCE_ADAPTER_SERVICE_PREFIX = ServiceName.JBOSS.append("ra");

    public static final ServiceName RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX = RESOURCE_ADAPTER_SERVICE_PREFIX.append("deployment");

    public static final ServiceName RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX = RESOURCE_ADAPTER_SERVICE_PREFIX.append("deployer");

    public static final ServiceName RESOURCE_ADAPTER_REGISTRY_SERVICE = ServiceName.JBOSS.append("raregistry");

    public static final ServiceName RESOURCE_ADAPTER_ACTIVATOR_SERVICE = ServiceName.JBOSS.append("raactivator");

    /**
     * MDR service name *
     */
    public static final ServiceName IRONJACAMAR_MDR = ServiceName.JBOSS.append("ironjacamar", "mdr");

    public static final ServiceName RA_REPOSISTORY_SERVICE = ServiceName.JBOSS.append("rarepository");

    public static final ServiceName MANAGEMENT_REPOSISTORY_SERVICE = ServiceName.JBOSS.append("management_repository");

    public static final ServiceName RESOURCEADAPTERS_SERVICE = ServiceName.JBOSS.append("resourceadapters");

    public static final ServiceName RA_SERVICE = ServiceName.JBOSS.append("resourceadapters", "ra");

    public static final ServiceName DATASOURCES_SERVICE = ServiceName.JBOSS.append("datasources");

    public static final ServiceName JDBC_DRIVER_REGISTRY_SERVICE = ServiceName.JBOSS.append("jdbc-driver", "registry");

    public static final ServiceName CCM_SERVICE = ServiceName.JBOSS.append("cached-connection-manager");

    private ConnectorServices() {
    }

    /**
     * convenient method to check notNull of value
     *
     * @param <T>   type of the value
     * @param value the value
     * @return the value or throw an {@link IllegalStateException} if value is null (a.k.a. service not started)
     */
    public static <T> T notNull(T value) {
        if (value == null)
            throw MESSAGES.serviceNotStarted();
        return value;
    }

    // DEPLOYMENTS

    private static Integer getDeploymentIdentifier(String raName) {
        Set<Integer> entries = deploymentIdentifiers.get(raName);

        if (entries == null) {
            Integer identifier = Integer.valueOf(1);

            entries = new HashSet<Integer>();
            deploymentIdentifiers.put(raName, entries);

            entries.add(identifier);
            return identifier;
        }

        Integer identifier = Integer.valueOf(1);
        for (; ; ) {
            if (!entries.contains(identifier)) {
                entries.add(identifier);
                return identifier;
            }

            identifier = Integer.valueOf(identifier.intValue() + 1);
        }
    }

    public static synchronized ServiceName registerDeployment(String raName) {
        if (raName == null)
            throw MESSAGES.undefinedVar("RaName");

        Integer identifier = getDeploymentIdentifier(raName);
        ServiceName serviceName = RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX.append(raName + "_" + identifier);

        Set<ServiceName> entries = deploymentServiceNames.get(raName);

        if (entries == null) {
            entries = new HashSet<ServiceName>(1);
            deploymentServiceNames.put(raName, entries);
        }

        if (entries.contains(serviceName)) {
            deploymentIdentifiers.get(raName).remove(identifier);
            throw MESSAGES.serviceAlreadyRegistered(serviceName.getCanonicalName());
        }

        entries.add(serviceName);

        return serviceName;
    }

    public static synchronized void unregisterDeployment(String raName, ServiceName serviceName) {
        if (raName == null)
            throw MESSAGES.undefinedVar("RaName");

        if (serviceName == null)
            throw MESSAGES.undefinedVar("ServiceName");

        Set<ServiceName> entries = deploymentServiceNames.get(raName);

        if (entries != null) {
            if (!entries.contains(serviceName))
                throw MESSAGES.serviceIsntRegistered(serviceName.getCanonicalName());

            Integer identifier = Integer.valueOf(serviceName.getSimpleName().substring(serviceName.getSimpleName().lastIndexOf("_") + 1));
            deploymentIdentifiers.get(raName).remove(identifier);

            entries.remove(serviceName);

            if (entries.size() == 0) {
                deploymentServiceNames.remove(raName);
                deploymentIdentifiers.remove(raName);
            }
        }
    }

    // RESOURCE ADAPTERS

    private static Integer getResourceAdapterIdentifier(String raName) {
        Set<Integer> entries = resourceAdapterIdentifiers.get(raName);

        if (entries == null) {
            Integer identifier = Integer.valueOf(1);

            entries = new HashSet<Integer>();
            resourceAdapterIdentifiers.put(raName, entries);

            entries.add(identifier);
            return identifier;
        }

        Integer identifier = Integer.valueOf(1);
        for (; ; ) {
            if (!entries.contains(identifier)) {
                entries.add(identifier);
                return identifier;
            }

            identifier = Integer.valueOf(identifier.intValue() + 1);
        }
    }

    public static synchronized ServiceName registerResourceAdapter(String raName) {
        if (raName == null)
            throw MESSAGES.undefinedVar("RaName");

        Integer identifier = getResourceAdapterIdentifier(raName);
        ServiceName serviceName = RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName + "_" + identifier);

        Set<ServiceName> entries = resourceAdapterServiceNames.get(raName);

        if (entries == null) {
            entries = new HashSet<ServiceName>(1);
            resourceAdapterServiceNames.put(raName, entries);
        }

        if (entries.contains(serviceName)) {
            resourceAdapterIdentifiers.get(raName).remove(identifier);
            throw MESSAGES.serviceAlreadyRegistered(serviceName.getCanonicalName());
        }

        entries.add(serviceName);

        return serviceName;
    }

    public static synchronized void unregisterResourceAdapter(String raName, ServiceName serviceName) {
        if (raName == null)
            throw MESSAGES.undefinedVar("RaName");

        if (serviceName == null)
            throw MESSAGES.undefinedVar("ServiceName");

        Set<ServiceName> entries = resourceAdapterServiceNames.get(raName);

        if (entries != null) {
            if (!entries.contains(serviceName))
                throw MESSAGES.serviceIsntRegistered(serviceName.getCanonicalName());

            Integer identifier = Integer.valueOf(serviceName.getSimpleName().substring(serviceName.getSimpleName().lastIndexOf("_") + 1));
            resourceAdapterIdentifiers.get(raName).remove(identifier);

            entries.remove(serviceName);

            if (entries.size() == 0) {
                resourceAdapterServiceNames.remove(raName);
                resourceAdapterIdentifiers.remove(raName);
            }
        }
    }

    public static synchronized Set<ServiceName> getResourceAdapterServiceNames(String raName) {
        return resourceAdapterServiceNames.get(raName);
    }

    /**
     * Returns the identifier with which the resource adapter named <code>raName</code> is registered
     * in the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}. Returns null, if there's no
     * registration for a resource adapter named <code>raName</code>
     *
     * @param raName The resource adapter name
     * @return
     */
    public static String getRegisteredResourceAdapterIdentifier(final String raName) {
        synchronized (resourceAdapterRepositoryIdentifiers) {
            return resourceAdapterRepositoryIdentifiers.get(raName);
        }
    }

    /**
     * Makes a note of the resource adapter identifier with which a resource adapter named <code>raName</code>
     * is registered in the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}.
     * <p/>
     * Subsequent calls to {@link #getRegisteredResourceAdapterIdentifier(String)} with the passed <code>raName</code>
     * return the <code>raIdentifier</code>
     *
     * @param raName       The resource adapter name
     * @param raIdentifier The resource adapter identifier
     */
    public static void registerResourceAdapterIdentifier(final String raName, final String raIdentifier) {
        synchronized (resourceAdapterRepositoryIdentifiers) {
            resourceAdapterRepositoryIdentifiers.put(raName, raIdentifier);
        }
    }

    /**
     * Clears the mapping between the <code>raName</code> and the resource adapter identifier, with which the resource
     * adapter is registered with the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     * <p/>
     * Subsequent calls to {@link #getRegisteredResourceAdapterIdentifier(String)} with the passed <code>raName</code>
     * return null
     *
     * @param raName       The resource adapter name
     */
    public static void unregisterResourceAdapterIdentifier(final String raName) {
        synchronized (resourceAdapterRepositoryIdentifiers) {
            resourceAdapterRepositoryIdentifiers.remove(raName);
        }
    }
}
