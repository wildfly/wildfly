/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * Adaptor of DeploymentAspect to DeploymentUnitProcessor
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @since 17-Jan-2011
 */
public final class AspectDeploymentProcessor extends TCCLDeploymentProcessor implements DeploymentUnitProcessor {

    private static Logger log = Logger.getLogger(AspectDeploymentProcessor.class);

    private Class<? extends DeploymentAspect> clazz;
    private String aspectClass;
    private DeploymentAspect aspect;

    public AspectDeploymentProcessor(Class<? extends DeploymentAspect> aspectClass) {
        this.clazz = aspectClass;
    }

    public AspectDeploymentProcessor(String aspectClass) {
        this.aspectClass = aspectClass;
    }

    public AspectDeploymentProcessor(DeploymentAspect aspect) {
        this.aspect = aspect;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void internalDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (ASHelper.isWebServiceDeployment(unit)) {
            if (aspect == null) {
                try {
                    if (clazz == null) {
                        clazz = (Class<? extends DeploymentAspect>) SecurityActions.getContextClassLoader().loadClass(aspectClass);
                    }
                    aspect = clazz.newInstance();
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException(e);
                }
            }
            log.debug(this.aspect + " start: " + unit.getName());
            final Deployment dep = ASHelper.getRequiredAttachment(unit, WSAttachmentKeys.DEPLOYMENT_KEY);
            aspect.start(dep);
        }
    }

    @Override
    public void internalUndeploy(org.jboss.as.server.deployment.DeploymentUnit context) {
        if (ASHelper.isWebServiceDeployment(context)) {
            log.debug(this.aspect + " stop: " + context.getName());
            final Deployment dep = ASHelper.getRequiredAttachment(context, WSAttachmentKeys.DEPLOYMENT_KEY);
            aspect.stop(dep);
        }
    }
}
