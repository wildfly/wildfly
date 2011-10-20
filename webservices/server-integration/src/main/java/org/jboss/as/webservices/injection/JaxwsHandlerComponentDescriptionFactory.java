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

package org.jboss.as.webservices.injection;

import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsService;
import static org.jboss.as.webservices.util.DotNames.HANDLER_CHAIN_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.SINGLETON_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.STATELESS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY;

import java.lang.reflect.Modifier;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.deployers.WSComponentDescriptionFactory;
import org.jboss.as.webservices.metadata.EndpointJaxwsEjb;
import org.jboss.as.webservices.metadata.EndpointJaxwsPojo;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class JaxwsHandlerComponentDescriptionFactory extends WSComponentDescriptionFactory {

    private static final Logger logger = Logger.getLogger(JaxwsHandlerComponentDescriptionFactory.class);

    public JaxwsHandlerComponentDescriptionFactory() {
        super(HANDLER_CHAIN_ANNOTATION);
    }

    @Override
    protected void processWSAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex, final EEModuleDescription moduleDescription) throws DeploymentUnitProcessingException {
        final ServiceName unitServiceName = unit.getServiceName();
        final WSEndpointHandlersMapping mapping = getRequiredAttachment(unit, WS_ENDPOINT_HANDLERS_MAPPING_KEY);
        final String endpointClassName = classInfo.name().toString();

        if (isJaxwsEjb(classInfo)) {
            for (final EndpointJaxwsEjb container : getJaxwsEjbs(unit)) {
                if (endpointClassName.equals(container.getClassName())) {
                    for (final String handlerClassName : mapping.getHandlers(endpointClassName)) {
                        final String ejbName = container.getName();
                        final String handlerID = ejbName + "-" + handlerClassName;
                        final ServiceName ejbContextServiceName = container.getContextServiceName();
                        final DeploymentDescriptorEnvironment ejbEnv = container.getDeploymentDescriptorEnvironment();
                        if (moduleDescription.getComponentByName(handlerID) == null) {
                            // register JAXWS handler component for EJB3 endpoint
                            final ComponentDescription jaxwsHandlerDescription = new WSComponentDescription(handlerID, handlerClassName, moduleDescription, unitServiceName);
                            moduleDescription.addComponent(jaxwsHandlerDescription);
                            // registering dependency on WS endpoint service
                            final ServiceName serviceName = EndpointService.getServiceName(unit, ejbName);
                            jaxwsHandlerDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                            // configure JAXWS EJB3 handler to be able to see EJB3 environment
                            jaxwsHandlerDescription.setContextServiceName(ejbContextServiceName);
                            jaxwsHandlerDescription.setDeploymentDescriptorEnvironment(ejbEnv);
                            jaxwsHandlerDescription.addDependency(ejbContextServiceName, ServiceBuilder.DependencyType.REQUIRED);
                        }
                    }
                }
            }
        } else {
            for (final EndpointJaxwsPojo pojoEndpoint : getJaxwsPojos(unit)) {
                if (endpointClassName.equals(pojoEndpoint.getClassName())) {
                    for (final String handlerClassName : mapping.getHandlers(endpointClassName)) {
                        final String pojoName = pojoEndpoint.getName();
                        final String handlerID = pojoName + "-" + handlerClassName;
                        if (moduleDescription.getComponentByName(handlerID) == null) {
                            // register JAXWS handler component for POJO endpoint
                            final ComponentDescription jaxwsHandlerDescription = new WSComponentDescription(handlerID, handlerClassName, moduleDescription, unitServiceName);
                            moduleDescription.addComponent(jaxwsHandlerDescription);
                            // registering dependency on WS endpoint service
                            final ServiceName serviceName = EndpointService.getServiceName(unit, pojoName);
                            jaxwsHandlerDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean matches(final ClassInfo clazz, final CompositeIndex index) {
        // assert WS endpoint class - TODO: refactor common code
        final short flags = clazz.flags();
        if (Modifier.isInterface(flags)) return false;
        if (Modifier.isAbstract(flags)) return false;
        if (!Modifier.isPublic(flags)) return false;
        if (isJaxwsService(clazz, index)) return false;
        // validate annotations
        final boolean hasWebServiceAnnotation = clazz.annotations().containsKey(WEB_SERVICE_ANNOTATION);
        final boolean hasWebServiceProviderAnnotation = clazz.annotations().containsKey(WEB_SERVICE_PROVIDER_ANNOTATION);
        if (hasWebServiceAnnotation && hasWebServiceProviderAnnotation) {
            final String className = clazz.name().toString();
            logger.warn("@HandlerChain can be specified only on classes annotated either with @WebService or @WebServiceProvider annotation - "
                    + className + " won't be considered as a webservice endpoint, since it doesn't meet that requirement");
            return false;
        }
        return true;
    }

    private static boolean isJaxwsEjb(final ClassInfo clazz) { // TODO: refactor to ASHelper
        final boolean isStateless = clazz.annotations().containsKey(STATELESS_ANNOTATION);
        final boolean isSingleton = clazz.annotations().containsKey(SINGLETON_ANNOTATION);
        return isStateless || isSingleton;
    }

}
