/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded.ejb3;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.embeddable.EJBContainer;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.embedded.EmbeddedLogger.ROOT_LOGGER;
import static org.jboss.as.embedded.EmbeddedMessages.MESSAGES;

/**
 * Implements JVM ClassPath scanning for EJB JARs as defined
 * by EJB 3.1 Final Draft 22.2.1.  This is a static utility
 * class which is not to be instantiated.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
class ClassPathEjbJarScanner {

    //TODO
    /*
    * This is an intentionally naive implementation which essentially
    * amounts to junkware.  It gets us to the next phases of development,
    * but isn't intended to be the final solution.  For starters it's a static utility.
    *
    * Open issues:
    *
    * 1) Don't load all Classes to look for annotations.  Vie for ASM or Javassist (or
    * other bytecode analyzer).  Or pass through an isolated VDF Deployer chain and let the
    * deployers figure out what the eligible modules are
    * 2) Define a configurable ScheduledExecutorService to back the TempFileProvider
    * used to mount ZIP VFS roots.  If we go the deployer chain route as noted by 1) this
    * won't be necessary
    */

    //-------------------------------------------------------------------------------------||
    // Class Members ----------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * System property key denoting the JVM ClassPath
     */
    private static final String SYS_PROP_KEY_CLASS_PATH = "java.class.path";

    /**
     * Dummy String array used in converting a {@link java.util.Collection} of {@link String} to a typed array
     */
    private static final String[] DUMMY = new String[]{};

    /**
     * Path of the EJB Descriptor, relative to the root of a deployment
     */
    private static final String PATH_EJB_JAR_XML = "META-INF/ejb-jar.xml";

    /**
     * .class Extension
     */
    private static final String EXTENSION_CLASS = ".class";

    /**
     * .jar Extension
     */
    private static final String EXTENSION_JAR = ".jar";

    /**
     * EJB Component-defining annotations
     */
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] EJB_COMPONENT_ANNOTATIONS = (Class<? extends Annotation>[]) new Class<?>[]
            {Stateless.class, Stateful.class, Singleton.class, MessageDriven.class};

    /**
     * {@link java.util.concurrent.ScheduledExecutorService} to mount files to be scanned
     */
    @Deprecated
    //TODO Get some reusable, configurable real SES (as managed by the container) else we'll block on JVM shutdown;
    // this one is never shut down cleanly
    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(Runtime.getRuntime()
            .availableProcessors());

    private static final String JAVA_HOME = getSystemProperty("java.home");

    /**
     * Configured exclusion filters
     * TODO Shouldn't be hardcoded, but available via user configuration
     */
    private static final List<ExclusionFilter> exclusionFilters;

    static {
        exclusionFilters = new ArrayList<ExclusionFilter>();
        exclusionFilters.add(new BundleSymbolicNameExclusionFilter("org.eclipse", "org.junit"));
        // scanning rt.jar leads to out of perm-gen
        exclusionFilters.add(new ExclusionFilter() {
            @Override
            public boolean exclude(VirtualFile file) throws IllegalArgumentException {
                final String pathName = file.getPathName();
                return pathName.startsWith(JAVA_HOME);
            }
        });
    }

    //-------------------------------------------------------------------------------------||
    // Constructor ------------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Internal Constructor, no instances permitted
     */
    private ClassPathEjbJarScanner() {
        throw new UnsupportedOperationException("No instances permitted");
    }

    //-------------------------------------------------------------------------------------||
    // Functional Methods -----------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Obtains all EJB JAR entries from the ClassPath
     */
    public static String[] getEjbJars(Map<?, ?> properties) {

        // Initialize
        final Collection<String> returnValue = new ArrayList<String>();

        // Get the full ClassPath
        String classPath = getSystemProperty("surefire.test.class.path");
        if (classPath == null || classPath.isEmpty())
            classPath = getSystemProperty(SYS_PROP_KEY_CLASS_PATH);
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Class Path: %s", classPath);
        }

        // Split by the path separator character
        final String[] classPathEntries = classPath.split(File.pathSeparator);

        final Object modules;
        if (properties != null) {
            modules = properties.get(EJBContainer.MODULES);
        } else {
            modules = null;
        }
        Set<String> moduleNames = null;
        if (modules != null) {
            if (modules instanceof File[]) {
                for (File file : (File[]) modules) {
                    returnValue.add(file.getAbsolutePath());
                }
                return returnValue.toArray(DUMMY);
            } else if (modules instanceof File) {
                returnValue.add(((File) modules).getAbsolutePath());
                return returnValue.toArray(DUMMY);
            } else if (modules instanceof String[]) {
                moduleNames = new HashSet<String>();
                moduleNames.addAll(Arrays.asList((String[]) modules));
            } else if (modules instanceof String) {
                moduleNames = new HashSet<String>();
                moduleNames.add(modules.toString());
            } else {
                throw MESSAGES.invalidModuleType(EJBContainer.MODULES, modules.getClass());
            }
        }
        // For each CP entry
        for (final String classPathEntry : classPathEntries) {
            // If this is an EJB JAR
            final String moduleName = getEjbJar(classPathEntry);
            if (moduleName != null) {
                if (moduleNames == null) {
                    // Add to be returned
                    returnValue.add(classPathEntry);
                } else if (moduleNames.contains(moduleName)) {
                    returnValue.add(classPathEntry);
                }
            }
        }


        // Return
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("EJB Modules discovered on ClassPath: %s", returnValue);
        }
        return returnValue.toArray(DUMMY);
    }

    private static ClassLoader getTccl() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        return doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static String getSystemProperty(final String property) {
        if (System.getSecurityManager() == null)
            return System.getProperty(property);
        return doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(property);
            }
        });
    }

    //-------------------------------------------------------------------------------------||
    // Internal Helper Methods ------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Determines whether this entry from the ClassPath is an EJB JAR
     *
     * @return The module name, or null if this is not an EJB jar
     */
    private static String getEjbJar(final String candidate) {
        if (candidate == null || candidate.isEmpty())
            return null;

        /*
        * EJB 3.1 22.2.1:
        *
        * A classpath entry is considered a matching entry if it meets one of the following criteria:
        * - It is an ejb-jar according to the standard module-type identification rules defined by the Java
        *   EE platform specification
        * - It is a directory containing a META-INF/ejb-jar.xml file or at least one .class with an enterprise
        *   bean component-defining annotation
        */

        // Represent as VFS so we get a nice unified API
        final VirtualFile file = VFS.getChild(candidate);

        /*
        * See if we've been configured to skip this file
        */
        for (final ExclusionFilter exclusionFilter : exclusionFilters) {
            // If we should exclude this
            if (exclusionFilter.exclude(file)) {
                // Exclude from further processing
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.tracef("%s matched %s for exclusion; skipping", exclusionFilter, file);
                }
                return null;
            }
        }

        Closeable handle = null;
        TempFileProvider provider = null;
        try {

            // If the file exists
            if (file.exists()) {

                // Mount Exploded dir
                if (file.isDirectory()) {
                    handle = VFS.mountReal(file.getPhysicalFile(), file);
                }
                // Mount EJB JAR
                else if (file.getName().endsWith(EXTENSION_JAR)) {
                    provider = TempFileProvider.create("jbossejbmodulescanner", ses, true);
                    handle = VFS.mountZip(file.getPhysicalFile(), file, provider);
                }
                // No conditions met
                else {
                    // So it's obvious if we've got something we didn't properly mount
                    ROOT_LOGGER.skippingUnknownFileType(file);
                    return null;
                }

            }
            // Not a real file
            else {
                ROOT_LOGGER.fileNotFound(file);
                return null;
            }


            /*
            * Directories and real JARs are handled the same way in VFS, so just do
            * one check and skip logic to test isDirectory or not
            */

            // Look for META-INF/ejb-jar.xml
            final VirtualFile ejbJarXml = file.getChild(PATH_EJB_JAR_XML);
            if (ejbJarXml.exists()) {
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.tracef("Found descriptor %s in %s", ejbJarXml.getPathNameRelativeTo(file), file);
                }
                return getModuleNameFromEjbJar(file, ejbJarXml);
            }

            // Look for at least one .class with an EJB annotation
            if (containsEjbComponentClass(file)) {
                return getModuleNameFromFileName(file);
            }

            // Return
            return null;
        } catch (final IOException e) {
            throw MESSAGES.cannotMountFile(e, candidate);
        } finally {
            VFSUtils.safeClose(handle, provider);
        }

    }

    private static String getModuleNameFromEjbJar(final VirtualFile file, final VirtualFile ejbJarXml) {
        //TODO: parse the xml file and get the module name
        return getModuleNameFromFileName(file);
    }

    private static String getModuleNameFromFileName(final VirtualFile file) {
        String moduleName = file.getName();
        int index = moduleName.lastIndexOf('.');
        if (index != -1) {
            return moduleName.substring(0, index - 1);
        }
        return moduleName;
    }

    /**
     * Determines if there is at least one .class in the given file
     * with an EJB component-defining annotation (Stateless, Stateful,
     * Singleton, MessageDriven)
     *
     * @param file
     * @return
     * @deprecated Use a real implementation scanner
     */
    @Deprecated
    private static boolean containsEjbComponentClass(final VirtualFile file) {
        Indexer indexer = new Indexer();
        indexClasses(file, file, indexer);
        Index index = indexer.complete();
        for (Class<? extends Annotation> annotation : EJB_COMPONENT_ANNOTATIONS) {
            final DotName annotationName = DotName.createSimple(annotation.getName());
            final List<AnnotationInstance> classes = index.getAnnotations(annotationName);
            if (classes != null && !classes.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if there is at least one .class in the given file
     * with an EJB component-defining annotation (Stateless, Stateful,
     * Singleton, MessageDriven).
     *
     * @param root The original root from which we started the search
     * @param file
     * @return
     * @deprecated Use a real implementation scanner
     */
    @Deprecated
    private static void indexClasses(final VirtualFile root, final VirtualFile file, Indexer indexer) {

        // Precondition check
        if (file == null) {
            throw MESSAGES.nullVar("file");
        }

        // For all children
        for (final VirtualFile child : file.getChildren()) {
            if (child.isDirectory()) {
                // Determine if there's one in the child
                indexClasses(root, child, indexer);
            }

            // Get the Class for all .class files
            final String childName = child.getPathNameRelativeTo(root);
            if (childName.endsWith(EXTENSION_CLASS)) {
                InputStream stream = null;
                try {
                    try {
                        stream = child.openStream();
                        indexer.index(stream);
                    } catch (IOException e) {
                        ROOT_LOGGER.cannotLoadClassFile(e, child);
                    }
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        ROOT_LOGGER.errorClosingFile(e, child);
                    }
                }
            }
        }
    }
}
