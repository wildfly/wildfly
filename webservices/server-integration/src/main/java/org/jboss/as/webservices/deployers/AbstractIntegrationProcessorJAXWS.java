/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsService;
import static org.jboss.as.webservices.util.DotNames.SINGLETON_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.STATELESS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;

import java.lang.reflect.Modifier;
import java.util.List;

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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public abstract class AbstractIntegrationProcessorJAXWS implements DeploymentUnitProcessor {

    private final DotName[] dotNames;

    protected AbstractIntegrationProcessorJAXWS(final DotName... dotNames) {
        this.dotNames = dotNames;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, unit)) {
            return;
        }

        final CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            ROOT_LOGGER.skippingAnnotationProcessing(unit.getName());
        } else {
            for (final DotName dotName : dotNames) {
                final List<AnnotationInstance> wsAnnotations = index.getAnnotations(dotName);
                if (!wsAnnotations.isEmpty()) {
                    for (final AnnotationInstance wsAnnotation : wsAnnotations) {
                        final AnnotationTarget target = wsAnnotation.target();
                        if (target instanceof ClassInfo) {
                            final ClassInfo classInfo = (ClassInfo) target;
                            if (isJaxwsEndpoint(classInfo, index)) {
                                processAnnotation(unit, classInfo, wsAnnotation, index);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit unit) {
        // does nothing
    }

    protected abstract void processAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException;

    private static boolean isJaxwsEndpoint(final ClassInfo clazz, final CompositeIndex index) {
        // assert JAXWS endpoint class flags
        final short flags = clazz.flags();
        if (Modifier.isInterface(flags)) return false;
        if (Modifier.isAbstract(flags)) return false;
        if (!Modifier.isPublic(flags)) return false;
        if (isJaxwsService(clazz, index)) return false;
        final boolean hasWebServiceAnnotation = clazz.annotations().containsKey(WEB_SERVICE_ANNOTATION);
        final boolean hasWebServiceProviderAnnotation = clazz.annotations().containsKey(WEB_SERVICE_PROVIDER_ANNOTATION);
        if (!hasWebServiceAnnotation && !hasWebServiceProviderAnnotation) {
            return false;
        }
        if (hasWebServiceAnnotation && hasWebServiceProviderAnnotation) {
            ROOT_LOGGER.mutuallyExclusiveAnnotations(clazz.name().toString());
            return false;
        }
        if (Modifier.isFinal(flags)) {
            ROOT_LOGGER.finalEndpointClassDetected(clazz.name().toString());
            return false;
        }
        return true;
    }

    static ComponentDescription createComponentDescription(final DeploymentUnit unit, final String componentName, final String componentClassName, final String dependsOnEndpointClassName) {
        final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
        // JBoss WEB processors may install fake components for WS endpoints - removing them forcibly
        moduleDescription.removeComponent(componentName, componentClassName);
        // register WS component
        ComponentDescription componentDescription = new WSComponentDescription(componentName, componentClassName, moduleDescription, unit.getServiceName());
        moduleDescription.addComponent(componentDescription);
        // register WS dependency
        final ServiceName endpointServiceName = EndpointService.getServiceName(unit, dependsOnEndpointClassName);
        componentDescription.addDependency(endpointServiceName, ServiceBuilder.DependencyType.REQUIRED);

        return componentDescription;
    }

    static ServiceName registerView(final ComponentDescription componentDescription, final String componentClassName) {
        final ViewDescription pojoView = new ViewDescription(componentDescription, componentClassName);
        componentDescription.getViews().add(pojoView);
        pojoView.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                // add WS POJO component instance associating interceptor
                configuration.addViewInterceptor(WSComponentInstanceAssociationInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });
        return pojoView.getServiceName();
    }

    static boolean isEjb3(final ClassInfo clazz) {
        final boolean isStateless = clazz.annotations().containsKey(STATELESS_ANNOTATION);
        final boolean isSingleton = clazz.annotations().containsKey(SINGLETON_ANNOTATION);
        return isStateless || isSingleton;
    }

}
