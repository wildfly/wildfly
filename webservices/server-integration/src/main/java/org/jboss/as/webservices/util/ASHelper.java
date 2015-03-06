/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.util;

import static org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT;
import static org.jboss.as.server.deployment.Attachments.RESOURCE_ROOTS;
import static org.jboss.as.webservices.util.DotNames.JAXWS_SERVICE_CLASS;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JBOSS_WEBSERVICES_METADATA_KEY;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.webservices.deployers.WebServiceAnnotationInfo;
import org.jboss.as.webservices.deployers.WebServiceProviderAnnotationInfo;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.as.webservices.webserviceref.WSRefRegistry;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.WebModuleMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.webservices.JBossPortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;

/**
 * JBoss AS integration helper class.
 *
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class ASHelper {

    private ASHelper() {
    }

    /**
     * Gets list of JAXWS EJBs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXWS EJBs meta data
     */
    public static List<EJBEndpoint> getJaxwsEjbs(final DeploymentUnit unit) {
        final JAXWSDeployment jaxwsDeployment = getOptionalAttachment(unit, WSAttachmentKeys.JAXWS_ENDPOINTS_KEY);
        return jaxwsDeployment != null ? jaxwsDeployment.getEjbEndpoints() : Collections.<EJBEndpoint>emptyList();
    }

    /**
     * Gets list of JAXWS POJOs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXWS POJOs meta data
     */
    public static List<POJOEndpoint> getJaxwsPojos(final DeploymentUnit unit) {
        final JAXWSDeployment jaxwsDeployment = unit.getAttachment(WSAttachmentKeys.JAXWS_ENDPOINTS_KEY);
        return jaxwsDeployment != null ? jaxwsDeployment.getPojoEndpoints() : Collections.<POJOEndpoint>emptyList();
    }

    /**
     * Returns endpoint name.
     *
     * @param servletMD servlet meta data
     * @return endpoint name
     */
    public static String getEndpointName(final ServletMetaData servletMD) {
        final String endpointName = servletMD.getName();
        return endpointName != null ? endpointName.trim() : null;
    }

    /**
     * Returns endpoint class name.
     *
     * @param servletMD servlet meta data
     * @return endpoint class name
     */
    public static String getEndpointClassName(final ServletMetaData servletMD) {
        final String endpointClass = servletMD.getServletClass();
        return endpointClass != null ? endpointClass.trim() : null;
    }

    /**
     * Returns servlet meta data for requested servlet name.
     *
     * @param jbossWebMD jboss web meta data
     * @param servletName servlet name
     * @return servlet meta data
     */
    public static ServletMetaData getServletForName(final JBossWebMetaData jbossWebMD, final String servletName) {
        for (JBossServletMetaData servlet : jbossWebMD.getServlets()) {
            if (servlet.getName().equals(servletName)) {
                return servlet;
            }
        }

        return null;
    }

    /**
     * Returns required attachment value from deployment unit.
     *
     * @param <A> expected value
     * @param unit deployment unit
     * @param key attachment key
     * @return required attachment
     * @throws IllegalStateException if attachment value is null
     */
    public static <A> A getRequiredAttachment(final DeploymentUnit unit, final AttachmentKey<A> key) {
        final A value = unit.getAttachment(key);
        if (value == null) {
            throw new IllegalStateException();
        }

        return value;
    }

    /**
     * Returns optional attachment value from deployment unit or null if not bound.
     *
     * @param <A> expected value
     * @param unit deployment unit
     * @param key attachment key
     * @return optional attachment value or null
     */
    public static <A> A getOptionalAttachment(final DeploymentUnit unit, final AttachmentKey<A> key) {
        return unit.getAttachment(key);
    }

    public static boolean isJaxwsService(final ClassInfo current, final CompositeIndex index) {
        ClassInfo tmp = current;
        while (tmp != null) {
            final DotName superName = tmp.superName();
            if (JAXWS_SERVICE_CLASS.equals(superName)) {
                return true;
            }
            tmp = index.getClassByName(superName);
        }
        return false;
    }

    public static boolean isJaxwsService(final ClassInfo current, final Index index) {
        ClassInfo tmp = current;
        while (tmp != null) {
            final DotName superName = tmp.superName();
            if (JAXWS_SERVICE_CLASS.equals(superName)) {
                return true;
            }
            tmp = index.getClassByName(superName);
        }
        return false;
    }

    public static boolean isJaxwsEndpointInterface(final ClassInfo clazz) {
        final short flags = clazz.flags();
        if (!Modifier.isInterface(flags)) return false;
        if (!Modifier.isPublic(flags)) return false;
        return clazz.annotations().containsKey(WEB_SERVICE_ANNOTATION);
    }

    public static boolean hasClassesFromPackage(final Index index, final String pck) {
        for (ClassInfo ci : index.getKnownClasses()) {
            if (ci.name().toString().startsWith(pck)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isJaxwsEndpoint(final ClassInfo clazz, final CompositeIndex index) {
        return isJaxwsEndpoint(clazz, index, true);
    }

    public static boolean isJaxwsEndpoint(final ClassInfo clazz, final CompositeIndex index, boolean log) {
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
            if (log) {
                WSLogger.ROOT_LOGGER.mutuallyExclusiveAnnotations(clazz.name().toString());
            }
            return false;
        }
        if (Modifier.isFinal(flags)) {
            if (log) {
                WSLogger.ROOT_LOGGER.finalEndpointClassDetected(clazz.name().toString());
            }
            return false;
        }
        return true;
    }

    public static boolean isJaxwsEndpoint(final EEModuleClassDescription classDescription, final CompositeIndex index) {
        ClassInfo classInfo = null;
        WebServiceAnnotationInfo webserviceAnnoationInfo = null;
        final ClassAnnotationInformation<WebService, WebServiceAnnotationInfo> classAnnotationInfo = classDescription.getAnnotationInformation(WebService.class);
        if (classAnnotationInfo!= null && !classAnnotationInfo.getClassLevelAnnotations().isEmpty()) {
            webserviceAnnoationInfo = classAnnotationInfo.getClassLevelAnnotations().get(0);
            classInfo = (ClassInfo)webserviceAnnoationInfo.getTarget();
        }
        WebServiceProviderAnnotationInfo webserviceProviderAnnoationInfo = null;
        final ClassAnnotationInformation<WebServiceProvider, WebServiceProviderAnnotationInfo> providerAnnotationInfo = classDescription.getAnnotationInformation(WebServiceProvider.class);
        if (providerAnnotationInfo!= null && !providerAnnotationInfo.getClassLevelAnnotations().isEmpty()) {
            webserviceProviderAnnoationInfo = providerAnnotationInfo.getClassLevelAnnotations().get(0);
            classInfo = (ClassInfo)webserviceProviderAnnoationInfo.getTarget();
        }
        if (classInfo == null) {
            return false;
        }
        // assert JAXWS endpoint class flags
        final short flags = classInfo.flags();
        if (Modifier.isInterface(flags)) return false;
        if (Modifier.isAbstract(flags)) return false;
        if (!Modifier.isPublic(flags)) return false;
        if (isJaxwsService(classInfo, index)) return false;

        if (webserviceAnnoationInfo !=null && webserviceProviderAnnoationInfo != null) {
            WSLogger.ROOT_LOGGER.mutuallyExclusiveAnnotations(classInfo.name().toString());
            return false;
        }
        if (Modifier.isFinal(flags)) {
            WSLogger.ROOT_LOGGER.finalEndpointClassDetected(classInfo.name().toString());
            return false;
        }
        return true;
    }

    /**
     * Gets the JBossWebMetaData from the WarMetaData attached to the provided deployment unit, if any.
     *
     * @param unit
     * @return the JBossWebMetaData or null if either that or the parent WarMetaData are not found.
     */
    public static JBossWebMetaData getJBossWebMetaData(final DeploymentUnit unit) {
        final WarMetaData warMetaData = getOptionalAttachment(unit, WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData result = null;
        if (warMetaData != null) {
            result = warMetaData.getMergedJBossWebMetaData();
            if (result == null) {
                result = warMetaData.getJBossWebMetaData();
            }
        } else {
            result = getOptionalAttachment(unit, WSAttachmentKeys.JBOSSWEB_METADATA_KEY);
        }
        return result;
    }

    public static List<AnnotationInstance> getAnnotations(final DeploymentUnit unit, final DotName annotation) {
       final CompositeIndex compositeIndex = getRequiredAttachment(unit, Attachments.COMPOSITE_ANNOTATION_INDEX);
       return compositeIndex.getAnnotations(annotation);
    }

    public static JAXWSDeployment getJaxwsDeployment(final DeploymentUnit unit) {
        JAXWSDeployment wsDeployment = unit.getAttachment(JAXWS_ENDPOINTS_KEY);
        if (wsDeployment == null) {
            wsDeployment = new JAXWSDeployment();
            unit.putAttachment(JAXWS_ENDPOINTS_KEY, wsDeployment);
        }
        return wsDeployment;
    }

    /**
     * Return a named port-component from the jboss-webservices.xml
     * @param unit
     * @param name
     * @return
     */
    public static JBossPortComponentMetaData getJBossWebserviceMetaDataPortComponent(
        final DeploymentUnit unit, final String name) {

        if (name != null) {
            final JBossWebservicesMetaData jbossWebserviceMetaData = unit.getAttachment(JBOSS_WEBSERVICES_METADATA_KEY);

            if (jbossWebserviceMetaData != null) {
                JBossPortComponentMetaData[] portComponent = jbossWebserviceMetaData.getPortComponents();

                if (portComponent != null) {
                    for (JBossPortComponentMetaData component : portComponent) {
                        if (name.equals(component.getEjbName())) {
                            return component;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an EJBEndpoint based upon fully qualified classname.
     * @param jaxwsDeployment
     * @param className
     * @return
     */
    public static EJBEndpoint getWebserviceMetadataEJBEndpoint(final JAXWSDeployment jaxwsDeployment,
                                                               final String className) {

        java.util.List<EJBEndpoint> ejbEndpointList = jaxwsDeployment.getEjbEndpoints();
        for (EJBEndpoint ejbEndpoint : ejbEndpointList) {
            if (className.equals(ejbEndpoint.getClassName())) {
                return ejbEndpoint;
            }
        }
        return null;
    }

    /**
     * Returns context root associated with webservice deployment.
     *
     * If there's application.xml descriptor provided defining nested web module, then context root defined there will be
     * returned. Otherwise context root defined in jboss-web.xml will be returned.
     *
     * @param dep webservice deployment
     * @param jbossWebMD jboss web meta data
     * @return context root
     */
    public static String getContextRoot(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        final JBossAppMetaData jbossAppMD = unit.getParent() == null ? null : ASHelper.getOptionalAttachment(unit.getParent(),
                WSAttachmentKeys.JBOSS_APP_METADATA_KEY);

        String contextRoot = null;

        // prefer context root defined in application.xml over one defined in jboss-web.xml
        if (jbossAppMD != null) {
            final ModuleMetaData moduleMD = jbossAppMD.getModules().get(dep.getSimpleName());
            if (moduleMD != null) {
                final WebModuleMetaData webModuleMD = (WebModuleMetaData) moduleMD.getValue();
                contextRoot = webModuleMD.getContextRoot();
            }
        }

        if (contextRoot == null) {
            contextRoot = jbossWebMD != null ? jbossWebMD.getContextRoot() : null;
        }

        return contextRoot;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMSCService(final ServiceName serviceName, final Class<T> clazz) {
        ServiceController<T> service = (ServiceController<T>) CurrentServiceContainer.getServiceContainer().getService(serviceName);
        return service != null ? service.getValue() : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMSCService(final ServiceName serviceName, final Class<T> clazz, final OperationContext context) {
        ServiceController<T> service = (ServiceController<T>)context.getServiceRegistry(false).getService(serviceName);
        return service != null ? service.getValue() : null;
    }

    public static WSRefRegistry getWSRefRegistry(final DeploymentUnit unit) {
        WSRefRegistry refRegistry = unit.getAttachment(WSAttachmentKeys.WS_REFREGISTRY);
        if (refRegistry == null) {
            refRegistry = WSRefRegistry.newInstance();
            unit.putAttachment(WSAttachmentKeys.WS_REFREGISTRY, refRegistry);
        }
        return refRegistry;
    }

    public static List<ResourceRoot> getResourceRoots(DeploymentUnit unit) {
        // wars define resource roots
        AttachmentList<ResourceRoot> resourceRoots = unit.getAttachment(RESOURCE_ROOTS);
        if (!unit.getName().endsWith(".war") && EjbDeploymentMarker.isEjbDeployment(unit)) {
            // ejb archives don't define resource roots, using root resource
            resourceRoots = new AttachmentList<ResourceRoot>(ResourceRoot.class);
            final ResourceRoot root = unit.getAttachment(DEPLOYMENT_ROOT);
            resourceRoots.add(root);
        }
        return resourceRoots;
    }
}
