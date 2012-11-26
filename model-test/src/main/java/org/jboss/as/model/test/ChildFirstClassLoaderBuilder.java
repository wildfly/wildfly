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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

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
        classloaderURLs.add(ChildFirstClassLoader.createSimpleResourceURL(resource));
        return this;
    }

    public ChildFirstClassLoaderBuilder addMavenResourceURL(String artifactGav) throws MalformedURLException {
        classloaderURLs.add(ChildFirstClassLoader.createMavenGavURL(artifactGav));
        return this;
    }

    public ChildFirstClassLoaderBuilder addRecursiveMavenResourceURL(String artifactGav) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
        classloaderURLs.addAll(ChildFirstClassLoader.createMavenGavRecursiveURLs(artifactGav));
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

    public ClassLoader build() {
        ClassLoader parent = this.getClass().getClassLoader() != null ? this.getClass().getClassLoader() : null;
        return new ChildFirstClassLoader(parent, parentFirst, childFirst, classloaderURLs.toArray(new URL[classloaderURLs.size()]));
    }

    private Pattern compilePattern(String pattern) {
        return Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
    }
}
