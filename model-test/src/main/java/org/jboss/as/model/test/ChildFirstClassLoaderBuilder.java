/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.jboss.modules.filter.ClassFilter;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoaderBuilder {

    /** Use this property on the lightning runs to make sure that people have set the root and cache properties */
    private static final String STRICT_PROPERTY = "org.jboss.model.test.cache.strict";

    /** Either the name of a parent directory e.g. "jboss-as", or a list of directories/files known to exist within that directory e.g. "[pom.xml, testsuite]"*/
    private static final String ROOT_PROPERTY = "org.jboss.model.test.cache.root";

    /** The relative location of the cache directory to the directory indicated by {@link #ROOT_PROPERTY} */
    private static final String CACHE_FOLDER_PROPERTY = "org.jboss.model.test.classpath.cache";

    /** A comma separated list of maven repository urls. If not set it will use http://repository.jboss.org/nexus/content/groups/developer/ */
    static final String MAVEN_REPOSITORY_URLS = "org.jboss.model.test.maven.repository.urls";

    private final MavenUtil mavenUtil;
    private final File cache;
    private final Set<URL> classloaderURLs = new LinkedHashSet<URL>();
    private final Set<Pattern> parentFirst = new LinkedHashSet<Pattern>();
    private final Set<Pattern> childFirst = new LinkedHashSet<Pattern>();
    private ClassFilter parentExclusionFilter;
    Map<URL, Set<String>> singleClassesByUrl = new HashMap<URL, Set<String>>();

    public ChildFirstClassLoaderBuilder(boolean useEapRepository) {
        this.mavenUtil = MavenUtil.create(useEapRepository);
        final String root = System.getProperty(ROOT_PROPERTY);
        final String cacheFolderName = System.getProperty(CACHE_FOLDER_PROPERTY);
        if (root == null && cacheFolderName == null) {
            if (System.getProperty(STRICT_PROPERTY) != null) {
                throw new IllegalStateException("Please use the " + ROOT_PROPERTY + " and " + CACHE_FOLDER_PROPERTY + " system properties to take advantage of cached classpaths");
            }
            cache = new File("target", "cached-classloader");
            cache.mkdirs();
            if (!cache.exists()) {
                throw new IllegalStateException("Could not create cache file");
            }
            System.out.println("To optimize this test use the " + ROOT_PROPERTY + " and " + CACHE_FOLDER_PROPERTY + " system properties to take advantage of cached classpaths");
        } else if (root != null && cacheFolderName != null){
            if (cacheFolderName.indexOf('/') != -1 && cacheFolderName.indexOf('\\') != -1){
                throw new IllegalStateException("Please use either '/' or '\\' as a file separator");
            }

            File file = new File(".").getAbsoluteFile();
            final String[] rootChildren = root.startsWith("[") && root.endsWith("]") ? root.substring(1, root.length() - 1).split(",") : null;
            if (rootChildren.length > 1) {
                for (int i = 0 ; i < rootChildren.length ; i++) {
                    if (rootChildren[i].indexOf("/") != -1 || rootChildren[i].indexOf("\\") != -1) {
                        throw new IllegalStateException("Children must be direct children");
                    }
                    rootChildren[i] = rootChildren[i].trim();
                }
            }
            while (file != null) {
                if (rootChildren == null) {
                    if (file.getName().equals(root)) {
                        break;
                    }
                } else {
                    boolean hasAllChildren = true;
                    for (String child : rootChildren) {
                        if (!new File(file, child).exists()) {
                            hasAllChildren = false;
                            break;
                        }
                    }
                    if (hasAllChildren) {
                        break;
                    }
                }
                file = file.getParentFile();
            }
            if (file != null) {
                String separator = cacheFolderName.contains("/") ? "/" : "\\\\";
                for (String part : cacheFolderName.split(separator)) {
                    file = new File(file, part);
                    if (file.exists()) {
                        if (!file.isDirectory()) {
                            throw new IllegalStateException(file.getAbsolutePath() + " is not a directory");
                        }
                    } else {
                        if (!file.mkdir()) {
                            if (!file.exists()) {
                                throw new IllegalStateException(file.getAbsolutePath() + " could not be created");
                            }
                        }
                    }
                }
                cache = file;
            } else if (System.getProperty(STRICT_PROPERTY) != null) {
                throw new IllegalStateException("Could not find a parent file called '" + root + "'");
            } else {
                // Probably running in an IDE where the working dir is not the source code root
                cache = new File("target", "cached-classloader");
                cache.mkdirs();
                if (!cache.exists()) {
                    throw new IllegalStateException("Could not create cache file");
                }
            }
        } else {
            throw new IllegalStateException("You must either set both " + ROOT_PROPERTY + " and " + CACHE_FOLDER_PROPERTY + ", or none of them");
        }
    }

    public ChildFirstClassLoaderBuilder addURL(URL url) {
        classloaderURLs.add(url);
        return this;
    }

    public ChildFirstClassLoaderBuilder addSimpleResourceURL(String resource) throws MalformedURLException {
        URL url = ChildFirstClassLoader.class.getResource(resource);
        if (url == null) {
            ClassLoader cl = ChildFirstClassLoader.class.getClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            url = cl.getResource(resource);
            if (url == null) {
                File file = new File(resource);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
        }
        if (url == null) {
            throw new IllegalArgumentException("Could not find resource " + resource);
        }
        classloaderURLs.add(url);
        return this;
    }

    public ChildFirstClassLoaderBuilder addMavenResourceURL(String artifactGav) throws IOException, ClassNotFoundException {
        final String name = "maven-" + escape(artifactGav);
        final File file = new File(cache, name);
        if (file.exists()) {
            System.out.println("Using cached maven url for " + artifactGav + " from " + file.getAbsolutePath());
            final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            try {
                classloaderURLs.add((URL)in.readObject());
            } catch (Exception e) {
                System.out.println("Error loading cached maven url for " + artifactGav + " from " + file.getAbsolutePath());
                throw e;
            } finally {
                IoUtils.safeClose(in);
            }
        } else {
            System.out.println("No cached maven url for " + artifactGav + " found. " + file.getAbsolutePath() + " does not exist.");
            final URL url = mavenUtil.createMavenGavURL(artifactGav);
            classloaderURLs.add(url);
            final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            try {
                out.writeObject(url);
            } catch (Exception e) {
                System.out.println("Error writing cached maven url for " + artifactGav + " to " + file.getAbsolutePath());
                throw e;
            } finally {
                IoUtils.safeClose(out);
            }
        }
        return this;
    }

    public ChildFirstClassLoaderBuilder addRecursiveMavenResourceURL(String artifactGav, String... excludes)
            throws DependencyCollectionException, DependencyResolutionException, IOException, ClassNotFoundException {
        final String name = "maven-recursive-" + escape(artifactGav);
        final File file = new File(cache, name);
        if (file.exists()) {
            System.out.println("Using cached recursive maven urls for " + artifactGav + " from " + file.getAbsolutePath());
            final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            try {
                classloaderURLs.addAll((List<URL>)in.readObject());
            } catch (Exception e) {
                System.out.println("Error loading cached recursive maven urls for " + artifactGav + " from " + file.getAbsolutePath());
                throw e;
            } finally {
                IoUtils.safeClose(in);
            }
        } else {
            System.out.println("No cached recursive maven urls for " + artifactGav + " found. " + file.getAbsolutePath() + " does not exist.");
            final List<URL> urls = mavenUtil.createMavenGavRecursiveURLs(artifactGav, excludes);
            classloaderURLs.addAll(urls);
            final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            try {
                out.writeObject(urls);
            } catch (Exception e) {
                System.out.println("Error writing cached recursive maven urls for " + artifactGav + " to " + file.getAbsolutePath());
                throw e;
            } finally {
                IoUtils.safeClose(out);
            }
        }

        return this;
    }

    public ChildFirstClassLoaderBuilder addParentFirstClassPattern(String pattern) {
        parentFirst.add(compilePattern(pattern));
        return this;
    }

    public ChildFirstClassLoaderBuilder addChildFirstClassPattern(String pattern) {
        childFirst.add(compilePattern(pattern));
        return this;
    }

    public ChildFirstClassLoaderBuilder excludeFromParent(ClassFilter filter) {
        parentExclusionFilter = filter;
        return this;
    }

    private String escape(String artifactGav) {
        return artifactGav.replaceAll(":", "-x-");
    }

    public ClassLoader build() {
        //Put the singleClassesByUrl classes into classloaderURLs
        for (Map.Entry<URL, Set<String>> entry : singleClassesByUrl.entrySet()) {
            if (classloaderURLs.contains(entry.getKey())) {
                throw new IllegalStateException("Url " + entry.getKey() + " which is the code source for the following classes has "
                        + "already been set up via other means: " + entry.getValue());
            }
            classloaderURLs.add(entry.getKey());

            //Now add the classes for the URL as child first classes
            Set<String> childFirstNames = new HashSet<String>();
            for (String clazz : entry.getValue()) {
                childFirst.add(compilePattern(clazz));
                childFirstNames.add(clazz);
            }
            //Then get all the other classes for the URL and add as parent first classes
            try {
                File file = new File(entry.getKey().toURI());
                if (file.isDirectory()) {
                    addParentFirstPatternsFromDirectory(file, "", childFirstNames);
                } else {
                    //TODO - implement something like addParentFirstPatternsFromJar if that becomes needed
                    throw new IllegalStateException("Single class exclusions from jar files is not working: " + entry);
                }
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        ClassLoader parent = this.getClass().getClassLoader() != null ? this.getClass().getClassLoader() : null;
        return new ChildFirstClassLoader(parent, parentFirst, childFirst, parentExclusionFilter, classloaderURLs.toArray(new URL[classloaderURLs.size()]));
    }

    private void addParentFirstPatternsFromDirectory(File directory, String prefix, Set<String> childFirstClassNames) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                addParentFirstPatternsFromDirectory(file, prefix +file.getName() + ".", childFirstClassNames);
            } else {
                final String fileName = file.getName();
                if (fileName.endsWith(".class")) {
                    //TODO we can optimize this by adding parentFirst patterns for the whole package as long as
                    String className = prefix + file.getName().substring(0, fileName.length() - ".class".length());
                    parentFirst.add(compilePattern(className));
                }
            }
        }
    }

    private Pattern compilePattern(String pattern) {
        return Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
    }

    public ChildFirstClassLoaderBuilder addSingleChildFirstClass(Class<?>...classes) {
        for (Class<?> clazz : classes) {
            URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
            Set<String> classSet = singleClassesByUrl.get(url);
            if (classSet == null) {
                classSet = new HashSet<String>();
                singleClassesByUrl.put(url, classSet);
            }
            classSet.add(clazz.getName());
        }
        return this;
    }

}
