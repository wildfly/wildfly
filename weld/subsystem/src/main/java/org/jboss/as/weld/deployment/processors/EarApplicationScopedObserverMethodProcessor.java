/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
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
