/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import static org.wildfly.microprofile.reactive.messaging.common.ReactiveMessagingAttachments.IS_REACTIVE_MESSAGING_DEPLOYMENT;
import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

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
                    SSLContext ctx = ElytronSSLContextRegistry.getInstalledSSLContext(propertyValue);
                    if (ctx == null) {
                        throw LOGGER.noElytronClientSSLContext(propertyValue);
                    }
                    mscDependencies.add(ElytronSSLContextRegistry.getSSLContextName(propertyValue));
                    addedProperties.put(prefix + SSL_ENGINE_FACTORY_CLASS, WildFlyKafkaSSLEngineFactory.class.getName());
                }
            }
            if (addedProperties.size() > 0) {
                ReactiveMessagingConfigSource.addProperties(config, addedProperties);
                for (ServiceName svcName : mscDependencies) {
                    phaseContext.addDeploymentDependency(svcName, DEPLOYMENT_ATTACHMENT_KEY);
                }
            }
        }
        if (addedProperties.size() > 0) {
            ReactiveMessagingConfigSource.addProperties(config, addedProperties);
            for (ServiceName svcName : mscDependencies) {
                phaseContext.addDeploymentDependency(svcName, DEPLOYMENT_ATTACHMENT_KEY);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

    private boolean isReactiveMessagingDeployment(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.hasAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT)) {
            Boolean isRm = deploymentUnit.getAttachment(IS_REACTIVE_MESSAGING_DEPLOYMENT);
            return isRm;
        }
        return false;
    }
}
