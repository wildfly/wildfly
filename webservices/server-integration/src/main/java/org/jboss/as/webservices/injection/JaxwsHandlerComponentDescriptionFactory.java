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
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.deployers.WSComponentDescriptionFactory;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
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
    protected void processAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        final WSEndpointHandlersMapping mapping = getRequiredAttachment(unit, WS_ENDPOINT_HANDLERS_MAPPING_KEY);
        final String endpointClassName = classInfo.name().toString();

        if (isJaxwsEjb(classInfo)) {
            for (final EJBEndpoint ejbEndpoint : getJaxwsEjbs(unit)) {
                if (endpointClassName.equals(ejbEndpoint.getClassName())) {
                    for (final String handlerClassName : mapping.getHandlers(endpointClassName)) {
                        final String ejbEndpointName = ejbEndpoint.getName();
                        final String handlerName = ejbEndpointName + "-" + handlerClassName;
                        final ComponentDescription jaxwsHandlerDescription = createComponentDescription(unit, handlerName, handlerClassName, ejbEndpointName);
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

    private static void propagateNamingContext(final ComponentDescription jaxwsHandlerDescription, final EJBEndpoint ejbEndpoint) {
        final ServiceName ejbContextServiceName = ejbEndpoint.getContextServiceName();
        final DeploymentDescriptorEnvironment ejbEnv = ejbEndpoint.getDeploymentDescriptorEnvironment();
        // configure JAXWS EJB3 handler to be able to see EJB3 environment
        jaxwsHandlerDescription.setContextServiceName(ejbContextServiceName);
        jaxwsHandlerDescription.setDeploymentDescriptorEnvironment(ejbEnv);
        jaxwsHandlerDescription.addDependency(ejbContextServiceName, ServiceBuilder.DependencyType.REQUIRED);
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
