/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
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
                    FactoryFinderCache.clearClassLoader(module.getClassLoader());
                    controller.removeListener(this);
                }
            }
        });
    }
}
