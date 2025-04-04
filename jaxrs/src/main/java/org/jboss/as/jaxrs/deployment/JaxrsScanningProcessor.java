/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;
import static org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters.RESTEASY_SCAN;
import static org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS;
import static org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters.RESTEASY_SCAN_RESOURCES;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.Application;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrapClasses;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

/**
 * Processor that finds Jakarta RESTful Web Services classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor implements DeploymentUnitProcessor {

    private static final DotName DECORATOR = DotName.createSimple("jakarta.decorator.Decorator");

    public static final DotName APPLICATION = DotName.createSimple(Application.class.getName());
    private static final String ORG_APACHE_CXF = "org.apache.cxf";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final Map<String, ResteasyDeploymentData> deploymentData;
        if (deploymentUnit.getParent() == null) {
            deploymentData = new ConcurrentHashMap<>();
            deploymentUnit.putAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA, deploymentData);
        } else {
            deploymentData = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        }

        final String moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_NAME);

        ResteasyDeploymentData resteasyDeploymentData = new ResteasyDeploymentData();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        try {

            if (warMetaData == null) {
                resteasyDeploymentData.setScanAll(true);
                scan(deploymentUnit, module.getClassLoader(), resteasyDeploymentData);
                deploymentData.put(moduleIdentifier, resteasyDeploymentData);
            } else {
                scanWebDeployment(warMetaData.getMergedJBossWebMetaData(), module.getClassLoader(), resteasyDeploymentData);
                scan(deploymentUnit, module.getClassLoader(), resteasyDeploymentData);

                // When BootStrap classes are present and no Application subclass declared
                // must check context param for Application subclass declaration
                if (resteasyDeploymentData.getScannedResourceClasses().isEmpty() &&
                    !resteasyDeploymentData.isDispatcherCreated() &&
                    hasBootClasses(warMetaData.getMergedJBossWebMetaData())) {
                    checkOtherParams(deploymentUnit, warMetaData.getMergedJBossWebMetaData(), module.getClassLoader(), resteasyDeploymentData);
                }
            }
            deploymentUnit.putAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA, resteasyDeploymentData);
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void checkOtherParams(final DeploymentUnit du,
                                  final JBossWebMetaData webdata,
                                  final ClassLoader classLoader,
                                  final ResteasyDeploymentData resteasyDeploymentData)
        throws DeploymentUnitProcessingException{

        HashSet<String> appClazzList = new HashSet<>();
        List<ParamValueMetaData> contextParamList = webdata.getContextParams();
        if (contextParamList !=null) {
            for(ParamValueMetaData param: contextParamList) {
                if ("jakarta.ws.rs.core.Application".equals(param.getParamName())) {
                    appClazzList.add(param.getParamValue());
                }
            }
        }

        if (webdata.getServlets() != null) {
            for (ServletMetaData servlet : webdata.getServlets()) {
                List<ParamValueMetaData> initParamList = servlet.getInitParam();
                if (initParamList != null) {
                    for(ParamValueMetaData param: initParamList) {
                        if ("jakarta.ws.rs.core.Application".equals(param.getParamName())) {
                            appClazzList.add(param.getParamValue());
                        }
                    }
                }
            }
        }

        processDeclaredApplicationClasses(du, appClazzList, classLoader, resteasyDeploymentData);
    }

    private void processDeclaredApplicationClasses(final DeploymentUnit du,
                                                   final Set<String> appClazzList,
                                                 final ClassLoader classLoader,
                                                 final ResteasyDeploymentData resteasyDeploymentData)
        throws DeploymentUnitProcessingException {

        final CompositeIndex index = du.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        List<AnnotationInstance> resources = index.getAnnotations(JaxrsAnnotations.PATH.getDotName());
        Map<String, ClassInfo> resourceMap = new HashMap<>(resources.size());
        for (AnnotationInstance a : resources) {
            if (a.target() instanceof ClassInfo) {
                resourceMap.put(((ClassInfo) a.target()).name().toString(),
                        (ClassInfo) a.target());
            }
        }

        for (String clazzName: appClazzList) {
            Class<?> clazz;
            try {
                clazz = classLoader.loadClass(clazzName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }

            if (Application.class.isAssignableFrom(clazz)) {
                try {
                    final Constructor<?> constructor = clazz.getConstructor();
                    final Application appClass = (Application) constructor.newInstance();
                    final Set<Class<?>> resourceClasses = appClass.getClasses() == null ? Set.of() : appClass.getClasses();

                    Set<String> scannedResourceClasses = resteasyDeploymentData.getScannedResourceClasses();
                    for (Class<?> cClazz : resourceClasses) {
                        if (cClazz.isAnnotationPresent(jakarta.ws.rs.Path.class)) {
                            final ClassInfo info = resourceMap.get(cClazz.getName());
                            if (info != null) {
                                if (info.annotationsMap().containsKey(DECORATOR)) {
                                    //we do not add decorators as resources
                                    //we can't pick up on programmatically added decorators, but that is such an edge case it should not really matter
                                    continue;
                                }
                                if (!Modifier.isInterface(info.flags())) {
                                    scannedResourceClasses.add(info.name().toString());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    throw JAXRS_LOGGER.cannotLoadApplicationClass(e);
                }
            }
        }
    }

    private static final Set<String> BOOT_CLASSES = Set.of(ResteasyBootstrapClasses.BOOTSTRAP_CLASSES);

    /**
     * If any servlet/filter classes are declared, then we probably don't want to scan.
     */
    protected boolean hasBootClasses(JBossWebMetaData webdata) {
        if (webdata.getServlets() != null) {
            for (ServletMetaData servlet : webdata.getServlets()) {
                String servletClass = servlet.getServletClass();
                if (servletClass != null && BOOT_CLASSES.contains(servletClass))
                    return true;
            }
        }
        if (webdata.getFilters() != null) {
            for (FilterMetaData filter : webdata.getFilters()) {
                if (filter.getFilterClass() != null && BOOT_CLASSES.contains(filter.getFilterClass()))
                    return true;
            }
        }
        return false;

    }

    protected void scanWebDeployment(final JBossWebMetaData webdata, final ClassLoader classLoader, final ResteasyDeploymentData resteasyDeploymentData) throws DeploymentUnitProcessingException {


        // If there is a resteasy boot class in web.xml, then the default should be to not scan
        // make sure this call happens before checkDeclaredApplicationClassAsServlet!!!
        boolean hasBoot = hasBootClasses(webdata);
        resteasyDeploymentData.setBootClasses(hasBoot);

        Class<?> declaredApplicationClass = checkDeclaredApplicationClassAsServlet(webdata, classLoader);
        // Assume that checkDeclaredApplicationClassAsServlet created the dispatcher
        if (declaredApplicationClass != null) {
            resteasyDeploymentData.setDispatcherCreated(true);

            // Instigate creation of resteasy configuration switches for
            // found provider and resource classes
            resteasyDeploymentData.setScanProviders(true);
            resteasyDeploymentData.setScanResources(true);
        }

        // set scanning on only if there are no boot classes
        if (!hasBoot && !webdata.isMetadataComplete()) {
            resteasyDeploymentData.setScanAll(true);
            resteasyDeploymentData.setScanProviders(true);
            resteasyDeploymentData.setScanResources(true);
        }

        // check resteasy configuration flags

        List<ParamValueMetaData> contextParams = webdata.getContextParams();

        if (contextParams != null) {
            for (ParamValueMetaData param : contextParams) {
                if (param.getParamName().equals(RESTEASY_SCAN)) {
                    resteasyDeploymentData.setScanAll(valueOf(RESTEASY_SCAN, param.getParamValue()));
                } else if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS)) {
                    resteasyDeploymentData.setScanProviders(valueOf(RESTEASY_SCAN_PROVIDERS, param.getParamValue()));
                } else if (param.getParamName().equals(RESTEASY_SCAN_RESOURCES)) {
                    resteasyDeploymentData.setScanResources(valueOf(RESTEASY_SCAN_RESOURCES, param.getParamValue()));
                } else if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS)) {
                    resteasyDeploymentData.setUnwrappedExceptionsParameterSet(true);
                }
            }
        }

    }

    protected void scan(final DeploymentUnit du, final ClassLoader classLoader, final ResteasyDeploymentData resteasyDeploymentData)
            throws DeploymentUnitProcessingException, ModuleLoadException {

        final CompositeIndex index = du.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        if (!resteasyDeploymentData.shouldScan()) {
            return;
        }

        if (!resteasyDeploymentData.isDispatcherCreated()) {
            final Set<ClassInfo> applicationClasses = index.getAllKnownSubclasses(APPLICATION);
            try {
                for (ClassInfo c : applicationClasses) {
                    if (Modifier.isAbstract(c.flags())) continue;
                    @SuppressWarnings("unchecked")
                    Class<? extends Application> scanned = (Class<? extends Application>) classLoader.loadClass(c.name().toString());
                    resteasyDeploymentData.getScannedApplicationClasses().add(scanned);
                }
            } catch (ClassNotFoundException e) {
                throw JaxrsLogger.JAXRS_LOGGER.cannotLoadApplicationClass(e);
            }
        }

        List<AnnotationInstance> resources = null;
        List<AnnotationInstance> providers = null;
        if (resteasyDeploymentData.isScanResources()) {
            resources = index.getAnnotations(JaxrsAnnotations.PATH.getDotName());
        }
        if (resteasyDeploymentData.isScanProviders()) {
            providers = index.getAnnotations(JaxrsAnnotations.PROVIDER.getDotName());
        }

        if ((resources == null || resources.isEmpty()) && (providers == null || providers.isEmpty()))
            return;
        final Set<ClassInfo> pathInterfaces = new HashSet<>();
        if (resources != null) {
            for (AnnotationInstance e : resources) {
                final ClassInfo info;
                if (e.target() instanceof ClassInfo) {
                    info = (ClassInfo) e.target();
                } else if (e.target() instanceof MethodInfo) {
                    //ignore
                    continue;
                } else {
                    JAXRS_LOGGER.classOrMethodAnnotationNotFound("@Path", e.target());
                    continue;
                }
                if(info.name().toString().startsWith(ORG_APACHE_CXF)) {
                    //do not add CXF classes
                    //see WFLY-9752
                    continue;
                }
                if(info.annotationsMap().containsKey(DECORATOR)) {
                    //we do not add decorators as resources
                    //we can't pick up on programmatically added decorators, but that is such an edge case it should not really matter
                    continue;
                }
                if (!Modifier.isInterface(info.flags())) {
                    resteasyDeploymentData.getScannedResourceClasses().add(info.name().toString());
                } else {
                    pathInterfaces.add(info);
                }
            }
        }
        if (providers != null) {
            for (AnnotationInstance e : providers) {
                if (e.target() instanceof ClassInfo) {
                    ClassInfo info = (ClassInfo) e.target();

                    if(info.name().toString().startsWith(ORG_APACHE_CXF)) {
                        //do not add CXF classes
                        //see WFLY-9752
                        continue;
                    }
                    if(info.annotationsMap().containsKey(DECORATOR)) {
                        //we do not add decorators as providers
                        //we can't pick up on programatically added decorators, but that is such an edge case it should not really matter
                        continue;
                    }
                    if (!Modifier.isInterface(info.flags())) {
                        resteasyDeploymentData.getScannedProviderClasses().add(info.name().toString());
                    }
                } else {
                    JAXRS_LOGGER.classAnnotationNotFound("@Provider", e.target());
                }
            }
        }

        // look for all implementations of interfaces annotated @Path
        for (final ClassInfo iface : pathInterfaces) {
            final Set<ClassInfo> implementors = index.getAllKnownImplementors(iface.name());
            for (final ClassInfo implementor : implementors) {
                if(implementor.name().toString().startsWith(ORG_APACHE_CXF)) {
                    //do not add CXF classes
                    //see WFLY-9752
                    continue;
                }

                if(implementor.annotationsMap().containsKey(DECORATOR)) {
                    //we do not add decorators as resources
                    //we can't pick up on programmatically added decorators, but that is such an edge case it should not really matter
                    continue;
                }
                resteasyDeploymentData.getScannedResourceClasses().add(implementor.name().toString());
            }
        }
    }

    protected Class<?> checkDeclaredApplicationClassAsServlet(JBossWebMetaData webData,
                                                              ClassLoader classLoader) throws DeploymentUnitProcessingException {
        if (webData.getServlets() == null)
            return null;

        for (ServletMetaData servlet : webData.getServlets()) {
            String servletClass = servlet.getServletClass();
            if (servletClass == null)
                continue;
            Class<?> clazz = null;
            try {
                clazz = classLoader.loadClass(servletClass);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
            if (Application.class.isAssignableFrom(clazz)) {
                servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
                servlet.setAsyncSupported(true);
                ParamValueMetaData param = new ParamValueMetaData();
                param.setParamName("jakarta.ws.rs.Application");
                param.setParamValue(servletClass);
                List<ParamValueMetaData> params = servlet.getInitParam();
                if (params == null) {
                    params = new ArrayList<>();
                    servlet.setInitParam(params);
                }
                params.add(param);

                return clazz;
            }
        }
        return null;
    }


    private boolean valueOf(String paramName, String value) throws DeploymentUnitProcessingException {
        if (value == null) {
            throw JaxrsLogger.JAXRS_LOGGER.invalidParamValue(paramName, value);
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw JaxrsLogger.JAXRS_LOGGER.invalidParamValue(paramName, value);
        }
    }

}
