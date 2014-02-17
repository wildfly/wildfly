/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module.descriptor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.MountedDeploymentOverlay;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * @author Stuart Douglas
 */
public class JBossDeploymentStructureParser10 implements XMLElementReader<ParseResult> {

    public static final JBossDeploymentStructureParser10 INSTANCE = new JBossDeploymentStructureParser10();

    static final JBossAllXMLParser<ParseResult> JBOSS_ALL_XML_PARSER = new JBossAllXmlParserAdaptor(INSTANCE);

    public static final String NAMESPACE_1_0 = "urn:jboss:deployment-structure:1.0";

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
            elementsMap.put(new QName(NAMESPACE_1_0, "jboss-deployment-structure"), Element.JBOSS_DEPLOYMENT_STRUCTURE);
            elementsMap.put(new QName(NAMESPACE_1_0, "ear-subdeployments-isolated"), Element.EAR_SUBDEPLOYMENTS_ISOLATED);
            elementsMap.put(new QName(NAMESPACE_1_0, "deployment"), Element.DEPLOYMENT);
            elementsMap.put(new QName(NAMESPACE_1_0, "sub-deployment"), Element.SUB_DEPLOYMENT);
            elementsMap.put(new QName(NAMESPACE_1_0, "module"), Element.MODULE);
            elementsMap.put(new QName(NAMESPACE_1_0, "dependencies"), Element.DEPENDENCIES);
            elementsMap.put(new QName(NAMESPACE_1_0, "resources"), Element.RESOURCES);
            elementsMap.put(new QName(NAMESPACE_1_0, "resource-root"), Element.RESOURCE_ROOT);
            elementsMap.put(new QName(NAMESPACE_1_0, "path"), Element.PATH);
            elementsMap.put(new QName(NAMESPACE_1_0, "exports"), Element.EXPORTS);
            elementsMap.put(new QName(NAMESPACE_1_0, "imports"), Element.IMPORTS);
            elementsMap.put(new QName(NAMESPACE_1_0, "include"), Element.INCLUDE);
            elementsMap.put(new QName(NAMESPACE_1_0, "exclude"), Element.EXCLUDE);
            elementsMap.put(new QName(NAMESPACE_1_0, "exclusions"), Element.EXCLUSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, "include-set"), Element.INCLUDE_SET);
            elementsMap.put(new QName(NAMESPACE_1_0, "exclude-set"), Element.EXCLUDE_SET);
            elementsMap.put(new QName(NAMESPACE_1_0, "filter"), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, "transformers"), Element.TRANSFORMERS);
            elementsMap.put(new QName(NAMESPACE_1_0, "transformer"), Element.TRANSFORMER);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        NAME, SLOT, EXPORT, SERVICES, PATH, OPTIONAL, CLASS, VALUE,

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
            attributesMap.put(new QName("value"), VALUE);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    enum Disposition {
        NONE("none"),
        IMPORT("import"),
        EXPORT("export"),;

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

        static Disposition of(final String value) {
            final Disposition disposition = values.get(value);
            return disposition == null ? NONE : disposition;
        }
    }

    private JBossDeploymentStructureParser10() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final ParseResult result) throws XMLStreamException {
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
                                result.setEarSubDeploymentsIsolated(true);
                            } else {
                                result.setEarSubDeploymentsIsolated(Boolean.valueOf(value));
                            }
                            break;
                        case DEPLOYMENT:
                            if (deploymentVisited) {
                                throw unexpectedContent(reader);
                            }
                            deploymentVisited = true;
                            parseDeployment(reader, result);
                            break;
                        case SUB_DEPLOYMENT:
                            parseSubDeployment(reader, result);
                            break;
                        case MODULE:
                            parseModule(reader, result);
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


    private static void parseDeployment(final XMLStreamReader reader, final ParseResult result) throws XMLStreamException {
        result.setRootDeploymentSpecification(new ModuleStructureSpec());
        parseModuleStructureSpec(result.getDeploymentUnit(), reader, result.getRootDeploymentSpecification(), result.getModuleLoader());
    }

    private static void parseSubDeployment(final XMLStreamReader reader, final ParseResult result) throws XMLStreamException {
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
        if (result.getSubDeploymentSpecifications().containsKey(name)) {
            throw ServerMessages.MESSAGES.duplicateSubdeploymentListing(name);
        }
        final ModuleStructureSpec moduleSpecification = new ModuleStructureSpec();
        result.getSubDeploymentSpecifications().put(name, moduleSpecification);
        parseModuleStructureSpec(result.getDeploymentUnit(), reader, moduleSpecification, result.getModuleLoader());
    }

    private static void parseModule(final XMLStreamReader reader, final ParseResult result) throws XMLStreamException {
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
            throw ServerMessages.MESSAGES.invalidModuleName(name);
        }
        ModuleStructureSpec moduleSpecification = new ModuleStructureSpec();
        moduleSpecification.setModuleIdentifier(ModuleIdentifier.create(name, slot));
        result.getAdditionalModules().add(moduleSpecification);
        parseModuleStructureSpec(result.getDeploymentUnit(), reader, moduleSpecification, result.getModuleLoader());
    }

    private static void parseModuleStructureSpec(final DeploymentUnit deploymentUnit, final XMLStreamReader reader,
                                                 final ModuleStructureSpec moduleSpec, final ModuleLoader moduleLoader) throws XMLStreamException {
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
                services == Disposition.IMPORT, true);
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
                        throw ServerMessages.MESSAGES.externalResourceRootsNotSupported(path);
                    } else {
                        try {
                            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                            final VirtualFile deploymentRootFile = deploymentRoot.getRoot();
                            VirtualFile child = deploymentRootFile.getChild(path);
                            Map<String, MountedDeploymentOverlay> overlays = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_OVERLAY_LOCATIONS);
                            MountedDeploymentOverlay overlay = overlays.get(path);
                            Closeable closable = null;
                            if(overlay != null) {
                                overlay.remountAsZip(false);
                            } else if(child.isFile()) {
                                closable = VFS.mountZip(child, child, TempFileProviderService.provider());
                            }
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
                    break;
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

    private static void parseTransformers(final XMLStreamReader reader, final ModuleStructureSpec moduleSpec) throws XMLStreamException {
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

    private static void parseTransformer(final XMLStreamReader reader, final List<String> transformerClassNames) throws XMLStreamException {
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

        return ServerMessages.MESSAGES.unexpectedContent(kind, (reader.hasName() ? reader.getName() : null),
                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return ServerMessages.MESSAGES.unexpectedEndOfDocument(location);
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder();
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return ServerMessages.MESSAGES.missingRequiredAttributes(b.toString(), location);
    }
}
