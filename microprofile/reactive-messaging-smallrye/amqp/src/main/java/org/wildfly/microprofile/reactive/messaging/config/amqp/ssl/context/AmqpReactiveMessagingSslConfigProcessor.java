/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.wildfly.microprofile.reactive.messaging.config.amqp.ssl.context;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.common.Assert;
import org.wildfly.microprofile.reactive.messaging.common.security.BaseReactiveMessagingSslConfigProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class AmqpReactiveMessagingSslConfigProcessor extends BaseReactiveMessagingSslConfigProcessor {

    private static final String CONNECTOR_NAME = "smallrye-amqp";
    private static final String CDI_BEAN_PROPERTY_SUFFIX = "client-ssl-context-name";
    private static final String CDI_BEAN_NAME_PREFIX = "cdi-ssl-context-bean-";

    AmqpReactiveMessagingSslConfigProcessor() {
        super(CONNECTOR_NAME);
    }

    @Override
    protected SecurityDeploymentContext createSecurityDeploymentContext() {
        return new AmqpSecurityDeploymentContext();
    }



    private class AmqpSecurityDeploymentContext implements BaseReactiveMessagingSslConfigProcessor.SecurityDeploymentContext {
        private static final String NO_SSL_CONTEXT = "none";
        private final Map<String, String> addedProperties = new HashMap<>();
        private String globalSslContext;
        private Map<String, String> sslContextsPerConnector = new HashMap<>();

        @Override
        public void setGlobalSslContext(String globalPropertyPrefix, String sslContext) {
            this.globalSslContext = sslContext;
        }

        @Override
        public void setConnectorSslContext(String connectorPrefix, String sslContext) {
            sslContextsPerConnector.put(connectorPrefix, sslContext == null ? NO_SSL_CONTEXT : sslContext);
        }

        @Override
        public Map<String, String> complete(DeploymentPhaseContext phaseContext) {
            if (globalSslContext == null && sslContextsPerConnector.isEmpty()) {
                return addedProperties;
            }

            AmqpReactiveMessagingCdiExtension cdiExtension = registerCdiExtension(phaseContext);

            Set<String> sslContextBeans = new HashSet<>();
            for (Map.Entry<String, String> connectorEntry : sslContextsPerConnector.entrySet()) {
                String sslContext = connectorEntry.getValue();
                if (NO_SSL_CONTEXT == sslContext) {
                    if (globalSslContext != null) {
                        addCdiBeanAndProperty(
                                cdiExtension, sslContextBeans, connectorEntry.getKey(), globalSslContext);
                    }
                } else {
                    addCdiBeanAndProperty(
                            cdiExtension, sslContextBeans, connectorEntry.getKey(), connectorEntry.getValue());
                }
            }
            return addedProperties;
        }

        private void addCdiBeanAndProperty(AmqpReactiveMessagingCdiExtension cdiExtension, Set<String> sslContextBeans, String connectorPrefix, String sslContextName) {
            String cdiBeanPropertyName = connectorPrefix + CDI_BEAN_PROPERTY_SUFFIX;
            String cdiBeanName = CDI_BEAN_NAME_PREFIX + sslContextName;
            addedProperties.put(cdiBeanPropertyName, cdiBeanName);

            if (sslContextBeans.add(cdiBeanName)) {
                cdiExtension.addSslContextBean(cdiBeanName, sslContextName);
            }
        }

        private AmqpReactiveMessagingCdiExtension registerCdiExtension(DeploymentPhaseContext phaseContext) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            AmqpReactiveMessagingCdiExtension extension = new AmqpReactiveMessagingCdiExtension();
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            if (support.hasCapability(WELD_CAPABILITY_NAME)) {
                try {
                    final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
                    weldCapability.registerExtensionInstance(extension, deploymentUnit);
                } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
                    Assert.unreachableCode();
                }
            }
            return extension;
        }
    }
}
