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

package org.jboss.as.connector.util;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.subsystems.resourceadapters.ModifiableResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.msc.service.ServiceName;

/**
 * ConnectorServices contains some utility methods used internally and constants for all connector's subsystems service names.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class ConnectorServices {

    /**
     * A map whose key corresponds to a ra name and whose value is an identifier with which the RA is registered in the
     * {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     */
    private static Map<String, String> resourceAdapterRepositoryIdentifiers = new HashMap<String, String>();

    public static final ServiceName CONNECTOR_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "config");

    public static final ServiceName BEAN_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "bean_validation",
            "config");

    public static final ServiceName TRACER_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "tracer",
                "config");

    public static final ServiceName ARCHIVE_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector",
            "archive_validation", "config");

    public static final ServiceName BOOTSTRAP_CONTEXT_SERVICE = ServiceName.JBOSS.append("connector", "bootstrapcontext");

    public static final ServiceName TRANSACTION_INTEGRATION_SERVICE = ServiceName.JBOSS.append("connector",
            "transactionintegration");

    public static final ServiceName WORKMANAGER_SERVICE = ServiceName.JBOSS.append("connector", "workmanager");

    public static final ServiceName WORKMANAGER_STATS_SERVICE = WORKMANAGER_SERVICE.append("statistics");

    public static final ServiceName DISTRIBUTED_WORKMANAGER_STATS_SERVICE = WORKMANAGER_SERVICE.append("distributed-statistics");

    public static final ServiceName RESOURCE_ADAPTER_SERVICE_PREFIX = ServiceName.JBOSS.append("ra");

    public static final String STATISTICS_SUFFIX = "STATISTICS";


    public static final ServiceName RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX = RESOURCE_ADAPTER_SERVICE_PREFIX
            .append("deployment");

    public static final ServiceName RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX = RESOURCE_ADAPTER_SERVICE_PREFIX
            .append("deployer");

    public static final ServiceName RESOURCE_ADAPTER_REGISTRY_SERVICE = ServiceName.JBOSS.append("raregistry");

    public static final ServiceName RESOURCE_ADAPTER_ACTIVATOR_SERVICE = ServiceName.JBOSS.append("raactivator");

    public static final ServiceName INACTIVE_RESOURCE_ADAPTER_SERVICE = ServiceName.JBOSS.append("rainactive");

    /**
     * MDR service name *
     */
    public static final ServiceName IRONJACAMAR_MDR = ServiceName.JBOSS.append("ironjacamar", "mdr");

    public static final ServiceName IRONJACAMAR_RESOURCE = ServiceName.JBOSS.append("ironjacamar", "resource");

    public static final ServiceName RA_REPOSITORY_SERVICE = ServiceName.JBOSS.append("rarepository");

    public static final ServiceName NON_JTA_DS_RA_REPOSITORY_SERVICE = ServiceName.JBOSS.append("non_jta_ds_rarepository");

    public static final ServiceName MANAGEMENT_REPOSITORY_SERVICE = ServiceName.JBOSS.append("management_repository");

    public static final ServiceName RESOURCEADAPTERS_SERVICE = ServiceName.JBOSS.append("resourceadapters");

    public static final ServiceName RESOURCEADAPTERS_SUBSYSTEM_SERVICE = ServiceName.JBOSS.append("resourceadapters-subsystem");

    public static final ServiceName RA_SERVICE = ServiceName.JBOSS.append("resourceadapters", "ra");

    public static final ServiceName DATASOURCES_SERVICE = ServiceName.JBOSS.append("datasources");

    public static final ServiceName JDBC_DRIVER_REGISTRY_SERVICE = ServiceName.JBOSS.append("jdbc-driver", "registry");

    public static final ServiceName CCM_SERVICE = ServiceName.JBOSS.append("cached-connection-manager");

    public static final ServiceName NON_TX_CCM_SERVICE = ServiceName.JBOSS.append("non-tx-cached-connection-manager");

    public static final ServiceName IDLE_REMOVER_SERVICE = ServiceName.JBOSS.append("ironjacamar", "idle-remover");

    public static final ServiceName CONNECTION_VALIDATOR_SERVICE = ServiceName.JBOSS.append("ironjacamar",
            "connection-validator");

    /**
     * convenient method to check notNull of value
     *
     * @param <T> type of the value
     * @param value the value
     * @return the value or throw an {@link IllegalStateException} if value is null (a.k.a. service not started)
     */
    public static <T> T notNull(T value) {
        if (value == null)
            throw ConnectorLogger.ROOT_LOGGER.serviceNotStarted();
        return value;
    }

    // resource-adapter DMR resource

    public static synchronized ServiceName getDeploymentServiceName(final String raName, final Activation raxml) {
        if (raName == null)
            throw ConnectorLogger.ROOT_LOGGER.undefinedVar("RaName");

        ServiceName serviceName = null;
        ModifiableResourceAdapter ra = (ModifiableResourceAdapter) raxml;
        if (ra != null && ra.getId() != null) {
            serviceName = getDeploymentServiceName(raName,ra.getId());
        } else {
            serviceName = getDeploymentServiceName(raName,(String)null);
        }
        ROOT_LOGGER.tracef("ConnectorServices: getDeploymentServiceName(%s,%s) -> %s", raName, raxml,serviceName);
        return serviceName;
    }

    public static synchronized ServiceName getDeploymentServiceName(String raName, String raId) {
        if (raName == null)
            throw ConnectorLogger.ROOT_LOGGER.undefinedVar("RaName");

        // ServiceName entry = deploymentServiceNames.get(raName);
        ServiceName serviceName = null;

        if (raId == null || raId.equals(raName)) {
            serviceName = RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX.append(raName);
        } else {
            serviceName = RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX.append(raName + "_" + raId);
        }

        ROOT_LOGGER.tracef("ConnectorServices: getDeploymentServiceName(%s,%s) -> %s", raName, raId,serviceName);
        return serviceName;
    }

    public static synchronized ServiceName getDeploymentServiceName(final String raName) {
        if (raName == null)
            throw ConnectorLogger.ROOT_LOGGER.undefinedVar("RaName");

        final ServiceName serviceName = RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX.append(raName);
        ROOT_LOGGER.tracef("ConnectorServices: getDeploymentServiceName(%s) -> %s", raName, serviceName);
        return serviceName;
    }

    public static synchronized ServiceName getResourceAdapterServiceName(final String id) {
        if (id == null || id.trim().isEmpty()) {
            throw ConnectorLogger.ROOT_LOGGER.undefinedVar("id");
        }

        ServiceName serviceName = RESOURCE_ADAPTER_SERVICE_PREFIX.append(stripDotRarSuffix(id));

        ROOT_LOGGER.tracef("ConnectorServices: getResourceAdapterServiceName(%s) -> %s", id, serviceName);
        return serviceName;
    }

    private static String stripDotRarSuffix(final String raName) {
       if (raName == null) {
          return null;
       }
       // See RaDeploymentParsingProcessor
       if (raName.endsWith(".rar"))       {
         return raName.substring(0, raName.indexOf(".rar"));
       }
       return raName;
    }

    /**
     * Returns the identifier with which the resource adapter named <code>raName</code> is registered in the
     * {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}. Returns null, if there's no registration for a resource
     * adapter named <code>raName</code>
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
     * Makes a note of the resource adapter identifier with which a resource adapter named <code>raName</code> is registered in
     * the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}.
     * <p/>
     * Subsequent calls to {@link #getRegisteredResourceAdapterIdentifier(String)} with the passed <code>raName</code> return
     * the <code>raIdentifier</code>
     *
     * @param raName The resource adapter name
     * @param raIdentifier The resource adapter identifier
     */
    public static void registerResourceAdapterIdentifier(final String raName, final String raIdentifier) {
        synchronized (resourceAdapterRepositoryIdentifiers) {
            resourceAdapterRepositoryIdentifiers.put(raName, raIdentifier);
        }
    }

    /**
     * Clears the mapping between the <code>raName</code> and the resource adapter identifier, with which the resource adapter
     * is registered with the {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     * <p/>
     * Subsequent calls to {@link #getRegisteredResourceAdapterIdentifier(String)} with the passed <code>raName</code> return
     * null
     *
     * @param raName The resource adapter name
     */
    public static void unregisterResourceAdapterIdentifier(final String raName) {
        synchronized (resourceAdapterRepositoryIdentifiers) {
            resourceAdapterRepositoryIdentifiers.remove(raName);
        }
    }
}
