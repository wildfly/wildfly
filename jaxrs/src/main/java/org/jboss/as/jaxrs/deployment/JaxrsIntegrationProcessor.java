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
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import org.jboss.as.jaxrs.JaxrsExtension;
import org.wildfly.extension.classchange.ClassChangeAttachments;
import org.wildfly.extension.classchange.ClassChangeListener;
import org.wildfly.extension.classchange.DeploymentClassChangeSupport;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.handlers.ServletRequestContext;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "javax.ws.rs.Application";
    private static final String RESOURCES = "resteasy.scanned.resources";

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

        deploymentUnit.getDeploymentSubsystemModel(JaxrsExtension.SUBSYSTEM_NAME);
        final List<ParamValueMetaData> params = webdata.getContextParams();
        boolean entityExpandEnabled = false;
        if (params != null) {
            Iterator<ParamValueMetaData> it = params.iterator();
            while (it.hasNext()) {
                final ParamValueMetaData param = it.next();
                if(param.getParamName().equals(ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES)) {
                    entityExpandEnabled = true;
                }
            }
        }

        //don't expand entity references by default
        if(!entityExpandEnabled) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES, "false");
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
        Set<Class<? extends Application>> applicationClassSet = new HashSet<>();
        for (Class<? extends Application> clazz : resteasy.getScannedApplicationClasses()) {
            if (clazz.isAnnotationPresent(ApplicationPath.class) || servletMappingsExist(webdata, clazz.getName())) {
                applicationClassSet.add(clazz);
            }
        }

        String servletName = JAX_RS_SERVLET_NAME;
        // add default servlet
        if (applicationClassSet.size() == 0) {
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(JAX_RS_SERVLET_NAME);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            addServlet(webdata, servlet);
            setServletMappingPrefix(webdata, JAX_RS_SERVLET_NAME, servlet);
        } else {

            for (Class<? extends Application> applicationClass : applicationClassSet) {

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

        DeploymentClassChangeSupport classChangeSupport = deploymentUnit.getAttachment(ClassChangeAttachments.DEPLOYMENT_CLASS_CHANGE_SUPPORT);
        if(classChangeSupport != null) {
            final String finalServletName = servletName;
            final AtomicBoolean jaxrsRestartRequired = new AtomicBoolean();
            final Deque<String> additionalResources = new LinkedBlockingDeque<>();
            classChangeSupport.addListener(new ClassChangeListener() {
                @Override
                public void classesReplaced(List<ChangedClasssDefinition> replacedClasses, List<NewClassDefinition> newClassDefinitions) {
//                    for(ChangedClasssDefinition c : replacedClasses) {
//                        if(c.getJavaClass().isAnnotationPresent(Path.class)) {
//                            jaxrsRestartRequired.set(true);
//                        }
//                    }
                    //this is not great, but we can't be sure that any given class is not serialized as part of a JAX-RS
                    //response, so we need to restart to clear any serialization caches
                    for(NewClassDefinition c : newClassDefinitions) {
                        List<AnnotationInstance> path = c.getClassInfo().annotations().get(DotName.createSimple(Path.class.getName()));
                        if(path != null && !path.isEmpty()) {
                            additionalResources.add(c.getName());
                        }
                    }
                    jaxrsRestartRequired.set(true);
                }
            });

            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_INNER_HANDLER_CHAIN_WRAPPERS, new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new HttpHandler() {
                        @Override
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            if(jaxrsRestartRequired.get()) {
                                jaxrsRestartRequired.set(false);
                                ServletRequestContext src = ServletRequestContext.requireCurrent();
                                ManagedServlet managedServlet = src.getDeployment().getServlets().getManagedServlet(finalServletName);
                                String res = src.getCurrentServletContext().getInitParameter(RESOURCES);
                                if(res != null) {
                                    StringBuilder sb = new StringBuilder(res);
                                    String resource = additionalResources.poll();
                                    while (resource != null) {
                                        sb.append(',');
                                        sb.append(resource);
                                        resource = additionalResources.poll();
                                    }
                                    src.getCurrentServletContext().getDeployment().getDeploymentInfo().addInitParameter(RESOURCES, sb.toString());
                                }
                                managedServlet.stop();
                            }
                            handler.handleRequest(exchange);
                        }
                    };
                }
            });
        }

        // suppress warning for EAR deployments, as we can't easily tell here the app is properly declared
        if (deploymentUnit.getParent() == null && (webdata.getServletMappings() == null || webdata.getServletMappings().isEmpty())) {
            JAXRS_LOGGER.noServletDeclaration(deploymentUnit.getName());
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
        //Clear the type cache in jackson databind
        //see https://issues.jboss.org/browse/WFLY-7037
        //see https://github.com/FasterXML/jackson-databind/issues/1363
        //we use reflection to avoid a non optional dependency on jackson
        try {
            Module module = context.getAttachment(Attachments.MODULE);
            Class<?> typeFactoryClass = module.getClassLoader().loadClass("com.fasterxml.jackson.databind.type.TypeFactory");
            Method defaultInstanceMethod = typeFactoryClass.getMethod("defaultInstance");
            Object typeFactory = defaultInstanceMethod.invoke(null);
            Method clearCache = typeFactoryClass.getDeclaredMethod("clearCache");
            clearCache.invoke(typeFactory);
        } catch (Exception e) {
            JAXRS_LOGGER.debugf("Failed to clear class utils LRU map");
        }
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
