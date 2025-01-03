/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.injection.deployment.DefaultJMSConnectionFactoryBinding.COMP_DEFAULT_JMS_CONNECTION_FACTORY;

import jakarta.jms.ConnectionFactory;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor responsible for adding an EEResourceReferenceProcessor, which defaults @resource ConnectionFactory injection to the default Jakarta Messaging Connection Factory.
 *
 * @author Eduardo Martins
 */
public class DefaultJMSConnectionFactoryResourceReferenceProcessor implements DeploymentUnitProcessor {

    private static final JMSConnectionFactoryResourceReferenceProcessor RESOURCE_REFERENCE_PROCESSOR = new JMSConnectionFactoryResourceReferenceProcessor();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry eeResourceReferenceProcessorRegistry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
            if (eeResourceReferenceProcessorRegistry != null) {
                final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
                if (eeModuleDescription != null && eeModuleDescription.getDefaultResourceJndiNames().getJmsConnectionFactory() != null) {
                    eeResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(RESOURCE_REFERENCE_PROCESSOR);
                }
            }
        }
    }

    private static class JMSConnectionFactoryResourceReferenceProcessor implements EEResourceReferenceProcessor {

        private static final String TYPE = ConnectionFactory.class.getName();
        private static final InjectionSource INJECTION_SOURCE = new LookupInjectionSource(COMP_DEFAULT_JMS_CONNECTION_FACTORY);

        @Override
        public String getResourceReferenceType() {
            return TYPE;
        }

        @Override
        public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
            return INJECTION_SOURCE;
        }
    }

}
