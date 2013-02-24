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

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.dmr.ModelNode;

/**
 * A builder to create a controller and initialize it with the passed in subsystem xml or boot operations.
 *
 * @see AbstractSubsystemTest#createKernelServicesBuilder(AdditionalInitialization)
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface KernelServicesBuilder {
    /**
     * Sets the subsystem xml resource containing the xml to be parsed to create the boot operations used to initialize the controller.The resource is loaded using similar
     * semantics to {@link Class#getResource(String)}
     * @throws IllegalStateException if {@link #setBootOperations(List)}, {@link #setSubsystemXml(String)} have already been called
     * @return this builder
     * @throws IOException if there were problems reading the resource
     * @throws XMLStreamException if there were problems parsing the xml
     */
    KernelServicesBuilder setSubsystemXmlResource(String resource) throws IOException, XMLStreamException;

    /**
     * Sets the subsystem xml to be parsed to create the boot operations used to initialize the controller
     * @param subsystemXml the subsystem xml
     * @return this builder
     * @throws IllegalStateException if {@link #setBootOperations(List)}, or {@link #setSubsystemXmlResource(String)} have
     * already been called
     * @throws XMLStreamException if there were problems parsing the xml
     */
    KernelServicesBuilder setSubsystemXml(String subsystemXml) throws XMLStreamException;

    /**
     * Sets the boot operations to be used to initialize the controller
     * @param bootOperations the boot operations
     * @return this builder
     * @throws IllegalStateException if {@link #setSubsystemXml(String)} or {@link #setSubsystemXmlResource(String)} have already been called
     */
    KernelServicesBuilder setBootOperations(List<ModelNode> bootOperations);

    /**
     * Sets the boot operations to be used to initialize the controller
     * @param bootOperations the boot operations
     * @return this builder
     * @throws IllegalStateException if {@link #setSubsystemXml(String)} or {@link #setSubsystemXmlResource(String)} have already been called
     */
    KernelServicesBuilder setBootOperations(ModelNode ... bootOperations);

    /**
     * Creates a new legacy kernel services initializer used to configure a new controller containing an older version of the subsystem being tested.
     * When {@link #build()} is called any legacy controllers will be created as well.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param modelVersion The model version of the legacy subsystem
     * @return the legacy kernel services initializer
     * @throws IllegalArgumentException if {@code additionalInit} does not have a running mode of {@link org.jboss.as.controller.RunningMode#ADMIN_ONLY}
     * @throws IllegalStateException if {@link #build()} has already been called
     */
     LegacyKernelServicesInitializer createLegacyKernelServicesBuilder(AdditionalInitialization additionalInit, ModelVersion modelVersion);

    /**
     * Creates the controller and initializes it with the passed in configuration options.
     * If {@link #createLegacyKernelServicesBuilder(AdditionalInitialization, ModelVersion)} was called kernel services will be created for the legacy subsystem
     * controllers as well, accessible from {@link KernelServices#getLegacyServices(ModelVersion)} on the created {@link KernelServices}
     * @throws IllegalStateException if #build() has already been called
     * @return the kernel services wrapping the controller
     */
    KernelServices build() throws Exception;

    /**
     * Parses the given xml into operations. This may be called after {@link #build()} has been called.
     *
     * @param xml a string containing the xml
     * @return the parsed operations
     */
    List<ModelNode> parseXml(String xml) throws Exception;

    /**
     * Parses the given xml into operations. The resource is loaded using similar semantics to {@link Class#getResource(String)}.
     * This may be called after {@link #build()} has been called.
     *
     * @param xmlResource a string containing the xml resource
     * @return the parsed operations
     */
    List<ModelNode> parseXmlResource(String xmlResource) throws Exception;
}
