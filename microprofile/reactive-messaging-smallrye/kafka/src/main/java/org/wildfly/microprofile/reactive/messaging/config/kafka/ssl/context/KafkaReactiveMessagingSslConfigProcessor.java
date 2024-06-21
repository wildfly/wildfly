/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.wildfly.microprofile.reactive.messaging.common.security.BaseReactiveMessagingSslConfigProcessor;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class KafkaReactiveMessagingSslConfigProcessor extends BaseReactiveMessagingSslConfigProcessor {

    static final String SSL_CONTEXT_PROPERTY_SUFFIX = "wildfly.elytron.ssl.context";
    private static final String SSL_ENGINE_FACTORY_CLASS = "ssl.engine.factory.class";

    private static final String CONNECTOR_NAME = "smallrye-kafka";

    // Disable the Windows and Mac Snappy check
    private static final boolean DISABLE_SNAPPY_ON_WINDOWS_AND_MAC = false;
    private static final String OS_NAME = "os.name";
    public static final String COMPRESSION_TYPE_PROPERTY_SUFFIX = ".compression.type";
    public static final String SNAPPY_COMPRESSION = "snappy";

    private final boolean runningOnWindowsOrMac;

    KafkaReactiveMessagingSslConfigProcessor() {
        super(CONNECTOR_NAME);
        String os = WildFlySecurityManager.getPropertyPrivileged("os.name", "x").toLowerCase(Locale.ENGLISH);
        runningOnWindowsOrMac = os.startsWith("windows") || os.startsWith("mac os");
    }

    @Override
    protected SecurityDeploymentContext createSecurityDeploymentContext() {
        return new KafkaSecurityDeploymentContext();
    }

    @Override
    protected boolean isExtraConfigValueCheck() {
        return DISABLE_SNAPPY_ON_WINDOWS_AND_MAC && runningOnWindowsOrMac;
    }

    @Override
    protected void extraConfigValueCheck(String key, String value) throws DeploymentUnitProcessingException {
        if (key.endsWith(COMPRESSION_TYPE_PROPERTY_SUFFIX) && value.trim().equals(SNAPPY_COMPRESSION)) {
            throw LOGGER.snappyCompressionNotSupportedOnWindows(key);
        }
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
