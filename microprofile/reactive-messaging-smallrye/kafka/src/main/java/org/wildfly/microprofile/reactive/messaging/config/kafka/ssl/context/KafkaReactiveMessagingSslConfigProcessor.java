/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
            addedProperties.put(prefix + SSL_ENGINE_FACTORY_CLASS, WildFlyKafkaSSLEngineFactory.class.getName());
        }

        public Map<String, String> complete(DeploymentPhaseContext phaseContext) {
            return addedProperties;
        }
    }
}
