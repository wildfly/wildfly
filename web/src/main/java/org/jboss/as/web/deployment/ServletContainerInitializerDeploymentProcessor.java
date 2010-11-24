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

import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
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
    public void processDeployment(final DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if(!isWarDeployment(context)) {
            return; // Skip non web deployments
        }
        final WarAnnotationIndex index = context.getAttachment(WarAnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }
        WarMetaData warMetaData = context.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + context.getName());
        }
        final ClassLoader classLoader = module.getClassLoader();
        ScisMetaData scisMetaData = context.getAttachment(ScisMetaData.ATTACHMENT_KEY);
        if (scisMetaData == null) {
            scisMetaData = new ScisMetaData();
            context.putAttachment(ScisMetaData.ATTACHMENT_KEY, scisMetaData);
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
        // FIXME: for now, look in all dependencies for SCIs
        for (ModuleConfig.Dependency dependency : ModuleDependencies.getAttachedDependencies(context).getDependencies()) {
            ServiceLoader<ServletContainerInitializer> serviceLoader;
            try {
                serviceLoader = Module.loadServiceFromCurrent(dependency.getIdentifier(), ServletContainerInitializer.class);
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
        // Find classes which extend, implement, or are annotated by HandlesTypes
        for (Class<?> type : typesArray) {
            DotName className = DotName.createSimple(type.getName());
            if (index.getRootIndex() != null) {
                Set<ClassInfo> classInfos = processHandlesType(className, type, index.getRootIndex());
                Set<Class<?>> classes = loadClassInfoSet(classInfos, classLoader);
                Set<ServletContainerInitializer> sciSet = typesMap.get(type);
                for (ServletContainerInitializer sci : sciSet) {
                   handlesTypes.get(sci).addAll(classes);
                }
            }
            for (final String pathName : index.getPathNames()) {
                final Index jarIndex = index.getIndex(pathName);
                Set<ClassInfo> classInfos = processHandlesType(className, type, jarIndex);
                Set<Class<?>> classes = loadClassInfoSet(classInfos, classLoader);
                Set<ServletContainerInitializer> sciSet = typesMap.get(type);
                for (ServletContainerInitializer sci : sciSet) {
                    handlesTypes.get(sci).addAll(classes);
                }
            }
        }
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

    private Set<ClassInfo> processHandlesType(DotName typeName, Class<?> type, Index index) throws DeploymentUnitProcessingException {
        Set<ClassInfo> classes = new HashSet<ClassInfo>();
        if (type.isAnnotation()) {
            List<AnnotationTarget> annotationTargets = index.getAnnotationTargets(typeName);
            for (AnnotationTarget annotationTarget : annotationTargets) {
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
            classes.addAll(index.getKnownSubclasses(typeName));
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
