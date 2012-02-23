/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.web.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.vfs.VirtualFile;

/**
 * SCI deployment processor.
 *
 * @author Emanuel Muckenhuber
 * @author Remy Maucherat
 */
public class ServletContainerInitializerDeploymentProcessor implements DeploymentUnitProcessor {

    /**
     * Process SCIs.
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ServiceModuleLoader loader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for " + deploymentUnit);
        }
        final ClassLoader classLoader = module.getClassLoader();
        ScisMetaData scisMetaData = deploymentUnit.getAttachment(ScisMetaData.ATTACHMENT_KEY);
        if (scisMetaData == null) {
            scisMetaData = new ScisMetaData();
            deploymentUnit.putAttachment(ScisMetaData.ATTACHMENT_KEY, scisMetaData);
        }
        Set<ServletContainerInitializer> scis = scisMetaData.getScis();
        if (scis == null) {
            scis = new HashSet<ServletContainerInitializer>();
            scisMetaData.setScis(scis);
        }
        Map<ServletContainerInitializer, Set<Class<?>>> handlesTypes = scisMetaData.getHandlesTypes();
        if (handlesTypes == null) {
            handlesTypes = new HashMap<ServletContainerInitializer, Set<Class<?>>>();
            scisMetaData.setHandlesTypes(handlesTypes);
        }
        // Find the SCIs from shared modules
        for (ModuleDependency dependency : moduleSpecification.getSystemDependencies()) {
            ServiceLoader<ServletContainerInitializer> serviceLoader;
            try {
                Module depModule = loader.loadModule(dependency.getIdentifier());
                serviceLoader = depModule.loadService(ServletContainerInitializer.class);
                for (ServletContainerInitializer service : serviceLoader) {
                    scis.add(service);
                }
            } catch (ModuleLoadException e) {
                throw new DeploymentUnitProcessingException("Error loading SCI from module: " + dependency.getIdentifier(), e);
            }
        }
        // Find local ServletContainerInitializer services
        List<String> order = warMetaData.getOrder();
        Map<String, VirtualFile> localScis = warMetaData.getScis();
        if (order != null && localScis != null) {
            for (String jar : order) {
                VirtualFile sci = localScis.get(jar);
                if (sci != null) {
                    ServletContainerInitializer service = loadSci(classLoader, sci, jar, true);
                    if (service != null) {
                        scis.add(service);
                    }
                }
            }
        }
        // Process HandlesTypes for ServletContainerInitializer
        Map<Class<?>, Set<ServletContainerInitializer>> typesMap = new HashMap<Class<?>, Set<ServletContainerInitializer>>();
        for (ServletContainerInitializer service : scis) {
            if (service.getClass().isAnnotationPresent(HandlesTypes.class)) {
                HandlesTypes handlesTypesAnnotation = service.getClass().getAnnotation(HandlesTypes.class);
                Class<?>[] typesArray = handlesTypesAnnotation.value();
                if (typesArray != null) {
                    for (Class<?> type : typesArray) {
                        Set<ServletContainerInitializer> servicesSet = typesMap.get(type);
                        if (servicesSet == null) {
                            servicesSet = new HashSet<ServletContainerInitializer>();
                            typesMap.put(type, servicesSet);
                        }
                        servicesSet.add(service);
                        handlesTypes.put(service, new HashSet<Class<?>>());
                    }
                }
            }
        }
        Class<?>[] typesArray = typesMap.keySet().toArray(new Class<?>[0]);

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(index == null) {
            throw new DeploymentUnitProcessingException("Unable to resolve annotation index for deployment unit " + deploymentUnit);
        }

        // Find classes which extend, implement, or are annotated by HandlesTypes
        for (Class<?> type : typesArray) {
            DotName className = DotName.createSimple(type.getName());
            Set<ClassInfo> classInfos = processHandlesType(className, type, index);
            Set<Class<?>> classes = loadClassInfoSet(classInfos, classLoader);
            Set<ServletContainerInitializer> sciSet = typesMap.get(type);
            for (ServletContainerInitializer sci : sciSet) {
                handlesTypes.get(sci).addAll(classes);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    private ServletContainerInitializer loadSci(ClassLoader classLoader, VirtualFile sci, String jar, boolean error) throws DeploymentUnitProcessingException {
        ServletContainerInitializer service = null;
        InputStream is = null;
        try {
            // Get the ServletContainerInitializer class name
            is = sci.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String servletContainerInitializerClassName = reader.readLine();
            int pos = servletContainerInitializerClassName.indexOf('#');
            if (pos > 0) {
                servletContainerInitializerClassName = servletContainerInitializerClassName.substring(0, pos);
            }
            servletContainerInitializerClassName = servletContainerInitializerClassName.trim();
            // Instantiate the ServletContainerInitializer
            service = (ServletContainerInitializer) classLoader.loadClass(servletContainerInitializerClassName).newInstance();
        } catch (Exception e) {
            if (error) {
                throw new DeploymentUnitProcessingException("Deployment error processing SCI for JAR: " + jar, e);
            } else {
                Logger.getLogger("org.jboss.web").info("Skipped SCI for JAR: " + jar, e);
            }
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        return service;
    }

    private Set<ClassInfo> processHandlesType(DotName typeName, Class<?> type, CompositeIndex index) throws DeploymentUnitProcessingException {
        Set<ClassInfo> classes = new HashSet<ClassInfo>();
        if (type.isAnnotation()) {
            List<AnnotationInstance> instances = index.getAnnotations(typeName);
            for (AnnotationInstance instance : instances) {
                AnnotationTarget annotationTarget = instance.target();
                if (annotationTarget instanceof ClassInfo) {
                    classes.add((ClassInfo) annotationTarget);
                } else if (annotationTarget instanceof FieldInfo) {
                    classes.add(((FieldInfo) annotationTarget).declaringClass());
                } else if (annotationTarget instanceof MethodInfo) {
                    classes.add(((MethodInfo) annotationTarget).declaringClass());
                } else if (annotationTarget instanceof MethodParameterInfo) {
                    classes.add(((MethodParameterInfo) annotationTarget).method().declaringClass());
                }
            }
        } else {
            classes.addAll(index.getAllKnownSubclasses(typeName));
            classes.addAll(index.getAllKnownImplementors(typeName));
        }
        return classes;
    }

    private Set<Class<?>> loadClassInfoSet(Set<ClassInfo> classInfos, ClassLoader classLoader) throws DeploymentUnitProcessingException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (ClassInfo classInfo : classInfos) {
            Class<?> type = null;
            try {
                type = classLoader.loadClass(classInfo.name().toString());
                classes.add(type);
            } catch (Exception e) {
                Logger.getLogger("org.jboss.web").info("Could not load class designated by HandlesTypes [" + classInfo + "]: " + e.getMessage());
            }
        }
        return classes;
    }
}
