/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses <code>jboss-deployment-structure.xml</code>, and merges the result with the deployment.
 * <p/>
 * <code>jboss-deployment-structure.xml</code> is only parsed for top level deployments. It allows configuration of the following for
 * deployments and sub deployments:
 * <ul>
 * <li>Additional dependencies</li>
 * <li>Additional resource roots</li>
 * <li>{@link java.lang.instrument.ClassFileTransformer}s that will be applied at classloading</li>
 * <li>Child first behaviour</li>
 * </ul>
 * <p/>
 * It also allows for the use to add additional modules, using a syntax similar to that used in module xml files.
 *
 * @author Stuart Douglas
 * @author Marius Bogoevici
 *
 */
public class DeploymentStructureDescriptorParser implements DeploymentUnitProcessor {

    private static class ModuleStructureSpec {
        private ModuleIdentifier moduleIdentifier;
        private final List<ModuleDependency> moduleDependencies = new ArrayList<ModuleDependency>();
        private final List<ResourceRoot> resourceRoots = new ArrayList<ResourceRoot>();
        private final List<ExtensionListEntry> moduleExtensionDependencies = new ArrayList<ExtensionListEntry>();
        private final List<FilterSpecification> exportFilters = new ArrayList<FilterSpecification>();
        private final List<ModuleIdentifier> exclusions = new ArrayList<ModuleIdentifier>();
        private final List<String> classFileTransformers = new ArrayList<String>();

        public ModuleIdentifier getModuleIdentifier() {
            return moduleIdentifier;
        }

        public void setModuleIdentifier(ModuleIdentifier moduleIdentifier) {
            this.moduleIdentifier = moduleIdentifier;
        }

        public void addModuleDependency(ModuleDependency dependency) {
            moduleDependencies.add(dependency);
        }

        public List<ModuleDependency> getModuleDependencies() {
            return Collections.unmodifiableList(moduleDependencies);
        }

        public void addResourceRoot(ResourceRoot resourceRoot) {
            resourceRoots.add(resourceRoot);
        }

        public List<ResourceRoot> getResourceRoots() {
            return Collections.unmodifiableList(resourceRoots);
        }

        public void addModuleExtensionDependency(ExtensionListEntry extension) {
            moduleExtensionDependencies.add(extension);
        }

        public List<ExtensionListEntry> getModuleExtensionDependencies() {
            return Collections.unmodifiableList(moduleExtensionDependencies);
        }

        public List<ModuleIdentifier> getExclusions() {
            return exclusions;
        }

        public List<FilterSpecification> getExportFilters() {
            return exportFilters;
        }

        public List<String> getClassFileTransformers() {
            return classFileTransformers;
        }

    }

    private static class ParseResult {
        private Boolean earSubDeploymentsIsolated = null;
        private ModuleStructureSpec rootDeploymentSpecification;
        private final Map<String, ModuleStructureSpec> subDeploymentSpecifications = new HashMap<String, ModuleStructureSpec>();
        private final List<ModuleStructureSpec> additionalModules = new ArrayList<ModuleStructureSpec>();
    }

    private static final Logger log = Logger
            .getLogger("org.jboss.as.server.deployment.module.deployment-structure-descriptor-processor");

    public static final String[] DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS = {"META-INF/jboss-deployment-structure.xml",
            "WEB-INF/jboss-deployment-structure.xml"};

    private static final String NAMESPACE = "urn:jboss:deployment-structure:1.0";

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    enum Element {
        JBOSS_DEPLOYMENT_STRUCTURE,
        EAR_SUBDEPLOYMENTS_ISOLATED,
        DEPLOYMENT,
        SUB_DEPLOYMENT,
        MODULE,
        DEPENDENCIES,
        EXPORTS,
        IMPORTS,
        INCLUDE,
        INCLUDE_SET,
        EXCLUDE,
        EXCLUDE_SET,
        RESOURCES,
        RESOURCE_ROOT,
        PATH,
        FILTER,
        TRANSFORMERS,
        TRANSFORMER,
        EXCLUSIONS,

        // default unknown element
        UNKNOWN;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE, "jboss-deployment-structure"), Element.JBOSS_DEPLOYMENT_STRUCTURE);
            elementsMap.put(new QName(NAMESPACE, "ear-subdeployments-isolated"), Element.EAR_SUBDEPLOYMENTS_ISOLATED);
            elementsMap.put(new QName(NAMESPACE, "deployment"), Element.DEPLOYMENT);
            elementsMap.put(new QName(NAMESPACE, "sub-deployment"), Element.SUB_DEPLOYMENT);
            elementsMap.put(new QName(NAMESPACE, "module"), Element.MODULE);
            elementsMap.put(new QName(NAMESPACE, "dependencies"), Element.DEPENDENCIES);
            elementsMap.put(new QName(NAMESPACE, "resources"), Element.RESOURCES);
            elementsMap.put(new QName(NAMESPACE, "resource-root"), Element.RESOURCE_ROOT);
            elementsMap.put(new QName(NAMESPACE, "path"), Element.PATH);
            elementsMap.put(new QName(NAMESPACE, "exports"), Element.EXPORTS);
            elementsMap.put(new QName(NAMESPACE, "imports"), Element.IMPORTS);
            elementsMap.put(new QName(NAMESPACE, "include"), Element.INCLUDE);
            elementsMap.put(new QName(NAMESPACE, "exclude"), Element.EXCLUDE);
            elementsMap.put(new QName(NAMESPACE, "exclusions"), Element.EXCLUSIONS);
            elementsMap.put(new QName(NAMESPACE, "include-set"), Element.INCLUDE_SET);
            elementsMap.put(new QName(NAMESPACE, "exclude-set"), Element.EXCLUDE_SET);
            elementsMap.put(new QName(NAMESPACE, "filter"), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE, "transformers"), Element.TRANSFORMERS);
            elementsMap.put(new QName(NAMESPACE, "transformer"), Element.TRANSFORMER);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        NAME, SLOT, EXPORT, SERVICES, PATH, OPTIONAL, CLASS,

        // default unknown attribute
        UNKNOWN;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName("name"), NAME);
            attributesMap.put(new QName("slot"), SLOT);
            attributesMap.put(new QName("export"), EXPORT);
            attributesMap.put(new QName("services"), SERVICES);
            attributesMap.put(new QName("path"), PATH);
            attributesMap.put(new QName("optional"), OPTIONAL);
            attributesMap.put(new QName("class"), CLASS);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    enum Disposition {
        NONE("none"), IMPORT("import"), EXPORT("export"),;

        private static final Map<String, Disposition> values;

        static {
            final Map<String, Disposition> map = new HashMap<String, Disposition>();
            for (Disposition d : values()) {
                map.put(d.value, d);
            }
            values = map;
        }

        private final String value;

        Disposition(String value) {
            this.value = value;
        }

        static Disposition of(String value) {
            final Disposition disposition = values.get(value);
            return disposition == null ? NONE : disposition;
        }
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final ServiceModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        VirtualFile deploymentFile = null;
        for (String loc : DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS) {
            VirtualFile file = resourceRoot.getRoot().getChild(loc);
            if (file.exists()) {
                deploymentFile = file;
                break;
            }
        }
        if (deploymentFile == null) {
            return;
        }
        if (deploymentUnit.getParent() != null) {
            log.warnf("%s in subdeployment ignored. jboss-deployment-structure.xml is only parsed for top level deployments.",
                    deploymentFile.getPathName());
            return;
        }

        try {
            ParseResult result = parse(deploymentFile.getPhysicalFile(), deploymentUnit, moduleLoader);

            ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            if (result.earSubDeploymentsIsolated != null) {
                // set the ear subdeployment isolation value overridden via the jboss-deployment-structure.xml
                moduleSpec.setSubDeploymentModulesIsolated(result.earSubDeploymentsIsolated);
            }
            // handle the the root deployment
            if (result.rootDeploymentSpecification != null) {
                moduleSpec.addUserDependencies(result.rootDeploymentSpecification.getModuleDependencies());
                moduleSpec.addExclusions(result.rootDeploymentSpecification.getExclusions());
                for (ResourceRoot additionalResourceRoot : result.rootDeploymentSpecification.getResourceRoots()) {
                    deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, additionalResourceRoot);
                }
                for (String classFileTransformer : result.rootDeploymentSpecification.getClassFileTransformers()) {
                    moduleSpec.addClassFileTransformer(classFileTransformer);
                }
            }
            // handle sub deployments
            final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            final Map<String, DeploymentUnit> subDeploymentMap = new HashMap<String, DeploymentUnit>();
            for (final DeploymentUnit subDeployment : subDeployments) {
                final ResourceRoot subDeploymentRoot = subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
                String path = subDeploymentRoot.getRoot().getPathNameRelativeTo(resourceRoot.getRoot());
                subDeploymentMap.put(path, subDeployment);
            }

            for (Entry<String, ModuleStructureSpec> subDeploymentResult : result.subDeploymentSpecifications.entrySet()) {
                final String path = subDeploymentResult.getKey();
                final ModuleStructureSpec spec = subDeploymentResult.getValue();
                if (!subDeploymentMap.containsKey(path)) {
                    throw subDeploymentNotFound(path, subDeploymentMap.keySet());
                }
                final DeploymentUnit subDeployment = subDeploymentMap.get(path);
                ModuleSpecification subModuleSpec = subDeployment.getAttachment(Attachments.MODULE_SPECIFICATION);

                subModuleSpec.addUserDependencies(spec.getModuleDependencies());
                subModuleSpec.addExclusions(spec.getExclusions());
                for (ResourceRoot additionalResourceRoot : spec.getResourceRoots()) {
                    subDeployment.addToAttachmentList(Attachments.RESOURCE_ROOTS, additionalResourceRoot);
                }
                for (String classFileTransformer : spec.getClassFileTransformers()) {
                    subModuleSpec.addClassFileTransformer(classFileTransformer);
                }
            }

            // handle additional modules
            for (final ModuleStructureSpec additionalModule : result.additionalModules) {
                AdditionalModuleSpecification additional = new AdditionalModuleSpecification(additionalModule
                        .getModuleIdentifier(),
                        additionalModule.getResourceRoots());
                additional.addSystemDependencies(additionalModule.getModuleDependencies());
                deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_MODULES, additional);
            }

        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private DeploymentUnitProcessingException subDeploymentNotFound(String path, Collection<String> subDeployments) {
        StringBuilder builder = new StringBuilder();
        builder.append("Sub deployment ");
        builder.append(path);
        builder.append(" in jboss-structure.xml was not found. Available sub deployments: ");
        for (String dep : subDeployments) {
            builder.append(dep);
            builder.append(", ");
        }
        return new DeploymentUnitProcessingException(builder.toString());
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    static ParseResult parse(final File file, DeploymentUnit deploymentUnit, ModuleLoader moduleLoader)
            throws DeploymentUnitProcessingException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new DeploymentUnitProcessingException("No jboss-deployment-structure.xml file found at " + file);
        }
        try {
            return parse(fis, file, deploymentUnit, moduleLoader);
        } finally {
            safeClose(fis);
        }
    }

    private static void setIfSupported(XMLInputFactory inputFactory, String property, Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private static ParseResult parse(InputStream source, File file, DeploymentUnit deploymentUnit, ModuleLoader moduleLoader)
            throws DeploymentUnitProcessingException {
        try {
            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                ParseResult result = new ParseResult();
                parseDocument(deploymentUnit, streamReader, result, moduleLoader);
                return result;
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw new DeploymentUnitProcessingException("Error loading jboss-structure.xml from " + file.getPath(), e);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
    }

    private static void safeClose(final XMLStreamReader streamReader) {
        if (streamReader != null)
            try {
                streamReader.close();
            } catch (XMLStreamException e) {
                // ignore
            }
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                kind = "attribute";
                break;
            case XMLStreamConstants.CDATA:
                kind = "cdata";
                break;
            case XMLStreamConstants.CHARACTERS:
                kind = "characters";
                break;
            case XMLStreamConstants.COMMENT:
                kind = "comment";
                break;
            case XMLStreamConstants.DTD:
                kind = "dtd";
                break;
            case XMLStreamConstants.END_DOCUMENT:
                kind = "document end";
                break;
            case XMLStreamConstants.END_ELEMENT:
                kind = "element end";
                break;
            case XMLStreamConstants.ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case XMLStreamConstants.NAMESPACE:
                kind = "namespace";
                break;
            case XMLStreamConstants.NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case XMLStreamConstants.SPACE:
                kind = "whitespace";
                break;
            case XMLStreamConstants.START_DOCUMENT:
                kind = "document start";
                break;
            case XMLStreamConstants.START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XMLStreamException(b.toString(), reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document", location);
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }

    private static void parseDocument(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                      ModuleLoader moduleLoader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.START_DOCUMENT: {
                    parseRootElement(deploymentUnit, reader, result, moduleLoader);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    if (Element.of(reader.getName()) != Element.JBOSS_DEPLOYMENT_STRUCTURE) {
                        throw unexpectedContent(reader);
                    }
                    parseStructureContents(deploymentUnit, reader, result, moduleLoader);
                    parseEndDocument(reader);
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseRootElement(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                         ModuleLoader moduleLoader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.START_ELEMENT: {
                    if (Element.of(reader.getName()) != Element.JBOSS_DEPLOYMENT_STRUCTURE) {
                        throw unexpectedContent(reader);
                    }
                    parseStructureContents(deploymentUnit, reader, result, moduleLoader);
                    parseEndDocument(reader);
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseStructureContents(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                               ModuleLoader moduleLoader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count != 0) {
            throw unexpectedContent(reader);
        }
        // xsd:sequence
        boolean deploymentVisited = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());

                    switch (element) {
                        case EAR_SUBDEPLOYMENTS_ISOLATED:
                            // TODO: This element should only be allowed for jboss-deployment-structure.xml
                            // of an .ear and *not* for a .war. Should we throw an error for this based on the deployment
                            // unit type?
                            String value = reader.getElementText();
                            if (value == null || value.isEmpty()) {
                                result.earSubDeploymentsIsolated = true;
                            } else {
                                result.earSubDeploymentsIsolated = Boolean.valueOf(value);
                            }
                            break;
                        case DEPLOYMENT:
                            if (deploymentVisited) {
                                throw unexpectedContent(reader);
                            }
                            deploymentVisited = true;
                            parseDeployment(deploymentUnit, reader, result, moduleLoader);
                            break;
                        case SUB_DEPLOYMENT:
                            parseSubDeployment(deploymentUnit, reader, result, moduleLoader);
                            break;
                        case MODULE:
                            parseModule(deploymentUnit, reader, result, moduleLoader);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseDeployment(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                        ModuleLoader moduleLoader) throws XMLStreamException {
        result.rootDeploymentSpecification = new ModuleStructureSpec();
        parseModuleStructureSpec(deploymentUnit, reader, result.rootDeploymentSpecification, moduleLoader);
    }

    private static void parseSubDeployment(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                           ModuleLoader moduleLoader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (result.subDeploymentSpecifications.containsKey(name)) {
            throw new XMLStreamException("Sub deployment " + name + " is listed twice in jboss-structure.xml");
        }
        ModuleStructureSpec moduleSpecification = new ModuleStructureSpec();
        result.subDeploymentSpecifications.put(name, moduleSpecification);
        parseModuleStructureSpec(deploymentUnit, reader, moduleSpecification, moduleLoader);
    }

    private static void parseModule(DeploymentUnit deploymentUnit, XMLStreamReader reader, ParseResult result,
                                    ModuleLoader moduleLoader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String slot = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case SLOT:
                    slot = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        // FIXME: change this
        if (!name.startsWith("deployment.")) {
            throw new XMLStreamException("Additional module name " + name
                    + " is not valid. Names must start with 'deployment.'");
        }
        ModuleStructureSpec moduleSpecification = new ModuleStructureSpec();
        moduleSpecification.setModuleIdentifier(ModuleIdentifier.create(name, slot));
        result.additionalModules.add(moduleSpecification);
        parseModuleStructureSpec(deploymentUnit, reader, moduleSpecification, moduleLoader);
    }

    private static void parseModuleStructureSpec(DeploymentUnit deploymentUnit, XMLStreamReader reader,
                                                 ModuleStructureSpec moduleSpec, ModuleLoader moduleLoader) throws XMLStreamException {
        // xsd:all
        Set<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    if (visited.contains(element)) {
                        throw unexpectedContent(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case EXPORTS:
                            parseFilterList(reader, moduleSpec.getExportFilters());
                            break;
                        case DEPENDENCIES:
                            parseDependencies(reader, moduleSpec, moduleLoader);
                            break;
                        case RESOURCES:
                            parseResources(deploymentUnit, reader, moduleSpec);
                            break;
                        case TRANSFORMERS:
                            parseTransformers(reader, moduleSpec);
                            break;
                        case EXCLUSIONS:
                            parseExclusions(reader, moduleSpec);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseDependencies(final XMLStreamReader reader, final ModuleStructureSpec specBuilder,
                                          ModuleLoader moduleLoader) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case MODULE:
                            parseModuleDependency(reader, specBuilder, moduleLoader);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleDependency(final XMLStreamReader reader, final ModuleStructureSpec specBuilder,
                                              ModuleLoader moduleLoader) throws XMLStreamException {
        String name = null;
        String slot = null;
        boolean export = false;
        boolean optional = false;
        Disposition services = Disposition.NONE;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case SLOT:
                    slot = reader.getAttributeValue(i);
                    break;
                case EXPORT:
                    export = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                case SERVICES:
                    services = Disposition.of(reader.getAttributeValue(i));
                    break;
                case OPTIONAL:
                    optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        ModuleDependency dependency = new ModuleDependency(moduleLoader, ModuleIdentifier.create(name, slot), optional, export,
                services == Disposition.IMPORT);
        specBuilder.addModuleDependency(dependency);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (services == Disposition.EXPORT) {
                        // If services are to be re-exported, add META-INF/services -> true near the end of the list
                        dependency.addExportFilter(PathFilters.getMetaInfServicesFilter(), true);
                    }
                    if (export) {
                        // If re-exported, add META-INF/** -> false at the end of the list (require explicit override)
                        dependency.addExportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                        dependency.addExportFilter(PathFilters.getMetaInfFilter(), false);
                    }
                    if (dependency.getImportFilters().isEmpty()) {
                        dependency.addImportFilter(services == Disposition.NONE ? PathFilters.getDefaultImportFilter()
                                : PathFilters.getDefaultImportFilterWithServices(), true);
                    } else {
                        if (services != Disposition.NONE) {
                            dependency.addImportFilter(PathFilters.getMetaInfServicesFilter(), true);
                        }
                        dependency.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                        dependency.addImportFilter(PathFilters.getMetaInfFilter(), false);
                    }
                    specBuilder.addModuleDependency(dependency);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case EXPORTS:
                            parseFilterList(reader, dependency.getExportFilters());
                            break;
                        case IMPORTS:
                            parseFilterList(reader, dependency.getImportFilters());
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseResources(final DeploymentUnit deploymentUnit, final XMLStreamReader reader,
                                       final ModuleStructureSpec specBuilder) throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case RESOURCE_ROOT: {
                            parseResourceRoot(deploymentUnit, reader, specBuilder);
                            break;
                        }
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseResourceRoot(final DeploymentUnit deploymentUnit, final XMLStreamReader reader,
                                          final ModuleStructureSpec specBuilder) throws XMLStreamException {
        String name = null;
        String path = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case PATH:
                    path = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        if (name == null)
            name = path;
        List<FilterSpecification> resourceFilters = new ArrayList<FilterSpecification>();
        final Set<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (path.startsWith("/")) {
                        throw new XMLStreamException(
                                "External resource roots not supported, resource roots may not start with a '/' :" + path);
                    } else {
                        try {
                            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                            final VirtualFile deploymentRootFile = deploymentRoot.getRoot();
                            VirtualFile child = deploymentRootFile.getChild(path);
                            final Closeable closable = child.isFile() ? VFS.mountZip(child, child, TempFileProviderService
                                    .provider()) : null;
                            final MountHandle mountHandle = new MountHandle(closable);
                            ResourceRoot resourceRoot = new ResourceRoot(name, child, mountHandle);
                            for (FilterSpecification filter : resourceFilters) {
                                resourceRoot.getExportFilters().add(filter);
                            }
                            specBuilder.addResourceRoot(resourceRoot);
                        } catch (IOException e) {
                            throw new XMLStreamException(e);
                        }
                    }
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    if (!encountered.add(element))
                        throw unexpectedContent(reader);
                    switch (element) {
                        case FILTER:
                            parseFilterList(reader, resourceFilters);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    // not reached
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
    }

    private static void parseFilterList(final XMLStreamReader reader, final List<FilterSpecification> filters)
            throws XMLStreamException {
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case INCLUDE:
                            parsePath(reader, true, filters);
                            break;
                        case EXCLUDE:
                            parsePath(reader, false, filters);
                            break;
                        case INCLUDE_SET:
                            parseSet(reader, true, filters);
                            break;
                        case EXCLUDE_SET:
                            parseSet(reader, false, filters);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parsePath(final XMLStreamReader reader, final boolean include, final List<FilterSpecification> filters)
            throws XMLStreamException {
        String path = null;
        final Set<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATH:
                    path = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        final boolean literal = path.indexOf('*') == -1 && path.indexOf('?') == -1;
        if (literal) {
            if (path.charAt(path.length() - 1) == '/') {
                filters.add(new FilterSpecification(PathFilters.isChildOf(path), include));
            } else {
                filters.add(new FilterSpecification(PathFilters.is(path), include));
            }
        } else {
            filters.add(new FilterSpecification(PathFilters.match(path), include));
        }

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseSet(final XMLStreamReader reader, final boolean include, final List<FilterSpecification> filters)
            throws XMLStreamException {
        final Set<String> set = new HashSet<String>();
        // xsd:choice
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    filters.add(new FilterSpecification(PathFilters.in(set), include));
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case PATH:
                            parsePathName(reader, set);
                            break;
                    }
                }
            }
        }
    }

    private static void parsePathName(final XMLStreamReader reader, final Set<String> set) throws XMLStreamException {
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        set.add(name);

        // consume remainder of element
        parseNoContent(reader);
    }

    private static void parseTransformers(XMLStreamReader reader, ModuleStructureSpec moduleSpec) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case TRANSFORMER:
                            parseTransformer(reader, moduleSpec.getClassFileTransformers());
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseTransformer(XMLStreamReader reader, List<String> transformerClassNames) throws XMLStreamException {
        String className = null;
        final Set<Attribute> required = EnumSet.of(Attribute.CLASS);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case CLASS:
                    className = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        transformerClassNames.add(className);

        // consume remainder of element
        parseNoContent(reader);
    }


    private static void parseNoContent(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseEndDocument(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_DOCUMENT: {
                    return;
                }
                case XMLStreamConstants.CHARACTERS: {
                    if (!reader.isWhiteSpace()) {
                        throw unexpectedContent(reader);
                    }
                    // ignore
                    break;
                }
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE: {
                    // ignore
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        return;
    }


    private static void parseExclusions(final XMLStreamReader reader, final ModuleStructureSpec specBuilder) throws XMLStreamException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.of(reader.getName())) {
                        case MODULE:
                            parseModuleExclusion(reader, specBuilder);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    private static void parseModuleExclusion(final XMLStreamReader reader, final ModuleStructureSpec specBuilder) throws XMLStreamException {
        String name = null;
        String slot = "main";
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case SLOT:
                    slot = reader.getAttributeValue(i);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        specBuilder.getExclusions().add(ModuleIdentifier.create(name, slot));
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                default:
                    unexpectedContent(reader);
            }
        }
    }
}
