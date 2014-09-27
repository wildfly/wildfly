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
import static org.jboss.as.webservices.util.ASHelper.getJBossWebserviceMetaDataPortComponent;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsEndpoint;
import static org.jboss.as.webservices.util.ASHelper.getWebserviceMetadataEJBEndpoint;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;
import static org.jboss.as.webservices.util.WebMetaDataHelper.getServlets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.jandex.ClassInfo;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.api.annotation.WebContext;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;
import org.jboss.ws.common.utils.UrlPatternUtils;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class WSIntegrationProcessorJAXWS_POJO extends AbstractIntegrationProcessorJAXWS {

    public WSIntegrationProcessorJAXWS_POJO() {
    }

    // @Override
    protected void processAnnotation(final DeploymentUnit unit, final EEModuleDescription moduleDescription)
            throws DeploymentUnitProcessingException {
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            return;
        }
        final Map<String, EEModuleClassDescription> classDescriptionMap = new HashMap<String, org.jboss.as.ee.component.EEModuleClassDescription>();
        final CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (EEModuleClassDescription classDescritpion : moduleDescription.getClassDescriptions()) {
            if (isJaxwsEndpoint(classDescritpion, index) && !exclude(unit, classDescritpion)) {
                classDescriptionMap.put(classDescritpion.getClassName(), classDescritpion);
            }
        }
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        final JAXWSDeployment jaxwsDeployment = getJaxwsDeployment(unit);
        if (jbossWebMD != null) {
            final Set<String> matchedEps = new HashSet<String>();
            for (final ServletMetaData servletMD : getServlets(jbossWebMD)) {
                final String endpointClassName = getEndpointClassName(servletMD);
                final String endpointName = getEndpointName(servletMD);
                if (classDescriptionMap.containsKey(endpointClassName) || matchedEps.contains(endpointClassName)) {
                    // creating component description for POJO endpoint
                    final ComponentDescription pojoComponent = createComponentDescription(unit, endpointName,
                            endpointClassName, endpointName);
                    final ServiceName pojoViewName = registerView(pojoComponent, endpointClassName);
                    final String urlPattern = getUrlPattern(endpointName, unit);
                    jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointName, endpointClassName, pojoViewName, urlPattern));
                    classDescriptionMap.remove(endpointClassName);
                    matchedEps.add(endpointClassName);
                } else {
                    if (unit.getParent() != null && DeploymentTypeMarker.isType(DeploymentType.EAR, unit.getParent())) {
                        final EEModuleDescription eeModuleDescription = unit.getParent().getAttachment(
                                org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
                        final CompositeIndex parentIndex = unit.getParent().getAttachment( Attachments.COMPOSITE_ANNOTATION_INDEX);
                        for (EEModuleClassDescription classDescription : eeModuleDescription.getClassDescriptions()) {
                            if (classDescription.getClassName().equals(endpointClassName)
                                    && isJaxwsEndpoint(classDescription, parentIndex)) {
                                final ComponentDescription pojoComponent = createComponentDescription(unit, endpointName,
                                        endpointClassName, endpointName);
                                final ServiceName pojoViewName = registerView(pojoComponent, endpointClassName);
                                final String urlPattern = getUrlPattern(endpointName, unit);
                                jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointName, endpointClassName, pojoViewName, urlPattern));

                            }
                        }
                    }
                }
            }
        }

        for (EEModuleClassDescription classDescription : classDescriptionMap.values()) {
            ClassInfo classInfo = null;
            String serviceName = null;
            String urlPattern = null;

            // #1 Override serviceName with the explicit urlPattern from port-component/port-component-uri in jboss-webservices.xml
            EJBEndpoint ejbEndpoint = getWebserviceMetadataEJBEndpoint(jaxwsDeployment, classDescription.getClassName());
            if (ejbEndpoint != null) {
                urlPattern = UrlPatternUtils.getUrlPatternByPortComponentURI(
                    getJBossWebserviceMetaDataPortComponent(unit, ejbEndpoint.getName()));
            }

            // #2 Override serviceName with @WebContext.urlPattern
            if (urlPattern == null) {
                final ClassAnnotationInformation<WebContext, WebContextAnnotationInfo> annotationWebContext =
                    classDescription.getAnnotationInformation(WebContext.class);
                if (annotationWebContext != null) {
                    WebContextAnnotationInfo wsInfo = annotationWebContext.getClassLevelAnnotations().get(0);
                    if (wsInfo != null && wsInfo.getUrlPattern().length() > 0) {
                        urlPattern = wsInfo.getUrlPattern();
                    }
                }
            }

            // #3 use serviceName declared in a class annotation
            if (urlPattern == null) {
                final ClassAnnotationInformation<WebService, WebServiceAnnotationInfo> annotationInfo = classDescription
                    .getAnnotationInformation(WebService.class);
                if (annotationInfo != null) {
                    WebServiceAnnotationInfo wsInfo = annotationInfo.getClassLevelAnnotations().get(0);
                    serviceName = wsInfo.getServiceName();
                    classInfo = (ClassInfo)wsInfo.getTarget();

                    urlPattern = UrlPatternUtils.getUrlPattern(classInfo.name().local(), serviceName);
                    if (jaxwsDeployment.contains(urlPattern)){
                        urlPattern = UrlPatternUtils.getUrlPattern(classInfo.name().local(), serviceName, wsInfo.getName());
                    }
                }

                final ClassAnnotationInformation<WebServiceProvider, WebServiceProviderAnnotationInfo> annotationProviderInfo = classDescription
                    .getAnnotationInformation(WebServiceProvider.class);
                if (annotationProviderInfo != null) {
                    WebServiceProviderAnnotationInfo wsInfo = annotationProviderInfo.getClassLevelAnnotations().get(0);
                    serviceName = wsInfo.getServiceName();
                    classInfo = (ClassInfo)wsInfo.getTarget();
                }
            }

            if (classInfo != null) {
                final String endpointClassName = classDescription.getClassName();
                final ComponentDescription pojoComponent = createComponentDescription(unit, endpointClassName,
                        endpointClassName, endpointClassName);
                final ServiceName pojoViewName = registerView(pojoComponent, endpointClassName);
                if (urlPattern == null) {
                    urlPattern = UrlPatternUtils.getUrlPattern(classInfo.name().local(), serviceName);
                }
                // register POJO endpoint
                jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointClassName,
                    pojoViewName, UrlPatternUtils.getUrlPattern(urlPattern)));
            }
        }

    }

    private boolean exclude(final DeploymentUnit unit, final EEModuleClassDescription classDescription) {
        //exclude if it's ejb3 and jms endpoint
        ClassInfo classInfo = null;
        ClassAnnotationInformation<WebService, WebServiceAnnotationInfo> annotationInfo = classDescription
                .getAnnotationInformation(WebService.class);
        if (annotationInfo != null) {
            classInfo = (ClassInfo) annotationInfo.getClassLevelAnnotations().get(0).getTarget();
        } else {
            ClassAnnotationInformation<WebServiceProvider, WebServiceProviderAnnotationInfo> providreInfo = classDescription
                    .getAnnotationInformation(WebServiceProvider.class);
            classInfo = (ClassInfo) providreInfo.getClassLevelAnnotations().get(0).getTarget();
        }

        if (isEjb3(classInfo) || isJmsEndpoint(unit, classInfo)) {
            return true;
        }
        return false;
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
            if (endpointClassName.equals(endpoint.getImplementor())) {
                return true;
            }
        }
        return false;
    }

}
