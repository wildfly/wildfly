/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    private static final Map<String, String> resourceAdapterRepositoryIdentifiers = new HashMap<String, String>();

    /**
     * A map whose key corresponds to a capability name and whose value is the service name for that capability
     */
    private static final Map<String, ServiceName> capabilityServiceNames = new HashMap<String, ServiceName>();

    public static final ServiceName CONNECTOR_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "config");

    public static final ServiceName BEAN_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "bean_validation",
            "config");

    public static final ServiceName TRACER_CONFIG_SERVICE = ServiceName.JBOSS.append("connector", "tracer",
                "config");

    public static final ServiceName ARCHIVE_VALIDATION_CONFIG_SERVICE = ServiceName.JBOSS.append("connector",
            "archive_validation", "config");

    public static final ServiceName BOOTSTRAP_CONTEXT_SERVICE = ServiceName.JBOSS.append("connector", "bootstrapcontext");

    /** @deprecated Use the "org.wildfly.jca.transaction-integration" capability. */
    @Deprecated
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

    public static final ServiceName RESOURCEADAPTERS_CONFIGURED_ADAPTERS_SERVICE = ServiceName.JBOSS.append("resourceadapters-configured-adapters");

    public static final ServiceName RESOURCEADAPTERS_REPORT_DIRECTORY_SERVICE = ServiceName.JBOSS.append("resourceadapters-report-directory");

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

    public static void registerCapabilityServiceName(String capabilityName, ServiceName serviceName) {
        synchronized (capabilityServiceNames) {
            // Minor check against misuse
            ServiceName existing = capabilityServiceNames.get(capabilityName);
            if (existing != null && ! existing.equals(serviceName)) {
                throw new IllegalStateException();
            }

            capabilityServiceNames.put(capabilityName, serviceName);
        }
    }

    /**
     * Name of the capability that ensures a local provider of transactions is present.
     * Once its service is started, calls to the getInstance() methods of ContextTransactionManager,
     * ContextTransactionSynchronizationRegistry and LocalUserTransaction can be made knowing
     * that the global default TM, TSR and UT will be from that provider.
     */
    public static final String LOCAL_TRANSACTION_PROVIDER_CAPABILITY = "org.wildfly.transactions.global-default-local-provider";

    /**
     * The capability name for the transaction TransactionSynchronizationRegistry.
     */
    public static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY = "org.wildfly.transactions.transaction-synchronization-registry";

    /**
     * The capability name for the transaction XAResourceRecoveryRegistry.
     */
    public static final String TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY = "org.wildfly.transactions.xa-resource-recovery-registry";

    public static ServiceName getCachedCapabilityServiceName(String capabilityName) {
        synchronized (capabilityServiceNames) {
            return capabilityServiceNames.get(capabilityName);
        }
    }

    /**
     * The capability name for the JCA transaction integration TransactionIntegration.
     */
    public static final String TRANSACTION_INTEGRATION_CAPABILITY_NAME = "org.wildfly.jca.transaction-integration";
}
