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

package org.jboss.as.controller.extension;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemXmlWriterRegistry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLMapper;

/**
* A registry for information about {@link Extension}s to the core application server.
*
* @author Brian Stansberry (c) 2011 Red Hat Inc.
*/
public class ExtensionRegistry {

    private final ProcessType processType;

    private SubsystemXmlWriterRegistry writerRegistry;
    private volatile ManagementResourceRegistration profileRegistration;
    private volatile ManagementResourceRegistration deploymentsRegistration;

    private final ConcurrentMap<String, ExtensionInfo> extensions = new ConcurrentHashMap<String, ExtensionInfo>();
    // subsystem -> extension
    private final ConcurrentMap<String, String> reverseMap = new ConcurrentHashMap<String, String>();
    private final RunningModeControl runningModeControl;
    // protected by extensions
    private boolean unnamedMerged;

    public ExtensionRegistry(ProcessType processType, RunningModeControl runningModeControl) {
        this.processType = processType;
        this.runningModeControl = runningModeControl;
    }

    /**
     * Gets the type of the current process.
     *
     * @return the current ProcessType.
     */
    public ProcessType getProcessType() {
        return processType;
    }

    /**
     * Sets the {@link SubsystemXmlWriterRegistry} to use for storing subsystem marshallers.
     *
     * @param writerRegistry  the writer registry
     */
    public void setWriterRegistry(final SubsystemXmlWriterRegistry writerRegistry) {
        this.writerRegistry = writerRegistry;

    }

    /**
     * Sets the {@link ManagementResourceRegistration}s for the resource under which subsystem child resources
     * should be registered.
     *
     * @param profileResourceRegistration the {@link ManagementResourceRegistration} for the resource under which
     *           subsystem child resources should be registered. Cannot be {@code null}
     * @param deploymentsResourceRegistration the {@link ManagementResourceRegistration} for the deployments resource.
     *                                        May be {@code null} if deployment resources are not supported
     */
    public void setSubsystemParentResourceRegistrations(ManagementResourceRegistration profileResourceRegistration,
                                                        ManagementResourceRegistration deploymentsResourceRegistration) {
        assert profileResourceRegistration != null : "profileResourceRegistration is null";
        assert this.profileRegistration == null : "profileResourceRegistration is already set";
        this.profileRegistration = profileResourceRegistration;

        if (deploymentsResourceRegistration != null) {
            assert this.deploymentsRegistration == null : "deploymentsResourceRegistration is already set";
            PathAddress subdepAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT));
            final ManagementResourceRegistration subdeployments = deploymentsResourceRegistration.getSubModel(subdepAddress);
            this.deploymentsRegistration = subdeployments == null ? deploymentsResourceRegistration
                        : new DeploymentManagementResourceRegistration(deploymentsResourceRegistration, subdeployments);

        }
    }

    /**
     * Gets the module names of all known {@link Extension}s.
     *
     * @return the names. Will not return {@code null}
     */
    public Set<String> getExtensionModuleNames() {
        return Collections.unmodifiableSet(extensions.keySet());
    }

    /**
     * Gets information about the subsystems provided by a given {@link Extension}.
     * @param moduleName  the name of the extension's module. Cannot be {@code null}
     *
     * @return map of subsystem names to information about the subsystem.
     */
    public Map<String, SubsystemInformation> getAvailableSubsystems(String moduleName) {
        mergeUnnamedParsers();
        Map<String, SubsystemInformation> result = null;
        final ExtensionInfo info = extensions.get(moduleName);
        if (info != null) {
            synchronized (info) {
                return Collections.unmodifiableMap(new HashMap<String, SubsystemInformation>(info.subsystems));
            }
        }
        return result;
    }

    /**
     * Gets an {@link ExtensionParsingContext} for use when
     * {@link Extension#initializeParsers(ExtensionParsingContext) initializing the extension's parsers}.
     *
     * @param moduleName the name of the extension's module. Cannot be {@code null}
     * @param xmlMapper the {@link XMLMapper} handling the extension parsing. Can be {@code null} if there won't
     *                  be any actual parsing (e.g. in a slave Host Controller or in a server in a managed domain)
     *
     * @return  the {@link ExtensionParsingContext}.  Will not return {@code null}
     */
    public ExtensionParsingContext getExtensionParsingContext(final String moduleName, final XMLMapper xmlMapper) {
        return new ExtensionParsingContextImpl(moduleName, xmlMapper);
    }

    /**
     * Gets an {@link ExtensionContext} for use when handling an {@code add} operation for
     * a resource representing an {@link Extension}.
     * @param moduleName the name of the extension's module. Cannot be {@code null}
     * @return  the {@link ExtensionContext}.  Will not return {@code null}
     *
     * @throws IllegalStateException if no {@link #setSubsystemParentResourceRegistrations(ManagementResourceRegistration, ManagementResourceRegistration)} profile resource registration has been set}
     */
    public ExtensionContext getExtensionContext(String moduleName) {
        return new ExtensionContextImpl(moduleName);
    }

    /**
     * Gets the URIs (in string form) of any XML namespaces
     * {@link ExtensionParsingContext#setSubsystemXmlMapping(String, XMLElementReader) registered by an extension without an accompanying subsystem name}
     * that could not be clearly associated with a single subsystem. If an extension registers namespaces with no
     * subsystem names but only has a single subsystem, the namespace can be clearly associated with that single
     * subsystem and will not appear as part of the result of this method.
     *
     * @param moduleName the name of the extension's module. Cannot be {@code null}
     * @return  the namespace URIs, or an empty set if there are none. Will not return {@code null}
     */
    public Set<String> getUnnamedNamespaces(final String moduleName) {
        mergeUnnamedParsers();
        Set<String> result;
        ExtensionInfo extension = extensions.get(moduleName);
        if (extension != null) {
            synchronized (extension) {
                result = new HashSet<String>(extension.unnamedParsers);
            }
        } else {
            result = Collections.emptySet();
        }

        return result;
    }

    /**
     * Cleans up a module's subsystems from the resource registration model.
     *
     * @param rootResource the model root resource
     * @param moduleName the name of the extension's module. Cannot be {@code null}
     * @throws IllegalStateException if the extension still has subsystems present in {@code rootResource} or its children
     */
    public void removeExtension(Resource rootResource, String moduleName) throws IllegalStateException {

        final ManagementResourceRegistration profileReg = profileRegistration;
        if (profileReg == null) {
            // we've been cleared and haven't re-initialized yet
            return;
        }

        final ManagementResourceRegistration deploymentsReg = deploymentsRegistration;
        ExtensionInfo extension = extensions.remove(moduleName);
        if (extension != null) {
            synchronized (extension) {
                Set<String> subsystemNames = extension.subsystems.keySet();
                for (String subsystem : subsystemNames) {
                    if (rootResource.getChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem)) != null) {
                        // Restore the data
                        extensions.put(moduleName, extension);
                        throw MESSAGES.removingExtensionWithRegisteredSubsystem(moduleName, subsystem);
                    }
                }
                for (Map.Entry<String, SubsystemInformation> entry : extension.subsystems.entrySet()) {
                    String subsystem = entry.getKey();
                    profileReg.unregisterSubModel(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem));
                    if (deploymentsReg != null) {
                        deploymentsReg.unregisterSubModel(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem));
                    }

                    if (extension.xmlMapper != null) {
                        SubsystemInformationImpl subsystemInformation = SubsystemInformationImpl.class.cast(entry.getValue());
                        for (String namespace : subsystemInformation.getXMLNamespaces()) {
                            extension.xmlMapper.unregisterRootElement(new QName(namespace, SUBSYSTEM));
                        }
                    }
                }
                if (extension.xmlMapper != null) {
                    for (String namespace : extension.unnamedParsers) {
                        extension.xmlMapper.unregisterRootElement(new QName(namespace, SUBSYSTEM));
                    }
                }
            }
        }
    }

    /**
     * Clears the registry to prepare for re-registration (e.g. as part of a reload).
     */
    public void clear() {
        synchronized (extensions) {    // we synchronize just to guard unnamedMerged
            profileRegistration = null;
            deploymentsRegistration = null;
            extensions.clear();
            reverseMap.clear();
            unnamedMerged = false;
        }
    }

    private ExtensionInfo getExtensionInfo(final String extensionModuleName) {
        ExtensionInfo result = extensions.get(extensionModuleName);
        if (result == null) {
            result = new ExtensionInfo(extensionModuleName);
            ExtensionInfo existing = extensions.putIfAbsent(extensionModuleName, result);
            result = existing == null ? result : existing;
        }
        return result;
    }

    private void checkNewSubystem(final String extensionModuleName, final String subsystemName) {
        String existingModule = reverseMap.putIfAbsent(subsystemName, extensionModuleName);
        if (existingModule != null && !extensionModuleName.equals(existingModule)) {
            throw ControllerMessages.MESSAGES.duplicateSubsystem(subsystemName, extensionModuleName, existingModule);
        }
    }

    /**
     * Check and see if any namespaces not associated with a subystem belong to extensions with only 1 subsystem.
     * If yes, associate them with that subsystem.
     */
    private void mergeUnnamedParsers() {
        synchronized (extensions) {  // we synchronize just to guard unnamedMerged
            if (!unnamedMerged) {
                for (ExtensionInfo extension : extensions.values()) {
                    synchronized (extension) {
                        if (extension.unnamedParsers.size() > 0 && extension.subsystems.size() == 1) {
                            // Only 1 subsystem; therefore we can merge
                            SubsystemInformationImpl info = SubsystemInformationImpl.class.cast(extension.subsystems.values().iterator().next());
                            for (String unnamed : extension.unnamedParsers) {
                                info.addParsingNamespace(unnamed);
                            }
                            extension.unnamedParsers.clear();
                        }
                    }
                }
                unnamedMerged = true;
            }
        }
    }

    public static interface SubsystemInformation {

        /**
         * Gets the URIs of the XML namespaces the subsystem can parse.
         *
         * @return list of XML namespace URIs. Will not return {@code null}
         */
        List<String> getXMLNamespaces();

        /**
         * Gets the major version of the subsystem's management interface, if available.
         * @return the major interface version, or {@code null} if the subsystem does not have a versioned interface
         */
        Integer getManagementInterfaceMajorVersion();

        /**
         * Gets the minor version of the subsystem's management interface, if available.
         * @return the minor interface version, or {@code null} if the subsystem does not have a versioned interface
         */
        Integer getManagementInterfaceMinorVersion();
    }


    private class ExtensionParsingContextImpl implements ExtensionParsingContext {

        private final ExtensionInfo extension;

        private ExtensionParsingContextImpl(String extensionName, XMLMapper xmlMapper) {
            extension = getExtensionInfo(extensionName);
            if (xmlMapper != null) {
                synchronized (extension) {
                    extension.xmlMapper = xmlMapper;
                }
            }
        }


        @Override
        public void setSubsystemXmlMapping(String namespaceUri, XMLElementReader<List<ModelNode>> reader) {
            assert namespaceUri != null : "namespaceUri is null";
            synchronized (extensions) { // we synchronize just to protect unnamedMerged
                synchronized (extension) {
                    if (extension.xmlMapper != null) {
                        extension.xmlMapper.registerRootElement(new QName(namespaceUri, SUBSYSTEM), reader);
                    }
                    extension.unnamedParsers.add(namespaceUri);
                }
                unnamedMerged = false;
            }
        }

        @Override
        public void setSubsystemXmlMapping(String subsystemName, String namespaceUri, XMLElementReader<List<ModelNode>> reader) {
            assert subsystemName != null : "subsystemName is null";
            assert namespaceUri != null : "namespaceUri is null";
            synchronized (extension) {
                extension.getSubsystemInfo(subsystemName).addParsingNamespace(namespaceUri);
                if (extension.xmlMapper != null) {
                    extension.xmlMapper.registerRootElement(new QName(namespaceUri, SUBSYSTEM), reader);
                }
            }
        }

        @Override
        public void setDeploymentXmlMapping(String namespaceUri, XMLElementReader<ModelNode> reader) {
            // ignored
        }

        @Override
        public void setDeploymentXmlMapping(String subsystemName, String namespaceUri, XMLElementReader<ModelNode> reader) {
            // ignored
        }
    }

    private class ExtensionContextImpl implements ExtensionContext {

        private final ExtensionInfo extension;

        private ExtensionContextImpl(String extensionName) {
            this.extension = getExtensionInfo(extensionName);
        }


        @Override
        public SubsystemRegistration registerSubsystem(String name) throws IllegalArgumentException, IllegalStateException {
            assert name != null : "name is null";
            checkNewSubystem(extension.extensionModuleName, name);
            extension.getSubsystemInfo(name); // establishes the SubsystemInfo
            return new SubsystemRegistrationImpl(name);
        }


        @Override
        public SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion) throws IllegalArgumentException, IllegalStateException {
            assert name != null : "name is null";
            checkNewSubystem(extension.extensionModuleName, name);
            SubsystemInformationImpl info = extension.getSubsystemInfo(name);
            info.setMajorVersion(majorVersion);
            info.setMinorVersion(minorVersion);
            return new SubsystemRegistrationImpl(name);
        }

        @Override
        public ProcessType getProcessType() {
            return processType;
        }

        @Override
        public RunningMode getRunningMode() {
            return runningModeControl.getRunningMode();
        }

        @Override
        public boolean isRuntimeOnlyRegistrationValid() {
            return processType.isServer() && runningModeControl.getRunningMode() != RunningMode.ADMIN_ONLY;
        }
    }

    private class SubsystemInformationImpl implements SubsystemInformation {

        private Integer majorVersion;
        private Integer minorVersion;
        private final List<String> parsingNamespaces = new ArrayList<String>();

        @Override
        public List<String> getXMLNamespaces() {
            return Collections.unmodifiableList(parsingNamespaces);
        }

        void addParsingNamespace(final String namespace) {
            parsingNamespaces.add(namespace);
        }

        @Override
        public Integer getManagementInterfaceMajorVersion() {
            return majorVersion;
        }

        private void setMajorVersion(Integer majorVersion) {
            this.majorVersion = majorVersion;
        }

        @Override
        public Integer getManagementInterfaceMinorVersion() {
            return minorVersion;
        }

        private void setMinorVersion(Integer minorVersion) {
            this.minorVersion = minorVersion;
        }
    }

    private class SubsystemRegistrationImpl implements SubsystemRegistration {
        private final String name;

        private SubsystemRegistrationImpl(String name) {
            this.name = name;
        }

        @Override
        public ManagementResourceRegistration registerSubsystemModel(final DescriptionProvider descriptionProvider) {
            assert descriptionProvider != null : "descriptionProvider is null";
            PathElement pathElement = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
            return registerSubsystemModel(new SimpleResourceDefinition(pathElement, descriptionProvider));
        }

        @Override
        public ManagementResourceRegistration registerSubsystemModel(ResourceDefinition resourceDefinition) {
            assert resourceDefinition != null : "resourceDefinition is null";

            ManagementResourceRegistration profileReg = profileRegistration;
            if (profileReg == null) {
                // we've been cleared and haven't re-initialized. This would mean a management op is registering
                // an extension at the same time we are shutting down or reloading. Unlikely.
                // Just provide a fake reg to the op (whose work is being discarded anyway) so it can finish cleanly.
                profileReg = getDummyRegistration();
            }
            return profileReg.registerSubModel(resourceDefinition);
        }

        @Override
        public ManagementResourceRegistration registerDeploymentModel(final DescriptionProvider descriptionProvider) {
            assert descriptionProvider != null : "descriptionProvider is null";
            PathElement pathElement = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
            return registerDeploymentModel(new SimpleResourceDefinition(pathElement, descriptionProvider));
        }

        @Override
        public ManagementResourceRegistration registerDeploymentModel(ResourceDefinition resourceDefinition) {
            assert resourceDefinition != null : "resourceDefinition is null";
            final ManagementResourceRegistration deploymentsReg = deploymentsRegistration;
            ManagementResourceRegistration base = deploymentsReg != null
                ? deploymentsReg
                : getDummyRegistration();
            return base.registerSubModel(resourceDefinition);
        }

        @Override
        public void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer) {
            writerRegistry.registerSubsystemWriter(name, writer);
        }

        private ManagementResourceRegistration getDummyRegistration() {
             return ManagementResourceRegistration.Factory.create(new DescriptionProvider() {
                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        return  new ModelNode();
                    }
                });
        }

    }

    private class ExtensionInfo {
        private final Map<String, SubsystemInformation> subsystems = new HashMap<String, SubsystemInformation>();
        private final Set<String> unnamedParsers = new HashSet<String>();
        private final String extensionModuleName;
        private XMLMapper xmlMapper;

        public ExtensionInfo(String extensionModuleName) {
            this.extensionModuleName = extensionModuleName;
        }


        private SubsystemInformationImpl getSubsystemInfo(final String subsystemName) {
            checkNewSubystem(extensionModuleName, subsystemName);
            synchronized (this) {
                SubsystemInformationImpl subsystem = SubsystemInformationImpl.class.cast(subsystems.get(subsystemName));
                if (subsystem == null) {
                    subsystem = new SubsystemInformationImpl();
                    subsystems.put(subsystemName, subsystem);
                }
                return subsystem;
            }

        }
    }

    private static class DeploymentManagementResourceRegistration implements ManagementResourceRegistration {

        private final ManagementResourceRegistration deployments;
        private final ManagementResourceRegistration subdeployments;

        private DeploymentManagementResourceRegistration(final ManagementResourceRegistration deployments,
                                                         final ManagementResourceRegistration subdeployments) {
            this.deployments = deployments;
            this.subdeployments = subdeployments;
        }

        @Override
        public boolean isRuntimeOnly() {
            return deployments.isRuntimeOnly();
        }

        @Override
        public void setRuntimeOnly(final boolean runtimeOnly){
            deployments.setRuntimeOnly(runtimeOnly);
            subdeployments.setRuntimeOnly(runtimeOnly);
        }


        @Override
        public boolean isRemote() {
            return deployments.isRemote();
        }

        @Override
        public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
            return deployments.getOperationHandler(address, operationName);
        }

        @Override
        public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
            return deployments.getOperationDescription(address, operationName);
        }

        @Override
        public Set<OperationEntry.Flag> getOperationFlags(PathAddress address, String operationName) {
            return deployments.getOperationFlags(address, operationName);
        }

        @Override
        public OperationEntry getOperationEntry(PathAddress address, String operationName) {
            return deployments.getOperationEntry(address, operationName);
        }

        @Override
        public Set<String> getAttributeNames(PathAddress address) {
            return deployments.getAttributeNames(address);
        }

        @Override
        public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
            return deployments.getAttributeAccess(address, attributeName);
        }

        @Override
        public Set<String> getChildNames(PathAddress address) {
            return deployments.getChildNames(address);
        }

        @Override
        public Set<PathElement> getChildAddresses(PathAddress address) {
            return deployments.getChildAddresses(address);
        }

        @Override
        public DescriptionProvider getModelDescription(PathAddress address) {
            return deployments.getModelDescription(address);
        }

        @Override
        public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
            return deployments.getOperationDescriptions(address, inherited);
        }

        @Override
        public ProxyController getProxyController(PathAddress address) {
            return deployments.getProxyController(address);
        }

        @Override
        public Set<ProxyController> getProxyControllers(PathAddress address) {
            return deployments.getProxyControllers(address);
        }

        @Override
        public ManagementResourceRegistration getOverrideModel(String name) {
            return deployments.getOverrideModel(name);
        }

        @Override
        public ManagementResourceRegistration getSubModel(PathAddress address) {
            return deployments.getSubModel(address);
        }

        @Override
        public ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider) {
            ManagementResourceRegistration depl = deployments.registerSubModel(address, descriptionProvider);
            ManagementResourceRegistration subdepl = subdeployments.registerSubModel(address, descriptionProvider);
            return new DeploymentManagementResourceRegistration(depl, subdepl);
        }

        @Override
        public ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition) {
            ManagementResourceRegistration depl = deployments.registerSubModel(resourceDefinition);
            ManagementResourceRegistration subdepl = subdeployments.registerSubModel(resourceDefinition);
            return new DeploymentManagementResourceRegistration(depl, subdepl);
        }

        @Override
        public void unregisterSubModel(PathElement address) {
            deployments.unregisterSubModel(address);
            subdeployments.unregisterSubModel(address);
        }

        @Override
        public boolean isAllowsOverride() {
            return deployments.isAllowsOverride();
        }

        @Override
        public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
            ManagementResourceRegistration depl = deployments.registerOverrideModel(name, descriptionProvider);
            ManagementResourceRegistration subdepl = subdeployments.registerOverrideModel(name, descriptionProvider);
            return new DeploymentManagementResourceRegistration(depl, subdepl);
        }

        @Override
        public void unregisterOverrideModel(String name) {
            deployments.unregisterOverrideModel(name);
            subdeployments.unregisterOverrideModel(name);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
        }

        @Override
        public void unregisterOperationHandler(String operationName) {
            deployments.unregisterOperationHandler(operationName);
            subdeployments.unregisterOperationHandler(operationName);
        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
            deployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
            subdeployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, flags);
            subdeployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, flags);
        }

        @Override
        public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {
            deployments.registerReadWriteAttribute(definition, readHandler, writeHandler);
            subdeployments.registerReadWriteAttribute(definition, readHandler, writeHandler);
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage) {
            deployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
            subdeployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerReadOnlyAttribute(attributeName, readHandler, flags);
            subdeployments.registerReadOnlyAttribute(attributeName, readHandler, flags);
        }

        @Override
        public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {
            deployments.registerReadOnlyAttribute(definition, readHandler);
            subdeployments.registerReadOnlyAttribute(definition, readHandler);
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
            deployments.registerMetric(attributeName, metricHandler);
            subdeployments.registerMetric(attributeName, metricHandler);
        }

        @Override
        public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
            deployments.registerMetric(definition, metricHandler);
            subdeployments.registerMetric(definition, metricHandler);
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerMetric(attributeName, metricHandler, flags);
            subdeployments.registerMetric(attributeName, metricHandler, flags);
        }

        @Override
        public void unregisterAttribute(String attributeName) {
            deployments.unregisterAttribute(attributeName);
            subdeployments.unregisterAttribute(attributeName);
        }

        @Override
        public void registerProxyController(PathElement address, ProxyController proxyController) {
            deployments.registerProxyController(address, proxyController);
            subdeployments.registerProxyController(address, proxyController);
        }

        @Override
        public void unregisterProxyController(PathElement address) {
            deployments.unregisterProxyController(address);
            subdeployments.unregisterProxyController(address);
        }
    }
}
