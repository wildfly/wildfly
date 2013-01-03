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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoaderBuilder {

    private List<URL> classloaderURLs = new ArrayList<URL>();
    private List<Pattern> parentFirst = new ArrayList<Pattern>();
    private List<Pattern> childFirst = new ArrayList<Pattern>();

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

    public ChildFirstClassLoaderBuilder addMavenResourceURL(String artifactGav) throws MalformedURLException {
        classloaderURLs.add(MavenUtil.createMavenGavURL(artifactGav));
        return this;
    }

    public ChildFirstClassLoaderBuilder addRecursiveMavenResourceURL(String artifactGav) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
        classloaderURLs.addAll(MavenUtil.createMavenGavRecursiveURLs(artifactGav));
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

    public ChildFirstClassLoaderBuilder createFromFile(File inputFile) {
        try {
            final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
            try {
                classloaderURLs = (List<URL>)in.readObject();
            } finally {
                IoUtils.safeClose(in);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public ClassLoader build() {
        return build(null);
    }

    public ClassLoader build(File outputFile) {
        if (outputFile != null) {
            outputSetupToFile(outputFile);
        }
        ClassLoader parent = this.getClass().getClassLoader() != null ? this.getClass().getClassLoader() : null;
        return new ChildFirstClassLoader(parent, parentFirst, childFirst, classloaderURLs.toArray(new URL[classloaderURLs.size()]));
    }

    private Pattern compilePattern(String pattern) {
        return Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
    }

    private void outputSetupToFile(File outputFile) {
        try {
            final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            try {
                out.writeObject(classloaderURLs);
            } finally {
                IoUtils.safeClose(out);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
