/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.el.cache.FactoryFinderCache;
import org.jboss.modules.Module;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Cleans up references to EE structures in the deployment unit
 *
 * @author Stuart Douglas
 */
public class EECleanUpProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        deploymentUnit.removeAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        deploymentUnit.removeAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        deploymentUnit.removeAttachment(Attachments.EE_MODULE_CONFIGURATION);
        deploymentUnit.removeAttachment(Attachments.EE_MODULE_DESCRIPTION);
        deploymentUnit.removeAttachment(Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        final Module module = context.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        ServiceName deploymentService = context.getServiceName();
        ServiceController<?> controller = context.getServiceRegistry().getRequiredService(deploymentService);
        //WFLY-9666 we do this cleanup at the end of undeploy
        //if we do it now any code in the undeploy sequence that attempts to use EL can cause it to be re-added
        controller.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> serviceController, LifecycleEvent lifecycleEvent) {
                if(lifecycleEvent == LifecycleEvent.DOWN) {
                    clearFactoryFinderCache(module);
                    controller.removeListener(this);
                }
            }
        });
    }

    private void clearFactoryFinderCache(Module module) {
        try {
            FactoryFinderCache.clearClassLoader(module.getClassLoader());
        } catch (NoClassDefFoundError e) {
            // FactoryFinderCache is only available when the JBoss fork of Jakarta EL is used
            EeLogger.ROOT_LOGGER.debugf("Cannot load FactoryFinderCache class -- cache clearing will not be performed");
        }
    }
}
