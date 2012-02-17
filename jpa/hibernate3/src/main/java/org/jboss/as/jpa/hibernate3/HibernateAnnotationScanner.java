/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate3;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.Scanner;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

/**
 * Annotation scanner for Hibernate
 *
 * @author Scott Marlow (forked from Ales Justin's ScannerImpl in AS6)
 */
public class HibernateAnnotationScanner implements Scanner {

    private static final ThreadLocal<PersistenceUnitMetadata> persistenceUnitMetadataTLS = new ThreadLocal<PersistenceUnitMetadata>();

    /** Caches, used when restarting the persistence unit service */
    private static final Map<PersistenceUnitMetadata, Map<URL, Set<Package>>> PACKAGES_IN_JAR_CACHE = new HashMap<PersistenceUnitMetadata, Map<URL,Set<Package>>>();
    private static final Map<PersistenceUnitMetadata, Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>> CLASSES_IN_JAR_CACHE = new HashMap<PersistenceUnitMetadata, Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>>();

    public static void setThreadLocalPersistenceUnitMetadata(final PersistenceUnitMetadata pu) {
        persistenceUnitMetadataTLS.set(pu);
    }

    public static void clearThreadLocalPersistenceUnitMetadata() {
        persistenceUnitMetadataTLS.remove();
    }


    private static void cachePackages(PersistenceUnitMetadata pu, URL jarToScan, Set<Package> packages) {
        synchronized (PACKAGES_IN_JAR_CACHE) {
            Map<URL, Set<Package>> packagesByUrl = PACKAGES_IN_JAR_CACHE.get(pu);
            if (packagesByUrl == null) {
                packagesByUrl = new HashMap<URL, Set<Package>>();
                PACKAGES_IN_JAR_CACHE.put(pu, packagesByUrl);
            }
            packagesByUrl.put(jarToScan, packages);
        }
    }

    private static Set<Package> getCachedPackages(PersistenceUnitMetadata pu, URL jarToScan){
        synchronized (PACKAGES_IN_JAR_CACHE) {
            Map<URL, Set<Package>> packagesByUrl = PACKAGES_IN_JAR_CACHE.get(pu);
            if (packagesByUrl == null) {
                return Collections.emptySet();
            }
            Set<Package> packages = packagesByUrl.get(jarToScan);
            if (packages == null) {
                return Collections.emptySet();
            }
            return packages;

        }
    }

    private static void cacheClasses(PersistenceUnitMetadata pu, URL jarToScan, Class<? extends Annotation> annotation, Set<Class<?>> classes){
        synchronized (CLASSES_IN_JAR_CACHE) {
            Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>> classesByURL = CLASSES_IN_JAR_CACHE.get(pu);
            if (classesByURL == null) {
                classesByURL = new HashMap<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>();
                CLASSES_IN_JAR_CACHE.put(pu, classesByURL);
            }
            Map<Class<? extends Annotation>, Set<Class<?>>> classesByAnnotation = classesByURL.get(jarToScan);
            if (classesByAnnotation == null) {
                classesByAnnotation = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();
                classesByURL.put(jarToScan, classesByAnnotation);
            }
            classesByAnnotation.put(annotation, classes);
        }

    }

    private static Set<Class<?>> getCachedClasses(PersistenceUnitMetadata pu, URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor){
        synchronized (CLASSES_IN_JAR_CACHE) {
            Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>> classesByURL = CLASSES_IN_JAR_CACHE.get(pu);
            if (classesByURL == null) {
                return Collections.emptySet();
            }
            Map<Class<? extends Annotation>, Set<Class<?>>> classesByAnnotation = classesByURL.get(jartoScan);
            if (classesByAnnotation == null) {
                return Collections.emptySet();
            }
            Set<Class<?>> classes = new HashSet<Class<?>>();
            for (Class<? extends Annotation> ann : annotationsToLookFor) {
                Set<Class<?>> classesForAnnotation = classesByAnnotation.get(ann);
                if (classesForAnnotation != null) {
                    classes.addAll(classesForAnnotation);
                }
            }
            return classes;
        }
    }

    static void cleanup(PersistenceUnitMetadata pu) {
        synchronized (CLASSES_IN_JAR_CACHE) {
            CLASSES_IN_JAR_CACHE.remove(pu);
        }
        synchronized (PACKAGES_IN_JAR_CACHE) {
            PACKAGES_IN_JAR_CACHE.remove(pu);
        }
    }

    @Override
    public Set<Package> getPackagesInJar(URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
        if (jarToScan == null) {
            throw MESSAGES.nullVar("jarToScan");
        }
        JPA_LOGGER.tracef("getPackagesInJar url=%s annotations=%s", jarToScan.getPath(), annotationsToLookFor);
        Set<Class<?>> resultClasses = new HashSet<Class<?>>();

        PersistenceUnitMetadata pu = persistenceUnitMetadataTLS.get();
        if (pu == null) {
            throw MESSAGES.missingPersistenceUnitMetadata();
        }

        if (annotationsToLookFor.size() > 0) {  // Hibernate doesn't pass any annotations currently
            resultClasses = getClassesInJar(jarToScan, annotationsToLookFor);
        } else {
            if (pu.getAnnotationIndex() != null) {
                Index index = getJarFileIndex(jarToScan, pu);
                if (index == null) {
                    JPA_LOGGER.tracef("No classes to scan for annotations in jar '%s' (jars with classes '%s')",
                        jarToScan, pu.getAnnotationIndex().keySet());
                    return new HashSet<Package>();
                }
                Collection<ClassInfo> allClasses = index.getKnownClasses();
                for (ClassInfo classInfo : allClasses) {
                    String className = classInfo.name().toString();
                    try {
                        resultClasses.add(pu.getClassLoader().loadClass(className));
                        // TODO:  fix temp classloader (get CFNE on entity class)
                        //result.add(pu.getNewTempClassLoader().loadClass(className));
                    } catch (ClassNotFoundException e) {
                        throw MESSAGES.cannotLoadEntityClass(e, className);
                    }
                }
            }
        }

        if (pu.getAnnotationIndex() != null || annotationsToLookFor.size() > 0) {
            Map<String, Package> uniquePackages = new HashMap<String, Package>();
            for (Class<?> classWithAnnotation : resultClasses) {
                Package classPackage = classWithAnnotation.getPackage();
                if (classPackage != null) {
                    JPA_LOGGER.tracef("getPackagesInJar found package %s", classPackage);
                    uniquePackages.put(classPackage.getName(), classPackage);
                }
            }
            Set<Package> packages = new HashSet<Package>(uniquePackages.values());
            cachePackages(pu, jarToScan, packages);
            return new HashSet<Package>(packages);
        } else {
            return getCachedPackages(pu, jarToScan);
        }
    }

    private Index getJarFileIndex(final URL jartoScan, final PersistenceUnitMetadata pu) {
        return pu.getAnnotationIndex().get(jartoScan);
    }

    @Override
    public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
        if (jartoScan == null) {
            throw MESSAGES.nullVar("jartoScan");
        }
        JPA_LOGGER.tracef("getClassesInJar url=%s annotations=%s", jartoScan.getPath(), annotationsToLookFor);
        PersistenceUnitMetadata pu = persistenceUnitMetadataTLS.get();
        if (pu == null) {
            throw MESSAGES.missingPersistenceUnitMetadata();
        }
        if (pu.getAnnotationIndex() != null) {
            Index index = getJarFileIndex(jartoScan, pu);
            if (index == null) {
                JPA_LOGGER.tracef("No classes to scan for annotations in jar '%s' (jars with classes '%s')",
                    jartoScan, pu.getAnnotationIndex().keySet());
                return new HashSet<Class<?>>();
            }
            if (annotationsToLookFor == null) {
                throw MESSAGES.nullVar("annotationsToLookFor");
            }
            if (annotationsToLookFor.size() == 0) {
                throw MESSAGES.emptyParameter("annotationsToLookFor");
            }

            Set<Class<?>> result = new HashSet<Class<?>>();

            for (Class<? extends Annotation> annClass : annotationsToLookFor) {
                DotName annotation = DotName.createSimple(annClass.getName());
                List<AnnotationInstance> classesWithAnnotation = index.getAnnotations(annotation);
                Set<Class<?>> classesForAnnotation = new HashSet<Class<?>>();
                for (AnnotationInstance annotationInstance : classesWithAnnotation) {
                    // verify that the annotation target is actually a class, since some frameworks
                    // may generate bytecode with annotations placed on methods (see AS7-2559)
                    if (annotationInstance.target() instanceof ClassInfo) {
                        String className = annotationInstance.target().toString();
                        try {
                            JPA_LOGGER.tracef("getClassesInJar found class %s with annotation %s", className, annClass.getName());
                            Class<?> clazz = pu.getClassLoader().loadClass(className);
                            result.add(clazz);
                            classesForAnnotation.add(clazz);
                            // TODO:  fix temp classloader (get CFNE on entity class)
                            //result.add(pu.getNewTempClassLoader().loadClass(className));
                        } catch (ClassNotFoundException e) {
                            throw MESSAGES.cannotLoadEntityClass(e, className);
                        }
                    }
                }
                cacheClasses(pu, jartoScan, annClass, classesForAnnotation);
            }
            return result;
        } else {
            return getCachedClasses(pu, jartoScan, annotationsToLookFor);
        }
    }

    @Override
    public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns) {
        if (jartoScan == null)
            throw MESSAGES.nullVar("jartoScann");
        if (filePatterns == null)
            throw MESSAGES.nullVar("filePatterns");

        Set<NamedInputStream> result = new HashSet<NamedInputStream>();
        Map<String, Set<NamedInputStream>> map;
        map = new HashMap<String, Set<NamedInputStream>>();
        findFiles(jartoScan, filePatterns, map, result);
        return result;
    }

    private void findFiles(URL jartoScan, Set<String> filePatterns, Map<String, Set<NamedInputStream>> map, Set<NamedInputStream> result) {
        if (filePatterns.isEmpty()) {
            for (Set<NamedInputStream> nims : map.values())
                result.addAll(nims);
        } else {
            VirtualFile root = null;
            for (String pattern : filePatterns) {
                Set<NamedInputStream> niss = map.get(pattern);
                if (niss == null) {
                    if (root == null)
                        root = getFile(jartoScan);

                    try {
                        List<VirtualFile> children = root.getChildrenRecursively(new HibernatePatternFilter(pattern));
                        niss = toNIS(children);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (niss != null)
                    result.addAll(niss);
            }
        }
    }

    private Set<NamedInputStream> toNIS(Iterable<VirtualFile> files) {
        Set<NamedInputStream> result = new HashSet<NamedInputStream>();
        for (VirtualFile file : files) {
            NamedInputStream nis = new HibernateVirtualFileNamedInputStream(file);
            result.add(nis);
        }
        return result;
    }

    @Override
    public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns) {
        throw MESSAGES.notYetImplemented();  // not currently called
    }

    @Override
    public String getUnqualifiedJarName(URL jarUrl) {
        VirtualFile file = getFile(jarUrl);
        return file.getName();
    }

    private VirtualFile getFile(URL url) {
        try {
            return VFS.getChild(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
