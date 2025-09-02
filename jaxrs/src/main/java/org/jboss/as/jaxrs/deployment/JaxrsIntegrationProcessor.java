/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.DeploymentRestResourcesDefintion;
import org.jboss.as.jaxrs.Jackson2Annotations;
import org.jboss.as.jaxrs.JaxrsExtension;
import org.jboss.as.jaxrs.JaxrsServerConfig;
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
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.wildfly.security.manager.WildFlySecurityManager;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final String JAX_RS_SERVLET_NAME = "jakarta.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "jakarta.ws.rs.Application";

    private final JaxrsServerConfig contextConfiguration;

    public JaxrsIntegrationProcessor(final JaxrsServerConfig contextConfiguration) {
        this.contextConfiguration = contextConfiguration;
    }

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

        // Add the context parameters
        contextConfiguration.getContextParameters().forEach((key, value) -> setContextParameter(webdata, key, value));

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);

        if (resteasy == null)
            return;

        // Set up the configuration factory
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module != null) {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            final WildFlyConfigurationFactory configurationFactory = WildFlyConfigurationFactory.getInstance();
            configurationFactory.register(module.getClassLoader(), useMicroProfileConfig(module, support));
        }

        final List<ParamValueMetaData> params = webdata.getContextParams();
        boolean entityExpandEnabled = false;
        if (params != null) {
            for (final ParamValueMetaData param : params) {
                if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES)) {
                    entityExpandEnabled = true;
                }
            }
        }

        //don't expand entity references by default
        if(!entityExpandEnabled) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES, "false");
        }

        final Map<String, ResteasyDeploymentData> attachmentMap = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        final List<ResteasyDeploymentData> additionalData = new ArrayList<>();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec != null && attachmentMap != null) {
            final Set<String> identifiers = new HashSet<>();
            for (ModuleDependency dep : moduleSpec.getAllDependencies()) {
                //make sure we don't double up
                if (!identifiers.contains(dep.getDependencyModule())) {
                    identifiers.add(dep.getDependencyModule());
                    if (attachmentMap.containsKey(dep.getDependencyModule())) {
                        additionalData.add(attachmentMap.get(dep.getDependencyModule()));
                    }
                }
            }
            resteasy.merge(additionalData);
        }
        if (!resteasy.getScannedResourceClasses().isEmpty()) {
            final String resources = String.join(",", resteasy.getScannedResourceClasses());
            JAXRS_LOGGER.debugf("Adding Jakarta RESTful Web Services resource classes: %s", resources);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, resources);
        }
        if (!resteasy.getScannedProviderClasses().isEmpty()) {
            final String providers = String.join(",", resteasy.getScannedProviderClasses());
            JAXRS_LOGGER.debugf("Adding Jakarta RESTful Web Services provider classes: %s", providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, providers);
        }

        if (!resteasy.getScannedJndiComponentResources().isEmpty()) {
            final String providers = String.join(",", resteasy.getScannedJndiComponentResources());
            JAXRS_LOGGER.debugf("Adding Jakarta RESTful Web Services jndi component resource classes: %s", providers);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
        }

        if (!resteasy.isUnwrappedExceptionsParameterSet()) {
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS, "jakarta.ejb.EJBException");
        }

        if (findContextParam(webdata, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB) == null) {
            final String prop = WildFlySecurityManager.getPropertyPrivileged(ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB, null);
            setContextParameter(webdata, ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB,
                    Objects.requireNonNullElseGet(prop, () -> Boolean.toString(hasJacksonAnnotations(deploymentUnit))));
        }

        boolean managementAdded = false;
        if (!resteasy.getScannedApplicationClasses().isEmpty() || resteasy.hasBootClasses() || resteasy.isDispatcherCreated()) {
            addManagement(deploymentUnit, resteasy);
            managementAdded = true;
        }

        // Check for the existence of the "resteasy.server.tracing.type" context parameter. If set to ALL or ON_DEMAND
        // log a warning message.
        final String value = webdata.getContextParams().stream()
                .filter(contextValue -> "resteasy.server.tracing.type".equals(contextValue.getParamName()))
                .map(ParamValueMetaData::getParamValue)
                .findFirst()
                .orElse(null);
        if (value != null && !"OFF".equals(value)) {
            JAXRS_LOGGER.tracingEnabled(deploymentUnit.getName());
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
        if (applicationClassSet.isEmpty()) {
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(JAX_RS_SERVLET_NAME);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            addServlet(webdata, servlet);
            setServletMappingPrefix(webdata, JAX_RS_SERVLET_NAME, servlet);
        } else {

            for (Class<? extends Application> applicationClass : applicationClassSet) {
                String servletName;

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
                    //no mappings, add our own
                    List<String> patterns = new ArrayList<>();
                    //for some reason the spec requires this to be decoded
                    String pathValue = URLDecoder.decode(applicationClass.getAnnotation(ApplicationPath.class)
                            .value()
                            .trim(), StandardCharsets.UTF_8);
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
                        webdata.setServletMappings(new ArrayList<>());
                    }
                    webdata.getServletMappings().add(mapping);
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
            classes.add(jndiCompArray[1]); // REST as Jakarta Enterprise Beans are added into jndiComponents
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
            params = new ArrayList<>();
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
                if (mapping.getServletName().equals(servletName)
                        && mapping.getUrlPatterns() != null) {
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
        Module module = null;
        try {
            module = context.getAttachment(Attachments.MODULE);
            Class<?> typeFactoryClass = module.getClassLoader().loadClass("com.fasterxml.jackson.databind.type.TypeFactory");
            Method defaultInstanceMethod = typeFactoryClass.getMethod("defaultInstance");
            Object typeFactory = defaultInstanceMethod.invoke(null);
            Method clearCache = typeFactoryClass.getDeclaredMethod("clearCache");
            clearCache.invoke(typeFactory);
        } catch (Exception e) {
            JAXRS_LOGGER.debugf("Failed to clear class utils LRU map");
        } finally {
            // Remove the deployment from the registered configuration factory
            if (module != null && JaxrsDeploymentMarker.isJaxrsDeployment(context)) {
                WildFlyConfigurationFactory.getInstance().unregister(module.getClassLoader());
            }
        }
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
            params = new ArrayList<>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }

    private static boolean useMicroProfileConfig(final Module module, final CapabilityServiceSupport support) {
        final boolean configSupported = support.hasCapability("org.wildfly.microprofile.config");
        if (configSupported) {
            // Can we load the org.jboss.resteasy.microprofile.config module? If so we can use the MicroProfile backed
            // configuration in RESTEasy. Otherwise, we need to use the default.
            try {
                module.getModule("org.jboss.resteasy.microprofile.config");
                return true;
            } catch (ModuleLoadException ignore) {
            }
        }
        return false;
    }
}
