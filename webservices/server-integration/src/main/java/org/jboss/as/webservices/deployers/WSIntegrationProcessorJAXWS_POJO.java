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

import static org.jboss.as.webservices.util.ASHelper.getEndpointClassName;
import static org.jboss.as.webservices.util.ASHelper.getEndpointName;
import static org.jboss.as.webservices.util.ASHelper.getJBossWebMetaData;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsDeployment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;
import static org.jboss.as.webservices.util.WebMetaDataHelper.getServlets;

import java.util.List;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.injection.WSComponentDescription;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class WSIntegrationProcessorJAXWS_POJO extends AbstractIntegrationProcessorJAXWS {

    public WSIntegrationProcessorJAXWS_POJO() {
        super(WEB_SERVICE_ANNOTATION, WEB_SERVICE_PROVIDER_ANNOTATION);
    }

    @Override
    protected void processAnnotation(final DeploymentUnit unit, final ClassInfo classInfo, final AnnotationInstance wsAnnotation, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        if (isEjb3(classInfo)) {
            // Don't create component description for EJB3 endpoints.
            // There's already one created by EJB3 subsystem.
            return;
        }
        if (isJmsEndpoint(unit, classInfo)) {
            // Do not create component description for JMS endpoints.
            return;
        }

        final String endpointClassName = classInfo.name().toString();
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        if (jbossWebMD != null) {
            final JAXWSDeployment jaxwsDeployment = getJaxwsDeployment(unit);
            boolean found = false;
            for (final ServletMetaData servletMD : getServlets(jbossWebMD)) {
                if (endpointClassName.equals(getEndpointClassName(servletMD))) {
                    found = true;
                    // creating component description for POJO endpoint
                    final String endpointName = getEndpointName(servletMD);
                    final ComponentDescription pojoComponent = createComponentDescription(unit, endpointName, endpointClassName, endpointName);
                    final ServiceName pojoViewName = registerView(pojoComponent, endpointClassName);
                    // register POJO endpoint
                    final String urlPattern = getUrlPattern(endpointName, unit);
                    jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointName, endpointClassName, pojoViewName, urlPattern));
                }
            }
            if (!found) {
                // JSR 109, version 1.3 final spec, section 5.3.2.1 javax.jws.WebService annotation
                final ComponentDescription pojoComponent = createComponentDescription(unit, endpointClassName, endpointClassName, endpointClassName);
                final ServiceName pojoViewName = registerView(pojoComponent, endpointClassName);
                // register POJO endpoint
                final String urlPattern = getUrlPattern(classInfo);
                jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointClassName, pojoViewName, urlPattern));
            }
        }
    }

    private static String getUrlPattern(final ClassInfo clazz) {
        final AnnotationInstance webServiceAnnotation = getWebServiceAnnotation(clazz);
        final String serviceName = getStringAttribute(webServiceAnnotation, "serviceName");
        return "/" + (serviceName != null ? serviceName : clazz.name().local());
    }

    private static String getUrlPattern(final String servletName, final DeploymentUnit unit) {
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        for (final ServletMappingMetaData servletMappingMD : jbossWebMD.getServletMappings()) {
            if (servletName.equals(servletMappingMD.getServletName())) {
                return servletMappingMD.getUrlPatterns().get(0);
            }
        }
        throw new IllegalStateException();
    }

    private static boolean isJmsEndpoint(final DeploymentUnit unit, final ClassInfo classInfo) {
        final String endpointClassName = classInfo.name().toString();
        final JMSEndpointsMetaData jmsEndpointsMD = getRequiredAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        for (final JMSEndpointMetaData endpoint : jmsEndpointsMD.getEndpointsMetaData()) {
            if (endpointClassName.equals(endpoint.getImplementor())) return true;
        }
        return false;
    }

    private static String getStringAttribute(final AnnotationInstance annotation, final String attributeName) {
        final AnnotationValue attributeValue = annotation.value(attributeName);
        if (attributeValue != null) {
            final String trimmedAttributeValue = attributeValue.asString().trim();
            return "".equals(trimmedAttributeValue) ? null : trimmedAttributeValue;
        }
        return null;
    }

    private static AnnotationInstance getWebServiceAnnotation(final ClassInfo clazz) {
        final List<AnnotationInstance> webServiceAnnotations = clazz.annotations().get(WEB_SERVICE_ANNOTATION);
        if (webServiceAnnotations != null && webServiceAnnotations.size() > 0) {
            return webServiceAnnotations.get(0);
        }
        final List<AnnotationInstance> webServiceProviderAnnotations = clazz.annotations().get(WEB_SERVICE_PROVIDER_ANNOTATION);
        if (webServiceProviderAnnotations != null && webServiceProviderAnnotations.size() > 0) {
            return webServiceProviderAnnotations.get(0);
        }
        throw new IllegalStateException();
    }

}
