/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

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
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrapClasses;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

/**
 * Processor that finds jax-rs classes in the deployment
 *
 * @author Stuart Douglas
 */
public class JaxrsScanningProcessor implements DeploymentUnitProcessor {

    private static final DotName DECORATOR = DotName.createSimple("javax.decorator.Decorator");

    public static final DotName APPLICATION = DotName.createSimple(Application.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final Map<ModuleIdentifier, ResteasyDeploymentData> deploymentData;
        if (deploymentUnit.getParent() == null) {
            deploymentData = Collections.synchronizedMap(new HashMap<ModuleIdentifier, ResteasyDeploymentData>());
            deploymentUnit.putAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA, deploymentData);
        } else {
            deploymentData = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        }

        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);

        ResteasyDeploymentData resteasyDeploymentData = new ResteasyDeploymentData();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        try {

            if (warMetaData == null) {
                resteasyDeploymentData.setScanAll(true);
                scan(deploymentUnit, module.getClassLoader(), resteasyDeploymentData);
                deploymentData.put(moduleIdentifier, resteasyDeploymentData);
            } else {
                scanWebDeployment(deploymentUnit, warMetaData.getMergedJBossWebMetaData(), module.getClassLoader(), resteasyDeploymentData);
                scan(deploymentUnit, module.getClassLoader(), resteasyDeploymentData);
            }
            deploymentUnit.putAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA, resteasyDeploymentData);
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    public static final Set<String> BOOT_CLASSES = new HashSet<String>();

    static {
        Collections.addAll(BOOT_CLASSES, ResteasyBootstrapClasses.BOOTSTRAP_CLASSES);
    }

    /**
     * If any servlet/filter classes are declared, then we probably don't want to scan.
     */
    protected boolean hasBootClasses(JBossWebMetaData webdata) throws DeploymentUnitProcessingException {
        if (webdata.getServlets() != null) {
            for (ServletMetaData servlet : webdata.getServlets()) {
                String servletClass = servlet.getServletClass();
                if (BOOT_CLASSES.contains(servletClass))
                    return true;
            }
        }
        if (webdata.getFilters() != null) {
            for (FilterMetaData filter : webdata.getFilters()) {
                if (BOOT_CLASSES.contains(filter.getFilterClass()))
                    return true;
            }
        }
        return false;

    }

    protected void scanWebDeployment(final DeploymentUnit du, final JBossWebMetaData webdata, final ClassLoader classLoader, final ResteasyDeploymentData resteasyDeploymentData) throws DeploymentUnitProcessingException {


        // If there is a resteasy boot class in web.xml, then the default should be to not scan
        // make sure this call happens before checkDeclaredApplicationClassAsServlet!!!
        boolean hasBoot = hasBootClasses(webdata);
        resteasyDeploymentData.setBootClasses(hasBoot);

        Class<?> declaredApplicationClass = checkDeclaredApplicationClassAsServlet(webdata, classLoader);
        // Assume that checkDeclaredApplicationClassAsServlet created the dispatcher
        if (declaredApplicationClass != null) {
            resteasyDeploymentData.setDispatcherCreated(true);
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
                if (param.getParamName().equals(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS)) {
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
        final Set<ClassInfo> pathInterfaces = new HashSet<ClassInfo>();
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
                if(info.annotations().containsKey(DECORATOR)) {
                    //we do not add decorators as resources
                    //we can't pick up on programatically added decorators, but that is such an edge case it should not really matter
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

                    if(info.annotations().containsKey(DECORATOR)) {
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

                if(implementor.annotations().containsKey(DECORATOR)) {
                    //we do not add decorators as resources
                    //we can't pick up on programatically added decorators, but that is such an edge case it should not really matter
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
                param.setParamName("javax.ws.rs.Application");
                param.setParamValue(servletClass);
                List<ParamValueMetaData> params = servlet.getInitParam();
                if (params == null) {
                    params = new ArrayList<ParamValueMetaData>();
                    servlet.setInitParam(params);
                }
                params.add(param);

                return clazz;
            }
        }
        return null;
    }
}
