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

import java.util.Collections;
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

    private static Map<String, ServiceName> deploymentServiceNames = new HashMap<String, ServiceName>();
    private static Map<String, Set<Integer>> deploymentIdentifiers = new HashMap<String, Set<Integer>>();
    private static Map<String, Set<Integer>> resourceIdentifiers = new HashMap<String, Set<Integer>>();


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

    public static final ServiceName INACTIVE_RESOURCE_ADAPTER_SERVICE = ServiceName.JBOSS.append("rainactive");

    /**
     * MDR service name *
     */
    public static final ServiceName IRONJACAMAR_MDR = ServiceName.JBOSS.append("ironjacamar", "mdr");

    public static final ServiceName RA_REPOSITORY_SERVICE = ServiceName.JBOSS.append("rarepository");

    public static final ServiceName MANAGEMENT_REPOSITORY_SERVICE = ServiceName.JBOSS.append("management_repository");

    public static final ServiceName RESOURCEADAPTERS_SERVICE = ServiceName.JBOSS.append("resourceadapters");

    public static final ServiceName RA_SERVICE = ServiceName.JBOSS.append("resourceadapters", "ra");

    public static final ServiceName DATASOURCES_SERVICE = ServiceName.JBOSS.append("datasources");

    public static final ServiceName JDBC_DRIVER_REGISTRY_SERVICE = ServiceName.JBOSS.append("jdbc-driver", "registry");

    public static final ServiceName CCM_SERVICE = ServiceName.JBOSS.append("cached-connection-manager");

    public static final ServiceName IDLE_REMOVER_SERVICE = ServiceName.JBOSS.append("ironjacamar", "idle-remover");

    public static final ServiceName CONNECTION_VALIDATOR_SERVICE = ServiceName.JBOSS.append("ironjacamar", "connection-validator");

    public static final String RA_SERVICE_NAME_SEPARATOR = "->";

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

    //resource-adapter DMR resource

    public static Integer getResourceIdentifier(String raName) {
        Set<Integer> entries = resourceIdentifiers.get(raName);

        if (entries == null) {
            Integer identifier = Integer.valueOf(0);

            entries = new HashSet<Integer>();
            resourceIdentifiers.put(raName, entries);

            entries.add(identifier);
            return identifier;
        }

        Integer identifier = Integer.valueOf(0);
        for (; ; ) {
            if (!entries.contains(identifier)) {
                entries.add(identifier);
                return identifier;
            }

            identifier = Integer.valueOf(identifier.intValue() + 1);
        }
    }

    public static void unregisterResourceIdentifier(String raName, Integer identifier) {

            Set<Integer> entries = resourceIdentifiers.get(raName);

            if (entries != null) {

                if (entries.contains(identifier)) {
                    entries.remove(identifier);
                }
                if (entries.isEmpty()) {
                    unregisterResourceIdentifiers(raName);
                }

            }
        }

    public static synchronized void unregisterResourceIdentifiers(String raName) {
            if (raName == null)
                throw MESSAGES.undefinedVar("RaName");


            resourceIdentifiers.remove(raName);
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

        ServiceName entry = deploymentServiceNames.get(raName);

        /*if (entry != null ) {
            deploymentIdentifiers.get(raName).remove(identifier);
            throw MESSAGES.serviceAlreadyRegistered(entry.getCanonicalName());
        } */

        deploymentServiceNames.put(raName, serviceName);

        return serviceName;
    }

    public static synchronized ServiceName getDeploymentServiceName(String raName) {
            if (raName == null)
                throw MESSAGES.undefinedVar("RaName");

            ServiceName entry = deploymentServiceNames.get(raName);



            return entry;
        }




    public static synchronized void unregisterDeployment(String raName, ServiceName serviceName) {
        if (raName == null)
            throw MESSAGES.undefinedVar("RaName");

        if (serviceName == null)
            throw MESSAGES.undefinedVar("ServiceName");

        ServiceName entry = deploymentServiceNames.get(raName);

        if (entry != null) {
            if (!entry.equals(serviceName))
                throw MESSAGES.serviceIsntRegistered(serviceName.getCanonicalName());

            Integer identifier = Integer.valueOf(serviceName.getSimpleName().substring(serviceName.getSimpleName().lastIndexOf("_") + 1));
            deploymentIdentifiers.get(raName).remove(identifier);

            deploymentServiceNames.remove(raName);
            if (deploymentIdentifiers.get(raName).size() == 0) {
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
        if (raName == null || raName.trim().isEmpty()) {
            throw MESSAGES.undefinedVar("RaName");
        }
        // There can be multiple activations for the same ra name. For example, multiple resource
        // adapter elements (with different configs) in the resource adapter subsystem, all pointing to the same ra archive.
        // The ServiceName for the first activation of a RA with raName *will always* be of the form:
        // RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName).
        // Any subsequent activations for the same raName will have a numeric identifier appended to the service name
        // as follows:
        // RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName).append(RA_SERVICE_NAME_SEPARATOR).append(<numeric-id>)

        // Check if this is the first activation for the RA name
        Set<ServiceName> serviceNamesForRAActivation = resourceAdapterServiceNames.get(raName);
        if (serviceNamesForRAActivation == null) {
            serviceNamesForRAActivation = new HashSet<ServiceName>();
            resourceAdapterServiceNames.put(raName, serviceNamesForRAActivation);
        }
        final ServiceName serviceName;
        if (serviceNamesForRAActivation.isEmpty()) {
            // this is the first activation, so the service name *won't* have a numeric identifier
            serviceName = RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName);
        } else {
            // there was already an activation for the raName. So generate a service name with a numeric identifier
            final Integer nextId = getResourceAdapterIdentifier(raName);
            serviceName = RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName).append(RA_SERVICE_NAME_SEPARATOR).append(nextId.toString());
        }
        serviceNamesForRAActivation.add(serviceName);
        return serviceName;
    }

    public static synchronized void unregisterResourceAdapter(String raName, ServiceName serviceName) {
        if (raName == null || raName.trim().isEmpty()) {
            throw MESSAGES.undefinedVar("RaName");
        }

        if (serviceName == null) {
            throw MESSAGES.undefinedVar("ServiceName");
        }

        final Set<ServiceName> registeredServiceNames = resourceAdapterServiceNames.get(raName);
        if (registeredServiceNames == null || registeredServiceNames.isEmpty() || !registeredServiceNames.contains(serviceName)) {
            throw MESSAGES.serviceIsntRegistered(serviceName.getCanonicalName());
        }
        // remove the service from the registered service names for this RA
        registeredServiceNames.remove(serviceName);

        // check if the ServiceName contains any numeric identifiers, or if it's just the first activation of a RA.
        // if the service name has a numeric part, then we need to get that numeric part and unregister that number
        // from the map which hold the in-use numeric ids.
        // @see registerResourceAdapter method for more details on how the service names are generated
        if (!serviceName.equals(RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName))) {
            final ServiceName baseServiceName = RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName).append(RA_SERVICE_NAME_SEPARATOR);
            // if the service name doesn't start with the RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName).append(RA_SERVICE_NAME_SEPARATOR)
            // format, then it isn't a RA service
            if (!baseServiceName.isParentOf(serviceName)) {
                throw MESSAGES.notResourceAdapterService(serviceName);
            }
            // get the service name parts
            final String[] parts = serviceName.toArray();
            // get the numerical id which will be the last part of the service name
            final String lastPart = parts[parts.length - 1];
            final Integer numericId;
            try {
                numericId = Integer.parseInt(lastPart);
            } catch (NumberFormatException nfe) {
                throw MESSAGES.notResourceAdapterService(serviceName);
            }
            // remove from the numeric id registration map
            resourceAdapterIdentifiers.get(raName).remove(numericId);
        }
    }

    /**
     * Returns the {@link ServiceName}s of the activations of the resource adapter named <code>raName</code>.
     * The returned service names can be used by other services to add dependency on the resource adapter activations.
     *
     * @param raName The resource adapter name
     * @return
     */
    public static synchronized Set<ServiceName> getResourceAdapterServiceNames(final String raName) {
        if (raName == null || raName.trim().isEmpty()) {
            throw MESSAGES.stringParamCannotBeNullOrEmpty("resource adapter name");
        }
        // For now, we just return a single service name as the dependency service name, even if there
        // might be multiple activations for the same RA. If the dependent service needs the service name of
        // a specific activation of the RA, then a different method which accepts specific properties for that
        // RA activation, will have to be used
        return Collections.singleton(RESOURCE_ADAPTER_SERVICE_PREFIX.append(raName));
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
