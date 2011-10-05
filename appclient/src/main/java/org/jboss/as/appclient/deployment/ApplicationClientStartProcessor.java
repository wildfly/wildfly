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
package org.jboss.as.appclient.deployment;

import java.lang.reflect.Method;

import org.jboss.as.appclient.service.ApplicationClientDeploymentService;
import org.jboss.as.appclient.service.ApplicationClientStartService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

/**
 * Processor that starts an application client deployment
 * @author Stuart Douglas
 */
public class ApplicationClientStartProcessor implements DeploymentUnitProcessor {

    private final String[] parameters;

    public ApplicationClientStartProcessor(final String[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevel = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();

        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        Boolean activate = deploymentUnit.getAttachment(AppClientAttachments.START_APP_CLIENT);
        if (activate == null || !activate) {
            return;
        }
        final Class<?> mainClass = deploymentUnit.getAttachment(AppClientAttachments.MAIN_CLASS);
        if (mainClass == null) {
            throw new RuntimeException("Could not start app client " + deploymentUnit.getName() + " as no main class was found");
        }

        ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(mainClass);
        Method method = index.getMethod(void.class, "main", String[].class);
        if (method == null) {
            throw new RuntimeException("Could not start app client " + deploymentUnit.getName() + " as no main main was found on main class " + mainClass);
        }
        final ApplicationClientStartService startService = new ApplicationClientStartService(method, parameters);
        phaseContext.getServiceTarget()
                .addService(deploymentUnit.getServiceName().append(ApplicationClientStartService.SERVICE_NAME), startService)
                .addDependency(ApplicationClientDeploymentService.SERVICE_NAME, ApplicationClientDeploymentService.class,  startService.getApplicationClientDeploymentServiceInjectedValue())
                .install();
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
