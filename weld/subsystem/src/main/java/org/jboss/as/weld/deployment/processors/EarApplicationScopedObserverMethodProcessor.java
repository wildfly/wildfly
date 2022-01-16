/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.weld.deployment.processors;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.weld.WeldCapability;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.logging.WeldLogger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

/**
 * Processor that registers a CDI portable extension for EAR deployments, which adds support for EE facilities, to CDI app context lifecycle event handlers.
 *
 * @author emmartins
 */
public class EarApplicationScopedObserverMethodProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            // ear deployment only processor
            return;
        }
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get();
            if (api.isPartOfWeldDeployment(deploymentUnit)) {
                api.registerExtensionInstance(new PortableExtension(deploymentUnit), deploymentUnit);
            }
        }
    }

    private static class PortableExtension implements Extension {

        private final DeploymentUnit deploymentUnit;

        private PortableExtension(DeploymentUnit deploymentUnit) {
            this.deploymentUnit = deploymentUnit;
        }

        public <Object, X> void processObserverMethod(@Observes ProcessObserverMethod<Object, X> event) {
            final ObserverMethod<Object> method = event.getObserverMethod();
            for (Annotation a : method.getObservedQualifiers()) {
                // only process @Initialized(ApplicationScoped.class), @BeforeDestroyed(ApplicationScoped.class) and @Destroyed(ApplicationScoped.class)
                if ((a instanceof Initialized && ((Initialized) a).value().equals(ApplicationScoped.class)) || (a instanceof BeforeDestroyed && ((BeforeDestroyed) a).value().equals(ApplicationScoped.class)) || (a instanceof Destroyed && ((Destroyed) a).value().equals(ApplicationScoped.class))) {
                    // if there are setup actions for the bean's class deployable unit wrap the observer method
                    final DeploymentUnit beanDeploymentUnit = getBeanDeploymentUnit(method.getBeanClass().getName());
                    if (beanDeploymentUnit != null) {
                        final List<SetupAction> setupActions = WeldDeploymentProcessor.getSetupActions(beanDeploymentUnit);
                        if (!setupActions.isEmpty()) {
                            event.setObserverMethod(new ObserverMethod<Object>() {
                                @Override
                                public Class<?> getBeanClass() {
                                    return method.getBeanClass();
                                }

                                @Override
                                public Type getObservedType() {
                                    return method.getObservedType();
                                }

                                @Override
                                public Set<Annotation> getObservedQualifiers() {
                                    return method.getObservedQualifiers();
                                }

                                @Override
                                public Reception getReception() {
                                    return method.getReception();
                                }

                                @Override
                                public TransactionPhase getTransactionPhase() {
                                    return method.getTransactionPhase();
                                }

                                @Override
                                public void notify(Object event) {
                                    try {
                                        for (SetupAction action : setupActions) {
                                            action.setup(null);
                                        }
                                        method.notify(event);
                                    } finally {
                                        for (SetupAction action : setupActions) {
                                            try {
                                                action.teardown(null);
                                            } catch (Exception e) {
                                                WeldLogger.DEPLOYMENT_LOGGER.exceptionClearingThreadState(e);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }

        private DeploymentUnit getBeanDeploymentUnit(String beanClass) {
            for (DeploymentUnit subDeploymentUnit : deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS)) {
                final BeanDeploymentModule beanDeploymentModule = subDeploymentUnit.getAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE);
                if (beanDeploymentModule != null) {
                    for (BeanDeploymentArchiveImpl beanDeploymentArchive : beanDeploymentModule.getBeanDeploymentArchives()) {
                        if (beanDeploymentArchive.getBeanClasses().contains(beanClass)) {
                            return subDeploymentUnit;
                        }
                    }
                }
            }
            return null;
        }
    }
}