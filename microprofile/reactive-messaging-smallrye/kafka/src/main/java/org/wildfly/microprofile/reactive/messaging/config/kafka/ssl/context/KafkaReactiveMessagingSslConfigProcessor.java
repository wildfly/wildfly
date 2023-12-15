/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.microprofile.reactive.messaging.common.security.BaseReactiveMessagingSslConfigProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class KafkaReactiveMessagingSslConfigProcessor extends BaseReactiveMessagingSslConfigProcessor {

    static final String SSL_CONTEXT_PROPERTY_SUFFIX = "wildfly.elytron.ssl.context";
    private static final String SSL_ENGINE_FACTORY_CLASS = "ssl.engine.factory.class";

    private static final String CONNECTOR_NAME = "smallrye-kafka";

    KafkaReactiveMessagingSslConfigProcessor() {
        super(CONNECTOR_NAME);
    }

    @Override
    protected SecurityDeploymentContext createSecurityDeploymentContext() {
        return new KafkaSecurityDeploymentContext();
    }

    private class KafkaSecurityDeploymentContext implements BaseReactiveMessagingSslConfigProcessor.SecurityDeploymentContext {
        private final Map<String, String> addedProperties = new HashMap<>();

        @Override
        public void setGlobalSslContext(String globalPropertyPrefix, String sslContext) {
            setSslContext(globalPropertyPrefix, sslContext);
        }

        @Override
        public void setConnectorSslContext(String connectorPrefix, String sslContext) {
            setSslContext(connectorPrefix, sslContext);
        }

        private void setSslContext(String prefix, String sslContext) {
            if (sslContext == null) {
                return;
            }
            addedProperties.put(prefix + SSL_ENGINE_FACTORY_CLASS, WildFlyKafkaSSLEngineFactory.class.getName());
        }

        public Map<String, String> complete(DeploymentPhaseContext phaseContext) {
            return addedProperties;
        }
    }
}
