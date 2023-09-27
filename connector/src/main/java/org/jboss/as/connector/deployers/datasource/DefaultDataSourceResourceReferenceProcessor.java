/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.datasource;

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

import javax.sql.DataSource;

/**
 * Processor responsible for adding an EEResourceReferenceProcessor, which defaults @resource datasource injection to java:comp/DefaultDataSource.
 *
 * @author Eduardo Martins
 */
public class DefaultDataSourceResourceReferenceProcessor implements DeploymentUnitProcessor {

    private static final DatasourceResourceReferenceProcessor RESOURCE_REFERENCE_PROCESSOR = new DatasourceResourceReferenceProcessor();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry eeResourceReferenceProcessorRegistry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
            if(eeResourceReferenceProcessorRegistry != null) {
                final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
                if (eeModuleDescription != null && eeModuleDescription.getDefaultResourceJndiNames().getDataSource() != null) {
                    eeResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(RESOURCE_REFERENCE_PROCESSOR);
                }
            }
        }
    }

    private static class DatasourceResourceReferenceProcessor implements EEResourceReferenceProcessor {

        private static final String TYPE = DataSource.class.getName();
        private static final InjectionSource INJECTION_SOURCE = new LookupInjectionSource(DefaultDataSourceBindingProcessor.COMP_DEFAULT_DATASOURCE_JNDI_NAME);

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
