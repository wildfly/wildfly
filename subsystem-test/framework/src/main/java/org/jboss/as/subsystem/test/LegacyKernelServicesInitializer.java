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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.OperationFixer;
import org.jboss.modules.filter.ClassFilter;


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
     *   <li>Using {link {@link java.io.File#File(String)}}
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
     * @throws IOException if something went wrong with the caching of the url
     * @throws ClassNotFoundException if something went wrong with the caching of the url
     * @throws IllegalArgumentException if the {@code artifactGav} is null
     * @throws IllegalArgumentException if the resolved {@code artifactGav} does not exist
     * @throws IllegalArgumentException if the resolved {@code artifactGav} does not contain a version
     */
    LegacyKernelServicesInitializer addMavenResourceURL(String...artifactGav) throws ClassNotFoundException, IOException;

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

    /**
     * By default we leave the legacy xml persister on for extra test coverage, but on a real slave xml will not be persisted.
     * In some cases there may be known bugs in the legacy subsystem's xml persister. Call this method to turn off xml
     * persisting in the legacy subsystem, when those cases are encountered.
     *
     * @return this initializer
     */
    LegacyKernelServicesInitializer dontPersistXml();

    /**
     * By default the {@link KernelServicesBuilder#build()} method will use the boot operations passed into the
     * legacy controller and try to boot up the current controller with those. This is for checking that e.g. cli scripts written
     * against the legacy controller still work with the current one. To turn this check off call this method.
     *
     * @return this initializer
     */
    LegacyKernelServicesInitializer skipReverseControllerCheck();

    /**
     * By default the {@link KernelServicesBuilder#build()} method will use the boot operations passed into the
     * legacy controller and try to boot up the current controller with those. This is for checking that e.g. cli scripts written
     * against the legacy controller still work with the current one. To turn this check off call this method.
     *
     * @param additionalInit the additional initialization to use
     * @param modelFixer a model fixer to fix up the booted subsystem model
     * @return this initializer
     */
    LegacyKernelServicesInitializer configureReverseControllerCheck(AdditionalInitialization additionalInit, ModelFixer modelFixer);


    /**
         * By default the {@link KernelServicesBuilder#build()} method will use the boot operations passed into the
         * legacy controller and try to boot up the current controller with those. This is for checking that e.g. cli scripts written
         * against the legacy controller still work with the current one. To turn this check off call this method.
         *
         * @param additionalInit the additional initialization to use
         * @param modelFixer a model fixer to fix up the booted subsystem model
         * @param operationFixer an operation fixer to fix up the operations to be booted
         * @return this initializer
         */
    LegacyKernelServicesInitializer configureReverseControllerCheck(AdditionalInitialization additionalInit,
                   ModelFixer modelFixer, OperationFixer operationFixer);

    /**
     * By default all operations sent into the model controller will be validated on boot. Operations matching what is
     * set up here will not be validated. This is mainly because the {@link org.jboss.as.controller.operations.validation.OperationValidator} used in 7.1.x did not handle expressions very well
     * when checking ranges. If there is a problem you should try to call {@link #addOperationValidationResolve(String, PathAddress)}
     * first.
     *
     * @param name the name of the operation, or {@code *} as a wildcard capturing all names
     * @param pathAddress the address of the operation, the pathAddress may use {@code *} as a wildcard for both the key and the value of {@link org.jboss.as.controller.PathElement}s
     */
    LegacyKernelServicesInitializer addOperationValidationExclude(String name, PathAddress pathAddress);

    /**
     * By default all operations sent into the model controller will be validated on boot. Operations matching what is
     * set up here will not be validated. This is mainly because the {@link org.jboss.as.controller.operations.validation.OperationValidator} used in 7.1.x did not handle expressions very well
     * when checking ranges.
     *
     * @param name the name of the operation, or {@code *} as a wildcard capturing all names
     * @param pathAddress the address of the operation, the pathAddress may use {@code *} as a wildcard for both the key and the value of {@link org.jboss.as.controller.PathElement}s
     * @return this initializer
     */
    LegacyKernelServicesInitializer addOperationValidationResolve(String name, PathAddress pathAddress);

    /**
     * By default all operations sent into the model controller will be validated on boot. Operations matching what is
     * set up here will be validated, but allow you to adjust the operation before validating.
     *
     * @param name the name of the operation, or {@code *} as a wildcard capturing all names
     * @param pathAddress the address of the operation, the pathAddress may use {@code *} as a wildcard for both the key and the value of {@link org.jboss.as.controller.PathElement}s
     * @param operationFixer an operation fixer to fix the operation before validation. This must be Serializable, and must be added via {@link #addSingleChildFirstClass(Class...)}
     * @return this initializer
     */
    LegacyKernelServicesInitializer addOperationValidationFixer(String name, PathAddress pathAddress, OperationFixer operationFixer);

     /**
     * By default all the parent classloader classes are available to be kernel which may provides conflict with classes loaded
     * through the service initializer. Thus we provide an exclusion mecanism.
     *
     * @param exclusionFilter the class filter used to exclude class from the the parent classloader when resolving.
     * @return this initializer
     */
    LegacyKernelServicesInitializer excludeFromParent(ClassFilter exclusionFilter);

    /**
     * Add classes to be loaded from the child classloader. The url's from the {@code classes} protection domain get added to the
     * list of URLs used by the child  first classloader. Classes coming from that URL, which are not specified in the {@code classes}
     * list, are loaded from the parent classloader. URLs implicitely added via this method must not be added via any of the other
     * methods such as {@link #addSimpleResourceURL(String)} and {@link #addMavenResourceURL(String)}; if that happens an
     * {@link IllegalStateException} will get thrown when the controllers are built.
     *
     * This is useful for adding {@link AdditionalInitialization} implementations for the scoped legacy controller.
     *
     * @param classes the classes to be added as child first classes
     * @return this initializer
     */
    LegacyKernelServicesInitializer addSingleChildFirstClass(Class<?>...classes);
}
