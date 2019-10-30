/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.messaging.activemq.deployment;

import javax.jms.ConnectionFactory;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor responsible for adding an EEResourceReferenceProcessor, which defaults @resource ConnectionFactory injection to the default JMS Connection Factory.
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
            if(eeResourceReferenceProcessorRegistry != null) {
                eeResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(RESOURCE_REFERENCE_PROCESSOR);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private static class JMSConnectionFactoryResourceReferenceProcessor implements EEResourceReferenceProcessor {

        private static final String TYPE = ConnectionFactory.class.getName();
        private static final InjectionSource INJECTION_SOURCE = new LookupInjectionSource(DefaultJMSConnectionFactoryBindingProcessor.COMP_DEFAULT_JMS_CONNECTION_FACTORY);

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
