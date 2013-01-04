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
package org.jboss.as.subsystem.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Contains the initialization of a controller containing a legacy version of a subsystem.
 *
 *
 * @see KernelServicesBuilder#createLegacyKernelServicesBuilder(AdditionalInitialization, org.jboss.as.controller.ModelVersion) (AdditionalInitialization, org.jboss.as.controller.ModelVersion, String, String...)
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface LegacyKernelServicesInitializer {

    /**
     * Sets the name of the extension class name. If not set the {@link org.jboss.as.subsystem.test.AbstractSubsystemTest#getMainExtension()} will be used for the test class which created the
     * {@link KernelServicesBuilder} used to create this legacy kernel services initializer
     *
     * @param extensionClassName The name of the extension class. If {@code null} the name of the class of {@link org.jboss.as.subsystem.test.AbstractSubsystemTest#getMainExtension()} will be used
     * @return this initializer
     */
    LegacyKernelServicesInitializer setExtensionClassName(String extensionClassName);

    /**
     * Adds a URL to the legacy subsystem classloader's search path. The legacy subsystem will be loaded in its own classloader which will search these
     * urls before searching the classloader running the test.
     *
     * @param url a classloader search url
     * @return this initializer
     */
    LegacyKernelServicesInitializer addURL(URL url);

    /**
     * Adds a URL created from a simple resource to the legacy subsystem classloader's search path. The legacy subsystem will be loaded in its own classloader which will search these
     * urls before searching the classloader running the test.
     * Will attempt to resolve the URL from the resource name via the following mechanisms in the shown order:
     * <ul>
     *   <li>Using {@link Class#getResource(String)} semantics.</li>
     *   <li>Using {@link ClassLoader#getResource(String)} for the classloader used to load this class. If {@code this.getClass().getClassLoader()} is {@code null},
     *   the system classloader is used.</li>
     *   <li>Using {link {@link File#File(String)}}
     * </ul>
     *
     * @param resource the string containing the resource to be resolved
     * @return this initializer
     * @throws MalformedURLException if the URL format is bad
     * @throws IllegalArgumentException if the {@code resource} is null
     * @throws IllegalArgumentException if the resolved {@code resource} does not exist
     */
    LegacyKernelServicesInitializer addSimpleResourceURL(String resource) throws MalformedURLException;

    /**
     * Adds a URL created from a maven artifact GAV to the legacy subsystem classloader's search path. The legacy subsystem will be loaded in its own classloader which will search these
     * GAV.
     *
     * @param artifactGav a maven GAV, e.g.: {@code org.sonatype.aether:aether-api:1.13.1}
     * @return this initializer
     * @throws MalformedURLException if the URL format is bad
     * @throws IllegalArgumentException if the {@code artifactGav} is null
     * @throws IllegalArgumentException if the resolved {@code artifactGav} does not exist
     * @throws IllegalArgumentException if the resolved {@code artifactGav} does not contain a version
     */
    LegacyKernelServicesInitializer addMavenResourceURL(String artifactGav) throws MalformedURLException;

    /**
     * Add a class name pattern that should be loaded from the parent classloader
     *
     * @param pattern class name pattern
     * @return this initializer
     */
    LegacyKernelServicesInitializer addParentFirstClassPattern(String pattern);

    /**
     * Add a class name pattern that should be loaded from the child classloader
     *
     * @param pattern class name pattern
     * @return this initializer
     */
    LegacyKernelServicesInitializer addChildFirstClassPattern(String pattern);
}
