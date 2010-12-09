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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Per lib/jar web annotation index.
 *
 * TODO this should be a general deployment thingy ?
 *
 * @author Emanuel Muckenhuber
 */
public class WarAnnotationIndex {

    private final Index rootIndex;

    /** lib/path -> index map. */
    private final Map<String, Index> indexes = new HashMap<String, Index>();

    private WarAnnotationIndex(final Index root) {
        this.rootIndex = root;
    }

    /**
     * Create a web annotation index for a .war.
     *
     * @param deploymentRoot the .war deployment root
     * @return the web annotation index
     * @throws DeploymentUnitProcessingException
     */
    static WarAnnotationIndex create(final VirtualFile deploymentRoot) throws DeploymentUnitProcessingException {
        final WarAnnotationIndex annotationIndex = new WarAnnotationIndex(createRoot(deploymentRoot));
        processLibs(deploymentRoot, annotationIndex.indexes);
        return annotationIndex;
    }

    /**
     * @return the rootIndex
     */
    public Index getRootIndex() {
        return rootIndex;
    }

    /**
     * Get all paths names.
     *
     * @return the path names
     */
    public Collection<String> getPathNames() {
        return this.indexes.keySet();
    }

    /**
     * Get an index.
     *
     * @param pathName the path name.
     * @return
     */
    public Index getIndex(String pathName) {
        if(pathName == null || pathName.equals("")) {
            return getRootIndex();
        }
        return indexes.get(pathName);
    }

    /**
     * Obtain a composite list of targets for the specified annotation.
     *
     * @param annotationName the name of the annotation to look for
     * @return a non-null list of annotation targets
     */
    public List<AnnotationInstance> getAnnotations(DotName annotationName) {
        final List<AnnotationInstance> list = new ArrayList<AnnotationInstance>();
        if(rootIndex != null) {
            list.addAll(rootIndex.getAnnotations(annotationName));
        }
        for(final Index index : indexes.values()) {
            list.addAll(index.getAnnotations(annotationName));
        }
        return list;
    }

    /**
     * Gets composite list of all known subclasses of the specified class name.
     *
     * @param className the super class of the desired subclasses
     * @return a non-null list of all known subclasses of className
     */
    public List<ClassInfo> getKnownSubclasses(DotName className) {
        final List<ClassInfo> list = new ArrayList<ClassInfo>();
        if(rootIndex != null) {
            list.addAll(rootIndex.getKnownSubclasses(className));
        }
        for(final Index index : indexes.values()) {
            list.addAll(index.getKnownSubclasses(className));
        }
        return list;
    }

    /**
     * Get a ClassInfo.
     *
     * @param className the name of the class
     * @return information about the class or null if it is not known
     */
    public ClassInfo getClassByName(DotName className) {
        ClassInfo info = null;
        if(rootIndex != null) {
            info = rootIndex.getClassByName(className);
        }
        if(info == null) {
            for(final Index index : indexes.values()) {
                info = index.getClassByName(className);
                if(info != null) {
                    return info;
                }
            }
        }
        return info;
    }

    /**
     * Gets all known classes by this composite index.
     *
     * @return a collection of known classes
     */
    public Collection<ClassInfo> getKnownClasses() {
        final List<ClassInfo> list = new ArrayList<ClassInfo>();
        if(rootIndex != null) {
            list.addAll(rootIndex.getKnownClasses());
        }
        for(final Index index : indexes.values()) {
            list.addAll(index.getKnownClasses());
        }
        return list;
    }

    /**
     * Create an index for each jar in the WEB-INF/lib directory.
     *
     * @param deploymentRoot the deployment root
     * @param indexes the indexes map
     * @throws DeploymentUnitProcessingException
     */
    static void processLibs(final VirtualFile deploymentRoot, Map<String, Index> indexes) throws DeploymentUnitProcessingException {
        final VirtualFile webInfLib = deploymentRoot.getChild(WarStructureDeploymentProcessor.WEB_INF_LIB);
        if(webInfLib.exists()) {
            try {
                final List<VirtualFile> archives = webInfLib.getChildren(WarStructureDeploymentProcessor.DEFAULT_WEB_INF_LIB_FILTER);
                for(final VirtualFile archive : archives) {
                    final Indexer indexer = new Indexer();
                    final List<VirtualFile> classChildren = archive.getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
                    for(VirtualFile classFile : classChildren) {
                        InputStream inputStream = null;
                        try {
                            inputStream = classFile.openStream();
                            indexer.index(inputStream);
                        } finally {
                            VFSUtils.safeClose(inputStream);
                        }
                    }
                    indexes.put(archive.getName(), indexer.complete());
                }
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to index deployment root for annotations");
            }
        }
    }

    /**
     * Create an annotation index for WEB-INF/classes.
     *
     * @param deploymentRoot the deployment root
     * @return the root index
     * @throws DeploymentUnitProcessingException
     */
    static Index createRoot(final VirtualFile deploymentRoot) throws DeploymentUnitProcessingException {
        final VirtualFile classes = deploymentRoot.getChild(WarStructureDeploymentProcessor.WEB_INF_CLASSES);
        if(classes.exists()) {
            final Indexer indexer = new Indexer();
            try {
                final List<VirtualFile> classChildren = classes.getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
                for(VirtualFile classFile : classChildren) {
                    InputStream inputStream = null;
                    try {
                        inputStream = classFile.openStream();
                        indexer.index(inputStream);
                    } finally {
                        VFSUtils.safeClose(inputStream);
                    }
                }
                return indexer.complete();
            } catch(Throwable t) {
                throw new DeploymentUnitProcessingException("Failed to index deployment root for annotations", t);
            }
        }
        return null;
    }

}
