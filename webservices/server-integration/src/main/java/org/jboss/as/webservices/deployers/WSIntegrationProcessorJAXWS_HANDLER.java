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

import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.webservices.injection.WSEndpointHandlersMapping;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.jandex.ClassInfo;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class WSIntegrationProcessorJAXWS_HANDLER extends AbstractIntegrationProcessorJAXWS {

    public WSIntegrationProcessorJAXWS_HANDLER() {
    }

    @Override
    protected void processAnnotation(final DeploymentUnit unit, final EEModuleDescription moduleDescription) throws DeploymentUnitProcessingException {
        final WSEndpointHandlersMapping mapping = getOptionalAttachment(unit, WS_ENDPOINT_HANDLERS_MAPPING_KEY);
        if (mapping == null)
            return;
        for (EEModuleClassDescription classDescription : moduleDescription.getClassDescriptions()) {
            ClassInfo classInfo = null;
            ClassAnnotationInformation<WebService, WebServiceAnnotationInfo> annotationInfo = classDescription
                    .getAnnotationInformation(WebService.class);
            if (annotationInfo != null) {
                classInfo = (ClassInfo) annotationInfo.getClassLevelAnnotations().get(0).getTarget();
            }
            final ClassAnnotationInformation<WebServiceProvider, WebServiceProviderAnnotationInfo> providreInfo = classDescription
                        .getAnnotationInformation(WebServiceProvider.class);
            if (providreInfo != null) {
                classInfo = (ClassInfo) providreInfo.getClassLevelAnnotations().get(0).getTarget();
            }
            if (classInfo != null && mapping.getHandlers(classInfo.name().toString()) != null) {
                final String endpointClassName = classInfo.name().toString();
                if (isEjb3(classInfo)) {
                    for (final EJBEndpoint ejbEndpoint : getJaxwsEjbs(unit)) {
                        if (endpointClassName.equals(ejbEndpoint.getClassName())) {
                            for (final String handlerClassName : mapping.getHandlers(endpointClassName)) {
                                final String ejbEndpointName = ejbEndpoint.getName();
                                final String handlerName = ejbEndpointName + "-" + handlerClassName;
                                final ComponentDescription jaxwsHandlerDescription = createComponentDescription(unit,
                                        handlerName, handlerClassName, ejbEndpointName);
                                propagateNamingContext(jaxwsHandlerDescription, ejbEndpoint);
                            }
                        }
                    }
                } else {
                    for (final POJOEndpoint pojoEndpoint : getJaxwsPojos(unit)) {
                        if (endpointClassName.equals(pojoEndpoint.getClassName())) {
                            for (final String handlerClassName : mapping.getHandlers(endpointClassName)) {
                                final String pojoEndpointName = pojoEndpoint.getName();
                                final String handlerName = pojoEndpointName + "-" + handlerClassName;
                                createComponentDescription(unit, handlerName, handlerClassName, pojoEndpointName);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void propagateNamingContext(final ComponentDescription jaxwsHandlerDescription, final EJBEndpoint ejbEndpoint) {
        final ServiceName ejbContextServiceName = ejbEndpoint.getContextServiceName();
        final DeploymentDescriptorEnvironment ejbEnv = ejbEndpoint.getDeploymentDescriptorEnvironment();
        // configure JAXWS EJB3 handler to be able to see EJB3 environment
        jaxwsHandlerDescription.setContextServiceName(ejbContextServiceName);
        jaxwsHandlerDescription.setDeploymentDescriptorEnvironment(ejbEnv);
        jaxwsHandlerDescription.addDependency(ejbContextServiceName, ServiceBuilder.DependencyType.REQUIRED);
    }

}
