/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import static org.wildfly.microprofile.reactive.messaging.common.ReactiveMessagingAttachments.IS_REACTIVE_MESSAGING_DEPLOYMENT;
import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class ReactiveMessagingSslConfigProcessor implements DeploymentUnitProcessor {

    private static final String RM_KAFKA_GLOBAL_PROPERTY_PREFIX = "mp.messaging.connector.smallrye-kafka.";
    private static final String RM_INCOMING_PROPERTY_PREFIX = "mp.messaging.outgoing.";
    private static final String RM_OUTGOING_PROPERTY_PREFIX = "mp.messaging.incoming.";
    static final String SSL_CONTEXT_PROPERTY_SUFFIX = "wildfly.elytron.ssl.context";

    private static final String SSL_ENGINE_FACTORY_CLASS = "ssl.engine.factory.class";

    private static final AttachmentKey<Object> DEPLOYMENT_ATTACHMENT_KEY = AttachmentKey.create(Object.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isReactiveMessagingDeployment(deploymentUnit)) {
            return;
        }
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        Config config = ConfigProviderResolver.instance().getConfig(module.getClassLoader());

        Map<String, String> addedProperties = new HashMap<>();

        Set<ServiceName> mscDependencies = new HashSet<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.endsWith(SSL_CONTEXT_PROPERTY_SUFFIX)) {
                String propertyValue = config.getValue(propertyName, String.class);
                String prefix = null;
                if (propertyName.equals(RM_KAFKA_GLOBAL_PROPERTY_PREFIX + SSL_CONTEXT_PROPERTY_SUFFIX)) {
                    prefix = RM_KAFKA_GLOBAL_PROPERTY_PREFIX;
                } else if (propertyName.startsWith(RM_INCOMING_PROPERTY_PREFIX) ||
                        propertyName.startsWith(RM_OUTGOING_PROPERTY_PREFIX)) {
                    prefix = propertyName.substring(0, propertyName.indexOf(SSL_CONTEXT_PROPERTY_SUFFIX));
                }
                if (prefix != null) {
                    LOGGER.foundPropertyUsingElytronClientSSLContext(propertyName, propertyValue);
                    if (!ElytronSSLContextRegistry.isSSLContextInstalled(propertyValue)) {
                        throw LOGGER.noElytronClientSSLContext(propertyValue);
                    }
                    mscDependencies.add(ElytronSSLContextRegistry.getSSLContextName(propertyValue));
                    addedProperties.put(prefix + SSL_ENGINE_FACTORY_CLASS, WildFlyKafkaSSLEngineFactory.class.getName());
                }
            }
        }
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

    private boolean isReactiveMessagingDeployment(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.hasAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT)) {
            Boolean isRm = deploymentUnit.getAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT);
            return isRm;
        }
        return false;
    }
}
