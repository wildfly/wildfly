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

import static org.jboss.as.webservices.util.ASHelper.getEndpointClassName;
import static org.jboss.as.webservices.util.ASHelper.getEndpointName;
import static org.jboss.as.webservices.util.ASHelper.getJBossWebMetaData;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsService;
import static org.jboss.as.webservices.util.DotNames.SINGLETON_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.STATELESS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.deployers.WSComponentDescriptionFactory;
import org.jboss.as.webservices.metadata.DeploymentJaxws;
import org.jboss.as.webservices.metadata.DeploymentJaxwsImpl;
import org.jboss.as.webservices.metadata.EndpointJaxwsPojoImpl;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.as.webservices.util.WebMetaDataHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JaxwsEndpointComponentDescriptionFactory extends WSComponentDescriptionFactory {

    private static final Logger logger = Logger.getLogger(JaxwsEndpointComponentDescriptionFactory.class);

    public JaxwsEndpointComponentDescriptionFactory() {
        super(WEB_SERVICE_ANNOTATION, WEB_SERVICE_PROVIDER_ANNOTATION);
    }

    @Override
    protected void processWSAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex, final EEModuleDescription moduleDescription) throws DeploymentUnitProcessingException {
        final DeploymentJaxws wsDeployment = getWSDeployment(unit);
        if (isJaxwsEjb(classInfo)) {
            // Don't create component description for EJB3 endpoints.
            // There's already one created by EJB3 subsystem.
        } else {
            final String beanClassName = classInfo.name().toString();
            if (isJmsEndpoint(unit, beanClassName)) return; // do not process JMS endpoints
            final ServiceName unitServiceName = unit.getServiceName();
            // TODO: refactor to convenient method that will return DD defined servlets matching class name
            final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
            final JBossServletsMetaData ddServlets = WebMetaDataHelper.getServlets(jbossWebMD);
            boolean found = false;
            for (final ServletMetaData servletMD : ddServlets) {
                if (beanClassName.equals(getEndpointClassName(servletMD))) {
                    // creating component description for POJO endpoint
                    found = true;
                    final String endpointName = getEndpointName(servletMD);
                    ComponentDescription jaxwsEndpointDescription = moduleDescription.getComponentByName(endpointName);
                    if (jaxwsEndpointDescription == null) {
                        jaxwsEndpointDescription = new WSComponentDescription(endpointName, beanClassName, moduleDescription, unitServiceName);
                        moduleDescription.addComponent(jaxwsEndpointDescription);
                    }
                    // registering dependency on WS endpoint service
                    final ServiceName serviceName = EndpointService.getServiceName(unit, endpointName);
                    jaxwsEndpointDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                    // register POJO endpoint
                    final String urlPattern = getURLPattern(endpointName, unit);
                    wsDeployment.addEndpoint(new EndpointJaxwsPojoImpl(endpointName, beanClassName, urlPattern));
                }
            }
            if (!found) {
                // JSR 109, version 1.3 final spec, section 5.3.2.1 javax.jws.WebService annotation
                final ComponentDescription jaxwsEndpointDescription = new WSComponentDescription(beanClassName, beanClassName, moduleDescription, unitServiceName);
                moduleDescription.addComponent(jaxwsEndpointDescription);
                // registering dependency on WS endpoint service
                final ServiceName serviceName = EndpointService.getServiceName(unit, beanClassName);
                jaxwsEndpointDescription.addDependency(serviceName, ServiceBuilder.DependencyType.REQUIRED);
                // register POJO endpoint
                final String urlPattern = getUrlPattern(classInfo);
                wsDeployment.addEndpoint(new EndpointJaxwsPojoImpl(beanClassName, urlPattern));
            }
        }
    }

    private static DeploymentJaxws getWSDeployment(final DeploymentUnit unit) {
        DeploymentJaxws wsDeployment = unit.getAttachment(JAXWS_ENDPOINTS_KEY);
        if (wsDeployment == null) {
            wsDeployment = new DeploymentJaxwsImpl();
            unit.putAttachment(JAXWS_ENDPOINTS_KEY, wsDeployment);
        }
        return wsDeployment;
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
        // TODO: Provide utility methods in ASHelper ...
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

    private static String getUrlPattern(final ClassInfo clazz) {
        final AnnotationInstance webServiceAnnotation = getWebServiceAnnotation(clazz);
        final String serviceName = getStringAttribute(webServiceAnnotation, "serviceName");
        return "/" + (serviceName != null ? serviceName : clazz.name().local());
    }

    // TODO: Provide utility methods in ASHelper ...
    private static String getStringAttribute(final AnnotationInstance annotation, final String attributeName) {
        final AnnotationValue attributeValue = annotation.value(attributeName);
        if (attributeValue != null) {
            final String trimmedAttributeValue = attributeValue.asString().trim();
            return "".equals(trimmedAttributeValue) ? null : trimmedAttributeValue;
        }
        return null;
    }

    private static String getURLPattern(final String servletName, final DeploymentUnit unit) {
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        for (final ServletMappingMetaData servletMappingMD : jbossWebMD.getServletMappings()) {
            if (servletName.equals(servletMappingMD.getServletName())) {
                return servletMappingMD.getUrlPatterns().get(0);
            }
        }
        throw new IllegalStateException();
    }

    private static AnnotationInstance getWebServiceAnnotation(final ClassInfo clazz) {
        final List<AnnotationInstance> webServiceAnnotations = clazz.annotations().get(WEB_SERVICE_ANNOTATION);
        if (webServiceAnnotations.size() > 0) {
            return webServiceAnnotations.get(0);
        }
        final List<AnnotationInstance> webServiceProviderAnnotations = clazz.annotations().get(WEB_SERVICE_PROVIDER_ANNOTATION);
        if (webServiceProviderAnnotations.size() > 0) {
            return webServiceProviderAnnotations.get(0);
        }
        throw new IllegalStateException();
    }

    private static boolean isJmsEndpoint(final DeploymentUnit unit, final String endpointClass) {
        final JMSEndpointsMetaData jmsEndpointsMD = getRequiredAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        for (final JMSEndpointMetaData endpoint : jmsEndpointsMD.getEndpointsMetaData()) {
            if (endpointClass.equals(endpoint.getImplementor())) return true;
        }
        return false;
    }

}
