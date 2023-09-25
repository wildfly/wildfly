/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEDefaultResourceJndiNames;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * The {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which adds the EE subsystem default bindings configuration to EE module descriptions.
 *
 * @author Eduardo Martins
 */
public class DefaultBindingsConfigurationProcessor implements DeploymentUnitProcessor {

    private volatile String contextService;
    private volatile String dataSource;
    private volatile String jmsConnectionFactory;
    private volatile String managedExecutorService;
    private volatile String managedScheduledExecutorService;
    private volatile String managedThreadFactory;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // store subsystem config in module description
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(eeModuleDescription != null) {
            // set names only if these are not set yet
            final EEDefaultResourceJndiNames defaultResourceJndiNames = eeModuleDescription.getDefaultResourceJndiNames();
            if(defaultResourceJndiNames.getContextService() == null) {
                defaultResourceJndiNames.setContextService(contextService);
            }
            if(defaultResourceJndiNames.getDataSource() == null) {
                defaultResourceJndiNames.setDataSource(dataSource);
            }
            if(defaultResourceJndiNames.getJmsConnectionFactory() == null) {
                defaultResourceJndiNames.setJmsConnectionFactory(jmsConnectionFactory);
            }
            if(defaultResourceJndiNames.getManagedExecutorService() == null) {
                defaultResourceJndiNames.setManagedExecutorService(managedExecutorService);
            }
            if(defaultResourceJndiNames.getManagedScheduledExecutorService() == null) {
                defaultResourceJndiNames.setManagedScheduledExecutorService(managedScheduledExecutorService);
            }
            if(defaultResourceJndiNames.getManagedThreadFactory() == null) {
                defaultResourceJndiNames.setManagedThreadFactory(managedThreadFactory);
            }
        }
    }

    public void setContextService(String contextService) {
        this.contextService = contextService;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void setJmsConnectionFactory(String jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    public void setManagedExecutorService(String managedExecutorService) {
        this.managedExecutorService = managedExecutorService;
    }

    public void setManagedScheduledExecutorService(String managedScheduledExecutorService) {
        this.managedScheduledExecutorService = managedScheduledExecutorService;
    }

    public void setManagedThreadFactory(String managedThreadFactory) {
        this.managedThreadFactory = managedThreadFactory;
    }

}