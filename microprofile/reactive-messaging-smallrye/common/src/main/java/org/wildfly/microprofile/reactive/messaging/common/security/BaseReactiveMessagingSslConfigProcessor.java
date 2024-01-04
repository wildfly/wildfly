/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.common.security;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.wildfly.microprofile.reactive.messaging.config.ReactiveMessagingConfigSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.wildfly.microprofile.reactive.messaging.common.ReactiveMessagingAttachments.IS_REACTIVE_MESSAGING_DEPLOYMENT;
import static org.wildfly.microprofile.reactive.messaging.common._private.MicroProfileReactiveMessagingCommonLogger.LOGGER;

public abstract class BaseReactiveMessagingSslConfigProcessor implements DeploymentUnitProcessor {

    private static final String RM_CONNECTOR_GLOBAL_PROPERTY_PREFIX = "mp.messaging.connector.";
    private static final String RM_INCOMING_PROPERTY_PREFIX = "mp.messaging.outgoing.";
    private static final String RM_OUTGOING_PROPERTY_PREFIX = "mp.messaging.incoming.";
    private static final String RM_CONNECTION_CONNECTOR_PROPERTY_SUFFIX = "connector";
    static final String SSL_CONTEXT_PROPERTY_SUFFIX = "wildfly.elytron.ssl.context";

    private static final AttachmentKey<Object> DEPLOYMENT_ATTACHMENT_KEY = AttachmentKey.create(Object.class);

    private final String connectorName;
    private final String globalPropertyPrefix;

    protected BaseReactiveMessagingSslConfigProcessor(String connectorName) {
        globalPropertyPrefix = RM_CONNECTOR_GLOBAL_PROPERTY_PREFIX + connectorName + ".";
        this.connectorName = connectorName;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isReactiveMessagingDeployment(deploymentUnit)) {
            return;
        }
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        Config config = ConfigProviderResolver.instance().getConfig(module.getClassLoader());
        SecurityDeploymentContext sdc = createSecurityDeploymentContext();
        Set<ServiceName> mscDependencies = new HashSet<>();

        for (String propertyName : config.getPropertyNames()) {

            if (propertyName.equals(globalPropertyPrefix + SSL_CONTEXT_PROPERTY_SUFFIX)) {
                // global thingy for this connector
                String propertyValue = config.getValue(propertyName, String.class);
                logAndCheck(mscDependencies, propertyName, propertyValue);
                sdc.setGlobalSslContext(globalPropertyPrefix, propertyValue);
            } else if (propertyName.startsWith(RM_INCOMING_PROPERTY_PREFIX) ||
                    propertyName.startsWith(RM_OUTGOING_PROPERTY_PREFIX)) {
                if (propertyName.endsWith(RM_CONNECTION_CONNECTOR_PROPERTY_SUFFIX)) {
                    if (config.getValue(propertyName, String.class).equals(connectorName)) {

                        String connectorPrefix = propertyName.substring(0, propertyName.indexOf(RM_CONNECTION_CONNECTOR_PROPERTY_SUFFIX));
                        String sslContext =
                                config.getOptionalValue(
                                                connectorPrefix + SSL_CONTEXT_PROPERTY_SUFFIX, String.class)
                                        .orElse(null);
                        if (sslContext != null) {
                            logAndCheck(mscDependencies, propertyName, sslContext);
                        }
                        sdc.setConnectorSslContext(connectorPrefix, sslContext);
                    }
                }
            }
        }

        Map<String, String> addedProperties = sdc.complete(phaseContext);
        if (addedProperties.size() > 0) {
            ReactiveMessagingConfigSource.addProperties(config, addedProperties);
            for (ServiceName svcName : mscDependencies) {
                phaseContext.addDeploymentDependency(svcName, DEPLOYMENT_ATTACHMENT_KEY);
            }
            if (config instanceof SmallRyeConfig) {
                // Refresh the cached property names so our new entries show up elsewhere
                // SmallRye Config 3.4 introduced caching of the property names
                ((SmallRyeConfig) config).getLatestPropertyNames();
            }
        }
    }



    private void logAndCheck(Set<ServiceName> mscDependencies, String propertyName, String sslContextName) {
        if (sslContextName == null) {
            return;
        }
        LOGGER.foundPropertyUsingElytronClientSSLContext(propertyName, sslContextName);
        if (!ElytronSSLContextRegistry.isSSLContextInstalled(sslContextName)) {
            throw LOGGER.noElytronClientSSLContext(sslContextName);
        }
        mscDependencies.add(ElytronSSLContextRegistry.getSSLContextName(sslContextName));
    }

    private boolean isReactiveMessagingDeployment(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.hasAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT)) {
            Boolean isRm = deploymentUnit.getAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT);
            return isRm;
        }
        return false;
    }

    protected abstract SecurityDeploymentContext createSecurityDeploymentContext();

    protected interface SecurityDeploymentContext {

        void setGlobalSslContext(String globalPropertyPrefix, String sslContext);

        void setConnectorSslContext(String connectorPrefix, String sslContext);

        Map<String, String> complete(DeploymentPhaseContext phaseContext);
    }
}
