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

import static org.jboss.as.webservices.util.DotNames.JAXWS_SERVICE_CLASS;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXRPC_ENDPOINTS_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;

import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXRPCDeployment;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.as.webservices.webserviceref.WSReferences;
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
        final boolean hasEjbEndpoints = jaxwsDeployment != null ? jaxwsDeployment.getEjbEndpoints().size() > 0 : false;
        return hasEjbEndpoints ? jaxwsDeployment.getEjbEndpoints() : Collections.<EJBEndpoint>emptyList();
    }

    /**
     * Gets list of JAXRPC EJBs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXRPC EJBs meta data
     */
    public static List<EJBEndpoint> getJaxrpcEjbs(final DeploymentUnit unit) {
        final JAXRPCDeployment jaxrpcDeployment = getOptionalAttachment(unit, WSAttachmentKeys.JAXRPC_ENDPOINTS_KEY);
        final boolean hasEjbEndpoints = jaxrpcDeployment != null ? jaxrpcDeployment.getEjbEndpoints().size() > 0 : false;
        return hasEjbEndpoints ? jaxrpcDeployment.getEjbEndpoints() : Collections.<EJBEndpoint>emptyList();
    }

    /**
     * Gets list of JAXWS POJOs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXWS POJOs meta data
     */
    public static List<POJOEndpoint> getJaxwsPojos(final DeploymentUnit unit) {
        final JAXWSDeployment jaxwsDeployment = unit.getAttachment(WSAttachmentKeys.JAXWS_ENDPOINTS_KEY);
        final boolean hasPojoEndpoints = jaxwsDeployment != null ? jaxwsDeployment.getPojoEndpoints().size() > 0 : false;
        return hasPojoEndpoints ? jaxwsDeployment.getPojoEndpoints() : Collections.<POJOEndpoint>emptyList();
    }

    /**
     * Gets list of JAXRPC POJOs meta data.
     *
     * @param unit deployment unit
     * @return list of JAXRPC POJOs meta data
     */
    public static List<POJOEndpoint> getJaxrpcPojos(final DeploymentUnit unit) {
        final JAXRPCDeployment jaxrpcDeployment = unit.getAttachment(WSAttachmentKeys.JAXRPC_ENDPOINTS_KEY);
        final boolean hasPojoEndpoints = jaxrpcDeployment != null ? jaxrpcDeployment.getPojoEndpoints().size() > 0 : false;
        return hasPojoEndpoints ? jaxrpcDeployment.getPojoEndpoints() : Collections.<POJOEndpoint>emptyList();
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
                result = warMetaData.getJbossWebMetaData();
            }
        } else {
            result = getOptionalAttachment(unit, WSAttachmentKeys.JBOSSWEB_METADATA_KEY);
        }
        return result;
    }

    // TODO: useful ?
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

    public static JAXRPCDeployment getJaxrpcDeployment(final DeploymentUnit unit) {
        JAXRPCDeployment wsDeployment = unit.getAttachment(JAXRPC_ENDPOINTS_KEY);
        if (wsDeployment == null) {
            wsDeployment = new JAXRPCDeployment();
            unit.putAttachment(JAXRPC_ENDPOINTS_KEY, wsDeployment);
        }
        return wsDeployment;
    }

    public static WSReferences getWSRefRegistry(final DeploymentUnit unit) {
        WSReferences refRegistry = unit.getAttachment(WSAttachmentKeys.WS_REFERENCES);
        if (refRegistry == null) {
            refRegistry = WSReferences.newInstance();
            unit.putAttachment(WSAttachmentKeys.WS_REFERENCES, refRegistry);
        }
        return refRegistry;
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
            final ModuleMetaData moduleMD = jbossAppMD.getModule(dep.getSimpleName());
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
        ServiceController<T> service = (ServiceController<T>)WSServices.getContainerRegistry().getService(serviceName);
        return service != null ? service.getValue() : null;
    }

}
