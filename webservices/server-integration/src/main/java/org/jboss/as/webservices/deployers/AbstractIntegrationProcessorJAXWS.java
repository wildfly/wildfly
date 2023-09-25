/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.deployers;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.DotNames.SINGLETON_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.STATELESS_ANNOTATION;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.injection.WSComponentDescription;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.invocation.AccessCheckingInterceptor;
import org.jboss.jandex.ClassInfo;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public abstract class AbstractIntegrationProcessorJAXWS implements DeploymentUnitProcessor {

    protected AbstractIntegrationProcessorJAXWS() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, unit)) {
            return;
        }
        final CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }
        final EEModuleDescription eeModuleDescription = unit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        processAnnotation(unit, eeModuleDescription);
    }

    protected abstract void processAnnotation(final DeploymentUnit unit, final EEModuleDescription eeModuleDescription) throws DeploymentUnitProcessingException;

    static ComponentDescription createComponentDescription(final DeploymentUnit unit, final String componentName, final String componentClassName, final String dependsOnEndpointClassName) {
        final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
        // JBoss WEB processors may install fake components for WS endpoints - removing them forcibly
        moduleDescription.removeComponent(componentName, componentClassName);
        // register WS component
        ComponentDescription componentDescription = new WSComponentDescription(componentName, componentClassName, moduleDescription, unit.getServiceName());
        moduleDescription.addComponent(componentDescription);
        // register WS dependency
        final ServiceName endpointServiceName = EndpointService.getServiceName(unit, dependsOnEndpointClassName);
        componentDescription.addDependency(endpointServiceName);

        return componentDescription;
    }

    static ServiceName registerView(final ComponentDescription componentDescription, final String componentClassName) {
        final ViewDescription pojoView = new ViewDescription(componentDescription, componentClassName);
        componentDescription.getViews().add(pojoView);
        pojoView.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(AccessCheckingInterceptor.getFactory(), InterceptorOrder.View.CHECKING_INTERCEPTOR);
                // add WS POJO component instance associating interceptor
                configuration.addViewInterceptor(WSComponentInstanceAssociationInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
        return pojoView.getServiceName();
    }

    static boolean isEjb3(final ClassInfo clazz) {
        return clazz.annotationsMap().containsKey(STATELESS_ANNOTATION) || clazz.annotationsMap().containsKey(SINGLETON_ANNOTATION);
    }

}
