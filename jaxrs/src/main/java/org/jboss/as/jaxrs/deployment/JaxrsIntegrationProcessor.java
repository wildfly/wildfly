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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import org.jboss.as.jaxrs.DeploymentRestResourcesDefintion;
import org.jboss.as.jaxrs.Jackson2Annotations;
import org.jboss.as.jaxrs.JacksonAnnotations;
import org.jboss.as.jaxrs.JaxrsAttribute;
import org.jboss.as.jaxrs.JaxrsConstants;
import org.jboss.as.jaxrs.JaxrsExtension;
import org.jboss.as.jaxrs.JaxrsServerConfig;
import org.jboss.as.jaxrs.JaxrsServerConfigService;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
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

        setConfigParameters(phaseContext, webdata);

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
            StringBuilder buf = null;
            for (String resource : resteasy.getScannedResourceClasses()) {
                if (buf == null) {
                    buf = new StringBuilder();
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
            StringBuilder buf = null;
            for (String provider : resteasy.getScannedProviderClasses()) {
                if (buf == null) {
                    buf = new StringBuilder();
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
            StringBuilder buf = null;
            for (String resource : resteasy.getScannedJndiComponentResources()) {
                if (buf == null) {
                    buf = new StringBuilder();
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

        if (findContextParam(webdata, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB) == null) {
            final String prop = WildFlySecurityManager.getPropertyPrivileged(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, null);
            if (prop != null) {
                setContextParameter(webdata, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, prop);
            } else {
                setContextParameter(webdata, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, Boolean.toString(hasJacksonAnnotations(deploymentUnit)));
            }
        }

        boolean managementAdded = false;
        if (resteasy.getScannedApplicationClasses().size() > 0 || resteasy.hasBootClasses() || resteasy.isDispatcherCreated()) {
            addManagement(deploymentUnit, resteasy);
            managementAdded = true;
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

        if (!managementAdded && webdata.getServletMappings() != null) {
            for (ServletMappingMetaData servletMapMeta: webdata.getServletMappings()) {
                if (JAX_RS_SERVLET_NAME.equals(servletMapMeta.getServletName())) {
                    addManagement(deploymentUnit, resteasy);
                    break;
                }
            }
        }

        // suppress warning for EAR deployments, as we can't easily tell here the app is properly declared
        if (deploymentUnit.getParent() == null && (webdata.getServletMappings() == null || webdata.getServletMappings().isEmpty())) {
            JAXRS_LOGGER.noServletDeclaration(deploymentUnit.getName());
        }
    }


    private void addManagement(DeploymentUnit deploymentUnit, ResteasyDeploymentData resteasy) {
        Set<String> classes = resteasy.getScannedResourceClasses();
        for (String jndiComp : resteasy.getScannedJndiComponentResources()) {
            String[] jndiCompArray = jndiComp.split(";");
            classes.add(jndiCompArray[1]); // REST as EJB is added into jndiComponents
        }
        List<String> rootRestClasses = new ArrayList<>(classes);
        Collections.sort(rootRestClasses);
        for (String componentClass : rootRestClasses) {
            try {
                final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit
                        .getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
                deploymentResourceSupport.getDeploymentSubModel(JaxrsExtension.SUBSYSTEM_NAME,
                        PathElement.pathElement(DeploymentRestResourcesDefintion.REST_RESOURCE_NAME, componentClass));
            } catch (Exception e) {
                JAXRS_LOGGER.failedToRegisterManagementViewForRESTResources(componentClass, e);
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

    private boolean hasJacksonAnnotations(DeploymentUnit deploymentUnit) {
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (Jackson2Annotations a : Jackson2Annotations.values())
        {
            if (checkAnnotation(a.getDotName(), index)) {
                return true;
            }
        }
        for (JacksonAnnotations a : JacksonAnnotations.values())
        {
            if (checkAnnotation(a.getDotName(), index)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAnnotation(DotName name, CompositeIndex index) {
        List<?> list = index.getAnnotations(name);
        if (list != null && !list.isEmpty()) {
            JAXRS_LOGGER.jacksonAnnotationDetected(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB);
            return true;
        }
        return false;
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
        String resteasyName = name;
        if (resteasyName.equals(JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB)) {
            resteasyName = ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB;
        } else {
            resteasyName = resteasyName.replace("-", ".");
        }
        param.setParamName(resteasyName);
        param.setParamValue(value);
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }

    private void setConfigParameters(DeploymentPhaseContext phaseContext, JBossWebMetaData webdata) {

        ServiceRegistry registry = phaseContext.getServiceRegistry();
        ServiceName name = JaxrsServerConfigService.CONFIG_SERVICE;
        @SuppressWarnings("deprecation")
        JaxrsServerConfig config =(JaxrsServerConfig) registry.getRequiredService(name).getValue();

        ModelNode modelNode;
        if (isTransmittable(JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING, modelNode = config.isJaxrs20RequestMatching())) {
            setContextParameter(webdata, JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_ADD_CHARSET, modelNode = config.isResteasyAddCharset())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_ADD_CHARSET, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY, modelNode = config.isResteasyBufferExceptionEntity())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER, modelNode = config.isResteasyDisableHtmlSanitizer())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER, modelNode.asString());
        }
        if (isSubstantiveList(modelNode = config.getResteasyDisableProviders())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_DISABLE_PROVIDERS, convertListToString(modelNode));
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, modelNode = config.isResteasyDocumentExpandEntityReferences())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, modelNode = config.isResteasySecureDisableDTDs())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, modelNode = config.isResteasyDocumentSecureProcessingFeature())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT, modelNode = config.getResteasyGzipMaxInput())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_GZIP_MAX_INPUT, modelNode.asString());
        }
        if (isSubstantiveList(modelNode = config.getResteasyJndiResources())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_JNDI_RESOURCES, convertListToString(modelNode));
        }
        if (isSubstantiveList(modelNode = config.getResteasyLanguageMappings())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS, convertMapToString(modelNode));
        }
        if (isSubstantiveList(modelNode = config.getResteasyMediaTypeMappings())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS, convertMapToString(modelNode));
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, modelNode = config.getResteasyMediaTypeParamMapping())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB, modelNode = config.isResteasyPreferJacksonOverJsonB())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB, modelNode.asString());
        }
        if (isSubstantiveList(modelNode = config.getResteasyProviders())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_PROVIDERS, convertListToString(modelNode));
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS, modelNode = config.isResteasyRFC7232Preconditions())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY, modelNode = config.isResteasyRoleBasedSecurity())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE, modelNode = config.getResteasySecureRandomMaxUse())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS, modelNode = config.isResteasyUseBuiltinProviders())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS, modelNode = config.isResteasyUseContainerFormParams())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS, modelNode.asString());
        }
        if (isTransmittable(JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING, modelNode = config.isResteasyWiderRequestMatching())) {
            setContextParameter(webdata, JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING, modelNode.asString());
        }
    }

    /**
     * Send value to RESTEasy only if it's not null, empty string, or the default value.
     */
    private boolean isTransmittable(AttributeDefinition attribute, ModelNode modelNode) {
        if (modelNode == null || ModelType.UNDEFINED.equals(modelNode.getType())) {
            return false;
        }
        String value = modelNode.asString();
        if ("".equals(value.trim())) {
            return false;
        }
        return !value.equals(attribute.getDefaultValue());
    }

    /**
     * List attributes can be reset to white space, but RESTEasy's ConfigurationBootstrap doesn't handle
     * empty maps appropriately at present.
     */
    private boolean isSubstantiveList(ModelNode modelNode) {
        if (modelNode == null || ModelType.UNDEFINED.equals(modelNode.getType())) {
            return false;
        }
        return modelNode.asList().size() != 0;
    }

    private String convertListToString(ModelNode modelNode) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ModelNode value : modelNode.asList()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(value.asString());
        }
        return sb.toString();
    }

    private String convertMapToString(ModelNode modelNode) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : modelNode.keys()) {
            ModelNode value = modelNode.get(key);
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(key + ":" + value.asString());
        }
        return sb.toString();
    }
}
