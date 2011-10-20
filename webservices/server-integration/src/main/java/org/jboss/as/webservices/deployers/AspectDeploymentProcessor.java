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
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * Adaptor of DeploymentAspect to DeploymentUnitProcessor
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @since 17-Jan-2011
 */
public final class AspectDeploymentProcessor implements DeploymentUnitProcessor {

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
        this.clazz = aspect.getClass();
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (ASHelper.isWebServiceDeployment(unit)) {
            ensureAspectInitialised();
            final Deployment dep = ASHelper.getRequiredAttachment(unit, WSAttachmentKeys.DEPLOYMENT_KEY);
            if (aspect.canHandle(dep)) {
                log.debug(this.aspect + " start: " + unit.getName());
                ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
                try {
                    SecurityActions.setContextClassLoader(aspect.getLoader());
                    dep.addAttachment(ServiceTarget.class, phaseContext.getServiceTarget());
                    aspect.start(dep);
                    dep.removeAttachment(ServiceTarget.class);
                } finally {
                    SecurityActions.setContextClassLoader(origClassLoader);
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        if (ASHelper.isWebServiceDeployment(context)) {
            final Deployment dep = ASHelper.getRequiredAttachment(context, WSAttachmentKeys.DEPLOYMENT_KEY);
            if (aspect.canHandle(dep)) {
                log.debug(this.aspect + " stop: " + context.getName());
                ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
                try {
                    SecurityActions.setContextClassLoader(aspect.getLoader());
                    aspect.stop(dep);
                } finally {
                    SecurityActions.setContextClassLoader(origClassLoader);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureAspectInitialised() throws DeploymentUnitProcessingException {
        if (aspect == null) {
            try {
                if (clazz == null) {
                    clazz = (Class<? extends DeploymentAspect>) ClassLoaderProvider.getDefaultProvider()
                            .getServerIntegrationClassLoader().loadClass(aspectClass);
                }
                aspect = clazz.newInstance();
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
    }
}
