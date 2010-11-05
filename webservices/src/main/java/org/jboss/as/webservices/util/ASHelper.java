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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.AttachmentKeys;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.metadata.common.jboss.WebserviceDescriptionMetaData;
import org.jboss.metadata.common.jboss.WebserviceDescriptionsMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * JBoss AS integration helper class.
 *
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class ASHelper {

    /**
     * EJB invocation property.
     */
    public static final String CONTAINER_NAME = "org.jboss.wsf.spi.invocation.ContainerName";

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(ASHelper.class);

    /**
     * Forbidden constructor.
     */
    private ASHelper() {
        super();
    }

    /**
     * Returns true if unit contains JAXWS JSE, JAXRPC JSE, JAXWS EJB or JAXRPC EJB deployment.
     *
     * @param unit deployment unit
     * @return true if JAXWS JSE, JAXRPC JSE, JAXWS EJB or JAXRPC EJB deployment, false otherwise.
     */
    public static boolean isWebServiceDeployment(final DeploymentUnit unit) {
        return ASHelper.getOptionalAttachment(unit, AttachmentKeys.DEPLOYMENT_TYPE_KEY) != null;
    }

    /**
     * Returns true if unit contains JAXRPC EJB deployment.
     *
     * @param unit deployment unit
     * @return true if JAXRPC EJB deployment, false otherwise
     */
    public static boolean isJaxrpcEjbDeployment(final DeploymentUnit unit) {
        final DeploymentType deploymentType = ASHelper.getOptionalAttachment(unit, AttachmentKeys.DEPLOYMENT_TYPE_KEY);

        return DeploymentType.JAXRPC_EJB21.equals(deploymentType);
    }

    /**
     * Returns true if unit contains JAXRPC JSE deployment.
     *
     * @param unit deployment unit
     * @return true if JAXRPC JSE deployment, false otherwise
     */
    public static boolean isJaxrpcJseDeployment(final DeploymentUnit unit) {
        final DeploymentType deploymentType = ASHelper.getOptionalAttachment(unit, AttachmentKeys.DEPLOYMENT_TYPE_KEY);

        return DeploymentType.JAXRPC_JSE.equals(deploymentType);
    }

    /**
     * Returns true if unit contains JAXWS EJB deployment.
     *
     * @param unit deployment unit
     * @return true if JAXWS EJB deployment, false otherwise
     */
    public static boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        final DeploymentType deploymentType = ASHelper.getOptionalAttachment(unit, AttachmentKeys.DEPLOYMENT_TYPE_KEY);

        return DeploymentType.JAXWS_EJB3.equals(deploymentType);
    }

    /**
     * Returns true if unit contains JAXWS JSE deployment.
     *
     * @param unit deployment unit
     * @return true if JAXWS JSE deployment, false otherwise
     */
    public static boolean isJaxwsJseDeployment(final DeploymentUnit unit) {
        final DeploymentType deploymentType = ASHelper.getOptionalAttachment(unit, AttachmentKeys.DEPLOYMENT_TYPE_KEY);

        return DeploymentType.JAXWS_JSE.equals(deploymentType);
    }

    /**
     * Returns true if unit contains either JAXWS JSE or JAXRPC JSE deployment.
     *
     * @param unit deployment unit
     * @return true if either JAXWS JSE or JAXRPC JSE deployment, false otherwise.
     */
    public static boolean isJseDeployment(final DeploymentUnit unit) {
        final boolean isJaxwsJse = ASHelper.isJaxwsJseDeployment(unit);
        final boolean isJaxrpcJse = ASHelper.isJaxrpcJseDeployment(unit);

        return isJaxwsJse || isJaxrpcJse;
    }

    /**
     * Returns true if unit contains either JAXWS EJB or JAXRPC EJB deployment.
     *
     * @param unit deployment unit
     * @return true if either JAXWS EJB or JAXRPC EJB deployment, false otherwise
     */
    public static boolean isEjbDeployment(final DeploymentUnit unit) {
        final boolean isJaxwsEjb = ASHelper.isJaxwsEjbDeployment(unit);
        final boolean isJaxrpcEjb = ASHelper.isJaxrpcEjbDeployment(unit);

        return isJaxwsEjb || isJaxrpcEjb;
    }

    /**
     * Returns true if unit contains either JAXWS EJB or JAXWS JSE deployment.
     *
     * @param unit deployment unit
     * @return true if either JAXWS EJB or JAXWS JSE deployment, false otherwise
     */
    public static boolean isJaxwsDeployment(final DeploymentUnit unit) {
        final boolean isJaxwsEjb = ASHelper.isJaxwsEjbDeployment(unit);
        final boolean isJaxwsJse = ASHelper.isJaxwsJseDeployment(unit);

        return isJaxwsEjb || isJaxwsJse;
    }

    /**
     * Returns true if unit contains either JAXRPC EJB or JAXRPC JSE deployment.
     *
     * @param unit deployment unit
     * @return true if either JAXRPC EJB or JAXRPC JSE deployment, false otherwise
     */
    public static boolean isJaxrpcDeployment(final DeploymentUnit unit) {
        final boolean isJaxrpcEjb = ASHelper.isJaxrpcEjbDeployment(unit);
        final boolean isJaxrpcJse = ASHelper.isJaxrpcJseDeployment(unit);

        return isJaxrpcEjb || isJaxrpcJse;
    }

    /**
     * Gets list of JAXWS servlets meta data.
     *
     * @param unit deployment unit
     * @return list of JAXWS servlets meta data
     */
    public static List<ServletMetaData> getJaxwsServlets(final DeploymentUnit unit) {
        return ASHelper.getWebServiceServlets(unit, true);
    }

    /**
     * Gets list of JAXRPC servlets meta data.
     *
     * @param unit deployment unit
     * @return list of JAXRPC servlets meta data
     */
    public static List<ServletMetaData> getJaxrpcServlets(final DeploymentUnit unit) {
        return ASHelper.getWebServiceServlets(unit, false);
    }

    /**
     * Gets list of JAXWS EJBs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXWS EJBs meta data
     */
    public static List<WebServiceDeclaration> getJaxwsEjbs(final DeploymentUnit unit) {
        final WebServiceDeployment wsDeployment = ASHelper.getRequiredAttachment(unit, AttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY);
        final List<WebServiceDeclaration> endpoints = new ArrayList<WebServiceDeclaration>();

        final Iterator<WebServiceDeclaration> ejbIterator = wsDeployment.getServiceEndpoints().iterator();
        while (ejbIterator.hasNext()) {
            final WebServiceDeclaration ejbContainer = ejbIterator.next();
            if (ASHelper.isWebServiceBean(ejbContainer)) {
                endpoints.add(ejbContainer);
            }
        }

        return endpoints;
    }

    /**
     * Returns true if EJB container is webservice endpoint.
     *
     * @param ejbContainerAdapter EJB container adapter
     * @return true if EJB container is webservice endpoint, false otherwise
     */
    public static boolean isWebServiceBean(final WebServiceDeclaration ejbContainerAdapter) {
        final boolean isWebService = ejbContainerAdapter.getAnnotation(WebService.class) != null;
        final boolean isWebServiceProvider = ejbContainerAdapter.getAnnotation(WebServiceProvider.class) != null;

        return isWebService || isWebServiceProvider;
    }

    /**
     * Returns endpoint class name.
     *
     * @param servletMD servlet meta data
     * @return endpoint class name
     */
    public static String getEndpointName(final ServletMetaData servletMD) {
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

        throw new IllegalStateException("Cannot find servlet for link: " + servletName);
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
            ASHelper.LOGGER.error("Cannot find attachment in deployment unit: " + key);
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

    /**
     * Returns true if deployment unit have attachment value associated with the <b>key</b>.
     *
     * @param unit deployment unit
     * @param key attachment key
     * @return true if contains attachment, false otherwise
     */
    public static boolean hasAttachment(final DeploymentUnit unit, final AttachmentKey<?> key) {
        return ASHelper.getOptionalAttachment(unit, key) != null;
    }

    /**
     * Returns first webservice description meta data or null if not found.
     *
     * @param wsDescriptionsMD webservice descriptions
     * @return webservice description
     */
    public static WebserviceDescriptionMetaData getWebserviceDescriptionMetaData(
            final WebserviceDescriptionsMetaData wsDescriptionsMD) {
        if (wsDescriptionsMD != null) {
            if (wsDescriptionsMD.size() > 1) {
                ASHelper.LOGGER.warn("Multiple <webservice-description> elements not supported");
            }

            if (wsDescriptionsMD.size() > 0) {
                return wsDescriptionsMD.iterator().next();
            }
        }

        return null;
    }

    /**
     * Gets list of JAXRPC or JAXWS servlets meta data.
     *
     * @param unit deployment unit
     * @param jaxws if passed value is <b>true</b> JAXWS servlets list will be returned, otherwise JAXRPC servlets list
     * @return either JAXRPC or JAXWS servlets list
     */
    private static List<ServletMetaData> getWebServiceServlets(final DeploymentUnit unit, final boolean jaxws) {
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        final List<ServletMetaData> endpoints = new ArrayList<ServletMetaData>();
        final Index annotationIndex = getRootAnnotationIndex(unit);
        final DotName webserviceAnnotation = DotName.createSimple(WebService.class.getName());
        final DotName webserviceProviderAnnotation = DotName.createSimple(WebServiceProvider.class.getName());

        for (ServletMetaData servletMD : jbossWebMD.getServlets()) {
            final String endpointClassName = ASHelper.getEndpointName(servletMD);
            if (endpointClassName != null && endpointClassName.length() > 0) { // exclude JSP
                // check webservice annotations
                Map<DotName, List<AnnotationInstance>> map = null;
                if (annotationIndex != null) {
                    ClassInfo ci = annotationIndex.getClassByName(DotName.createSimple(endpointClassName));
                    if (ci != null) {
                        map = ci.annotations();
                    }
                }
                if (map == null) {
                    map = new HashMap<DotName, List<AnnotationInstance>>();
                }
                final boolean isWebService = map.containsKey(webserviceAnnotation);
                final boolean isWebServiceProvider = map.containsKey(webserviceProviderAnnotation);
                // detect webservice type
                final boolean isJaxwsEndpoint = jaxws && (isWebService || isWebServiceProvider);
                final boolean isJaxrpcEndpoint = !jaxws && (!isWebService && !isWebServiceProvider);

                if (isJaxwsEndpoint || isJaxrpcEndpoint) {
                    endpoints.add(servletMD);
                }
            }
        }
        return endpoints;
    }

    /**
     * Gets the JBossWebMetaData from the WarMetaData attached to the provided deployment unit, if any.
     *
     * @param unit
     * @return the JBossWebMetaData or null if either that or the parent WarMetaData are not found.
     */
    public static JBossWebMetaData getJBossWebMetaData(final DeploymentUnit unit) {
        final WarMetaData warMetaData = ASHelper.getOptionalAttachment(unit, WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData result = null;
        if (warMetaData != null) {
            result = warMetaData.getMergedJBossWebMetaData();
            if (result == null) {
                result = warMetaData.getJbossWebMetaData();
            }
        }
        return result;
    }

    public static Index getRootAnnotationIndex(final DeploymentUnit unit) {
        Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(unit);
        for (ResourceRoot rr : indexes.keySet()) {
            if (ModuleRootMarker.isModuleRoot(rr)) {
                return indexes.get(rr);
            }
        }
        throw new RuntimeException("Could not find root annotation index!");
    }
}
