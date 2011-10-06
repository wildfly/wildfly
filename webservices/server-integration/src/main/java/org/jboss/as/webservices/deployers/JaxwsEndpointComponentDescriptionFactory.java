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

import static org.jboss.as.webservices.util.DotNames.SINGLETON_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.STATELESS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsService;
import static org.jboss.as.webservices.util.ASHelper.isServlet;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.component.WSComponentDescription;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JaxwsEndpointComponentDescriptionFactory extends WSComponentDescriptionFactory {

    private static final Logger logger = Logger.getLogger(JaxwsEndpointComponentDescriptionFactory.class);

    public JaxwsEndpointComponentDescriptionFactory() {
        super(WEB_SERVICE_ANNOTATION, WEB_SERVICE_PROVIDER_ANNOTATION);
    }

    @Override
    protected void processWSAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex, final EEModuleDescription moduleDescription, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        if (isJaxwsEjb(classInfo)) {
            // Don't create component description for EJB3 endpoints.
            // There's already one created by EJB3 subsystem.
        } else {
            final String beanClassName = classInfo.name().toString();
            final ServiceName unitServiceName = unit.getServiceName();
            // TODO: refactor to convenient method that will return DD defined servlets matching class name
            List<ServletMetaData> ddServlets = ASHelper.getJaxwsServlets(unit);
            boolean found = false;
            for (final ServletMetaData servletMD : ddServlets) {
                if (beanClassName.equals(ASHelper.getEndpointClassName(servletMD))) {
                    found = true;
                    final String endpointName = ASHelper.getEndpointName(servletMD);
                    final ComponentDescription jaxwsEndpointDescription = new WSComponentDescription(endpointName, beanClassName, moduleDescription, unitServiceName, applicationClasses);
                    moduleDescription.addComponent(jaxwsEndpointDescription);
                    // TODO: register dependency on WS endpoint service
                    final ServiceName serviceName = EndpointService.getServiceName(unit, endpointName);
                    jaxwsEndpointDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                }
            }
            if (!found) {
                // TODO: JBWS-3276
                /*
                final ComponentDescription jaxwsEndpointDescription = new WSComponentDescription(beanClassName, beanClassName, moduleDescription, unitServiceName, applicationClasses);
                moduleDescription.addComponent(jaxwsEndpointDescription);
                // TODO: register dependency on WS endpoint service
                final ServiceName serviceName = EndpointService.getServiceName(unit, beanClassName);
                jaxwsEndpointDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                */
            }
        }
    }

    // TODO: @WebService can be specified on interfaces too. Create component description only for real endpoints!!!
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
            logger.warn("[JAXWS 2.2 spec, section 7.7] The @WebService and @WebServiceProvider annotations are mutually exclusive - "
                    + className + " won't be considered as a webservice endpoint, since it doesn't meet that requirement");
            return false;
        }
        if (Modifier.isFinal(flags)) {
            final String className = clazz.name().toString();
            logger.warn("Neither @WebService nor @WebServiceProvider annotated endpoint can be final class - "
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
