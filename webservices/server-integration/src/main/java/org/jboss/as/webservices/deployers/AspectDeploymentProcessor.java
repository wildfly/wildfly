/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Adaptor of DeploymentAspect to DeploymentUnitProcessor
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class AspectDeploymentProcessor implements DeploymentUnitProcessor {

    private Class<? extends DeploymentAspect> clazz;
    private String aspectClass;
    private DeploymentAspect aspect;

    public AspectDeploymentProcessor(final Class<? extends DeploymentAspect> aspectClass) {
        this.clazz = aspectClass;
    }

    public AspectDeploymentProcessor(final String aspectClass) {
        this.aspectClass = aspectClass;
    }

    public AspectDeploymentProcessor(final DeploymentAspect aspect) {
        this.aspect = aspect;
        this.clazz = aspect.getClass();
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (isWebServiceDeployment(unit)) {
            ensureAspectInitialized();
            final Deployment dep = ASHelper.getRequiredAttachment(unit, WSAttachmentKeys.DEPLOYMENT_KEY);
            WSLogger.ROOT_LOGGER.tracef("%s start: %s", aspect, unit.getName());
            ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(aspect.getLoader());
                dep.addAttachment(ServiceTarget.class, phaseContext.getServiceTarget());
                aspect.start(dep);
                dep.removeAttachment(ServiceTarget.class);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit unit) {
        if (isWebServiceDeployment(unit)) {
            final Deployment dep = ASHelper.getRequiredAttachment(unit, WSAttachmentKeys.DEPLOYMENT_KEY);
            WSLogger.ROOT_LOGGER.tracef("%s stop: %s", aspect, unit.getName());
            ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(aspect.getLoader());
                aspect.stop(dep);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureAspectInitialized() throws DeploymentUnitProcessingException {
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

    private static boolean isWebServiceDeployment(final DeploymentUnit unit) {
        return unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY) != null;
    }

}
