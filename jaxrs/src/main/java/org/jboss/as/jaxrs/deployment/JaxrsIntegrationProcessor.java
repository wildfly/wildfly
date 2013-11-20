/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs.deployment;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import static org.jboss.as.jaxrs.JaxrsLogger.JAXRS_LOGGER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "javax.ws.rs.Application";
    public static final String RESTEASY_SCAN = "resteasy.scan";
    public static final String RESTEASY_SCAN_RESOURCES = "resteasy.scan.resources";
    public static final String RESTEASY_SCAN_PROVIDERS = "resteasy.scan.providers";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);


        if (resteasy == null)
            return;

        //remove the resteasy.scan parameter
        //because it is not needed
        final List<ParamValueMetaData> params = webdata.getContextParams();
        if (params != null) {
            Iterator<ParamValueMetaData> it = params.iterator();
            while (it.hasNext()) {
                final ParamValueMetaData param = it.next();
                if (param.getParamName().equals(RESTEASY_SCAN)) {
                    it.remove();
                } else if (param.getParamName().equals(RESTEASY_SCAN_RESOURCES)) {
                    it.remove();
                } else if (param.getParamName().equals(RESTEASY_SCAN_PROVIDERS)) {
                    it.remove();
                }
            }
        }


        final Map<ModuleIdentifier, ResteasyDeploymentData> attachmentMap = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        final List<ResteasyDeploymentData> additionalData = new ArrayList<ResteasyDeploymentData>();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec != null && attachmentMap != null) {
            final Set<ModuleIdentifier> identifiers = new HashSet<ModuleIdentifier>();
            for (ModuleDependency dep : moduleSpec.getAllDependencies()) {
                //make sure we don't double up
                if (!identifiers.contains(dep.getIdentifier())) {
                    identifiers.add(dep.getIdentifier());
                    if (attachmentMap.containsKey(dep.getIdentifier())) {
                        additionalData.add(attachmentMap.get(dep.getIdentifier()));
                    }
                }
            }
            resteasy.merge(additionalData);
        }
        if (!resteasy.getScannedResourceClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedResourceClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String resources = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS resource classes: %s", resources);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, resources);
        }
        if (!resteasy.getScannedProviderClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String provider : resteasy.getScannedProviderClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(provider);
                } else {
                    buf.append(",").append(provider);
                }
            }
            String providers = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS provider classes: %s", providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, providers);
        }

        if (!resteasy.getScannedJndiComponentResources().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedJndiComponentResources()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String providers = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS jndi component resource classes: %s", providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
        }

        if (!resteasy.isUnwrappedExceptionsParameterSet()) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS, "javax.ejb.EJBException");
        }

        if (resteasy.hasBootClasses() || resteasy.isDispatcherCreated())
            return;

        // ignore any non-annotated Application class that doesn't have a servlet mapping
        Set<Class> applicationClassSet = new HashSet<>();
        for (Class<? extends Application> clazz : resteasy.getScannedApplicationClasses()) {
            if (clazz.isAnnotationPresent(ApplicationPath.class) || servletMappingsExist(webdata, clazz.getName())) {
                applicationClassSet.add(clazz);
            }
        }

        // add default servlet
        if (applicationClassSet.size() == 0) {
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(JAX_RS_SERVLET_NAME);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            addServlet(webdata, servlet);
            setServletMappingPrefix(webdata, JAX_RS_SERVLET_NAME, servlet);
            return;
        }

        for (Class<? extends Application> applicationClass : applicationClassSet) {
            String servletName = null;

            servletName = applicationClass.getName();
            JBossServletMetaData servlet = new JBossServletMetaData();
            // must load on startup for services like JSAPI to work
            servlet.setLoadOnStartup("" + 0);
            servlet.setName(servletName);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            setServletInitParam(servlet, SERVLET_INIT_PARAM, applicationClass.getName());
            addServlet(webdata, servlet);
            if (!servletMappingsExist(webdata, servletName)) {
                try {
                    //no mappings, add our own
                    List<String> patterns = new ArrayList<String>();
                    //for some reason the spec requires this to be decoded
                    String pathValue = URLDecoder.decode(applicationClass.getAnnotation(ApplicationPath.class).value().trim(), "UTF-8");
                    if (!pathValue.startsWith("/")) {
                        pathValue = "/" + pathValue;
                    }
                    String prefix = pathValue;
                    if (pathValue.endsWith("/")) {
                        pathValue += "*";
                    } else {
                        pathValue += "/*";
                    }
                    patterns.add(pathValue);
                    setServletInitParam(servlet, "resteasy.servlet.mapping.prefix", prefix);
                    ServletMappingMetaData mapping = new ServletMappingMetaData();
                    mapping.setServletName(servletName);
                    mapping.setUrlPatterns(patterns);
                    if (webdata.getServletMappings() == null) {
                        webdata.setServletMappings(new ArrayList<ServletMappingMetaData>());
                    }
                    webdata.getServletMappings().add(mapping);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                setServletMappingPrefix(webdata, servletName, servlet);
            }

        }
    }

    protected void setServletInitParam(JBossServletMetaData servlet, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = servlet.getInitParam();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            servlet.setInitParam(params);
        }
        params.add(param);
    }


    private void setServletMappingPrefix(JBossWebMetaData webdata, String servletName, JBossServletMetaData servlet) {
        final List<ServletMappingMetaData> mappings = webdata.getServletMappings();
        if (mappings != null) {
            boolean mappingSet = false;
            for (final ServletMappingMetaData mapping : mappings) {
                if (mapping.getServletName().equals(servletName)) {
                    if (mapping.getUrlPatterns() != null) {
                        for (String pattern : mapping.getUrlPatterns()) {
                            if (mappingSet) {
                                JAXRS_LOGGER.moreThanOneServletMapping(servletName, pattern);
                            } else {
                                mappingSet = true;
                                String realPattern = pattern;
                                if (realPattern.endsWith("*")) {
                                    realPattern = realPattern.substring(0, realPattern.length() - 1);
                                }
                                setServletInitParam(servlet, "resteasy.servlet.mapping.prefix", realPattern);
                            }
                        }
                    }
                }
            }
        }
    }


    private void addServlet(JBossWebMetaData webdata, JBossServletMetaData servlet) {
        if (webdata.getServlets() == null) {
            webdata.setServlets(new JBossServletsMetaData());
        }
        webdata.getServlets().add(servlet);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    protected void setFilterInitParam(FilterMetaData filter, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = filter.getInitParam();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            filter.setInitParam(params);
        }
        params.add(param);

    }

    public static ParamValueMetaData findContextParam(JBossWebMetaData webdata, String name) {
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null)
            return null;
        for (ParamValueMetaData param : params) {
            if (param.getParamName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public static ParamValueMetaData findInitParam(JBossWebMetaData webdata, String name) {
        JBossServletsMetaData servlets = webdata.getServlets();
        if (servlets == null)
            return null;
        for (JBossServletMetaData servlet : servlets) {
            List<ParamValueMetaData> initParams = servlet.getInitParam();
            if (initParams != null) {
                for (ParamValueMetaData param : initParams) {
                    if (param.getParamName().equals(name)) {
                        return param;
                    }
                }
            }
        }
        return null;
    }

    public static boolean servletMappingsExist(JBossWebMetaData webdata, String servletName) {
        List<ServletMappingMetaData> mappings = webdata.getServletMappings();
        if (mappings == null)
            return false;
        for (ServletMappingMetaData mapping : mappings) {
            if (mapping.getServletName().equals(servletName)) {
                return true;
            }
        }
        return false;
    }


    public static void setContextParameter(JBossWebMetaData webdata, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }


}
