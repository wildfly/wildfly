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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jws.WebService;
import javax.servlet.Servlet;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.metadata.common.jboss.WebserviceDescriptionMetaData;
import org.jboss.metadata.common.jboss.WebserviceDescriptionsMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.as.webservices.metadata.WebServiceDeclaration;
import org.jboss.as.webservices.metadata.WebServiceDeployment;

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

    /** @WebService jandex annotation. */
    public static final DotName WEB_SERVICE_ANNOTATION = DotName.createSimple(WebService.class.getName());

    /** @WebServiceProvider jandex annotation. */
    public static final DotName WEB_SERVICE_PROVIDER_ANNOTATION = DotName.createSimple(WebServiceProvider.class.getName());

    private static final String SERVLET_CLASS_NAME = Servlet.class.getName();

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
        return ASHelper.getOptionalAttachment(unit, WSAttachmentKeys.DEPLOYMENT_TYPE_KEY) != null;
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
        final WebServiceDeployment wsDeployment = ASHelper.getRequiredAttachment(unit, WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY);

        return Collections.unmodifiableList(wsDeployment.getServiceEndpoints());
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
        return selectWebServiceServlets(unit, jbossWebMD.getServlets(), jaxws);
    }

    /**
     * Return a new sublist of the provided ServletMetaData list including the WS servlet data only
     *
     * @param annotationIndex the annotation index to use for scanning for annotations
     * @param smd the initial servlet metadata collection
     * @param jaxws if passed value is <b>true</b> JAXWS servlets list will be returned, otherwise JAXRPC servlets list
     * @return either JAXRPC or JAXWS servlets list
     */
    private static <T extends ServletMetaData> List<ServletMetaData> selectWebServiceServlets(final DeploymentUnit unit, final Collection<T> smd, final boolean jaxws) {
        if (smd == null) return Collections.emptyList();
        final CompositeIndex index = ASHelper.getOptionalAttachment(unit, Attachments.COMPOSITE_ANNOTATION_INDEX);

        final List<ServletMetaData> endpoints = new ArrayList<ServletMetaData>();

        for (final ServletMetaData servletMD : smd) {
            final boolean isWebServiceEndpoint = index != null ? isWebserviceEndpoint(servletMD, index, jaxws) : isWebserviceEndpoint(
                    servletMD, unit.getAttachment(WSAttachmentKeys.CLASSLOADER_KEY), jaxws);
            if (isWebServiceEndpoint) {
                endpoints.add(servletMD);
            }
        }

        return endpoints;
    }

    private static boolean isWebserviceEndpoint(final ServletMetaData servletMD, final CompositeIndex index, boolean jaxws) {
        final String endpointClassName = ASHelper.getEndpointName(servletMD);
        if (isJSP(endpointClassName)) return false;
        final DotName endpointDotName = DotName.createSimple(endpointClassName);
        final ClassInfo endpointClassInfo = index.getClassByName(endpointDotName);

        if (endpointClassInfo != null) {
            if (jaxws) {
                //directly check annotations when looking for jaxws endpoints
                if (endpointClassInfo.annotations().containsKey(WEB_SERVICE_ANNOTATION))
                    return true;
                if (endpointClassInfo.annotations().containsKey(WEB_SERVICE_PROVIDER_ANNOTATION))
                    return true;
            } else {
                //just verify the class is not a servlet for jaxrpc endpoints
                if (!isServlet(endpointClassInfo, index))
                    return true;
            }
        }

        return false;
    }

    private static boolean isWebserviceEndpoint(final ServletMetaData servletMD, final ClassLoader loader, boolean jaxws) {
        final String endpointClassName = ASHelper.getEndpointName(servletMD);
        if (isJSP(endpointClassName)) return false;
        final Class<?> endpointClass = ASHelper.getEndpointClass(endpointClassName, loader);
        if (endpointClass != null) {
            if (jaxws) {
                if (endpointClass.isAnnotationPresent(WebService.class))
                    return true;
                if (endpointClass.isAnnotationPresent(WebServiceProvider.class))
                    return true;
            } else {
                if (!Servlet.class.isAssignableFrom(endpointClass))
                    return true;
            }
        }
        return false;
    }

    private static Class<?> getEndpointClass(final String endpointClassName, final ClassLoader loader) {
        try {
            final Class<?> endpointClass = loader.loadClass(endpointClassName);
            return (!Servlet.class.isAssignableFrom(endpointClass)) ? endpointClass : null;
        } catch (ClassNotFoundException cnfe) {
            LOGGER.warn("Cannot load servlet class: " + endpointClassName, cnfe);
            return null;
        }
    }

    private static boolean isJSP(final String endpointClassName) {
        return endpointClassName == null || endpointClassName.length() == 0;
    }

    protected static boolean isServlet(final ClassInfo info, CompositeIndex index) {
        Set<DotName> interfacesToProcess = new HashSet<DotName>();
        Set<DotName> processedInterfaces = new HashSet<DotName>();
        boolean b = isServlet(info, index, interfacesToProcess);
        while (!b && !interfacesToProcess.isEmpty()) {
            final Iterator<DotName> toProcess = interfacesToProcess.iterator();
            DotName dn = toProcess.next();
            toProcess.remove();
            processedInterfaces.add(dn);
            b = extendsServlet(dn, index, interfacesToProcess, processedInterfaces);
        }
        return b;
    }

    private static boolean isServlet(final ClassInfo info, CompositeIndex index, Set<DotName> interfaces) {
        for (DotName dn : info.interfaces()) {
            if (SERVLET_CLASS_NAME.equals(dn.toString())) {
                return true;
            } else {
                interfaces.add(dn);
            }
        }
        final DotName superName = info.superName();
        if (!"java.lang.Object".equals(superName.toString())) {
            ClassInfo su = index.getClassByName(superName);
            if (su != null) {
                return isServlet(su, index, interfaces);
            }
        }
        return false;
    }

    private static boolean extendsServlet(DotName current, CompositeIndex index, Set<DotName> interfacesToProcess, Set<DotName> processedInterfaces) {
        ClassInfo ci = index.getClassByName(current);
        if (ci != null) {
            final DotName superName = ci.superName();
            if (SERVLET_CLASS_NAME.equals(superName.toString())) {
                return true;
            } else if (!"java.lang.Object".equals(superName.toString()) && !processedInterfaces.contains(superName)) {
                interfacesToProcess.add(superName);
            }
        }
        return false;
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
        } else {
            result = ASHelper.getOptionalAttachment(unit, WSAttachmentKeys.JBOSSWEB_METADATA_KEY);
        }
        return result;
    }

    public static List<AnnotationInstance> getAnnotations(final DeploymentUnit unit, final DotName annotation) {
       final CompositeIndex compositeIndex = ASHelper.getRequiredAttachment(unit, Attachments.COMPOSITE_ANNOTATION_INDEX);
       return compositeIndex.getAnnotations(annotation);
    }
}
