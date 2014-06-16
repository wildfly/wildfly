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

package org.wildfly.build.plugin.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.plugin.BuildPropertyReplacer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
class BuildModelParser10 implements XMLElementReader<Build> {


    private final BuildPropertyReplacer propertyReplacer;

    public static final String NAMESPACE_1_0 = "urn:wildfly:build:1.0";

    enum Element {
        BUILD,
        SERVER,
        SERVERS,
        ARTIFACT,
        PATH,
        FILES,
        FILTER,
        MODULES,
        CONFIG,
        STANDALONE,
        DOMAIN,
        COPY_ARTIFACT,
        ARTIFACTS,
        FILE_PERMISSIONS,
        PERMISSION,
        PROPERTY,
        MKDIRS,
        DIR,
        LINE_ENDINGS,
        WINDOWS,
        UNIX,


        // default unknown element
        UNKNOWN;

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, "build"), Element.BUILD);
            elementsMap.put(new QName(NAMESPACE_1_0, "server"), Element.SERVER);
            elementsMap.put(new QName(NAMESPACE_1_0, "servers"), Element.SERVERS);
            elementsMap.put(new QName(NAMESPACE_1_0, "artifact"), Element.ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, "path"), Element.PATH);
            elementsMap.put(new QName(NAMESPACE_1_0, "files"), Element.FILES);
            elementsMap.put(new QName(NAMESPACE_1_0, "filter"), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, "modules"), Element.MODULES);
            elementsMap.put(new QName(NAMESPACE_1_0, "config"), Element.CONFIG);
            elementsMap.put(new QName(NAMESPACE_1_0, "copy-artifact"), Element.COPY_ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, "standalone"), Element.STANDALONE);
            elementsMap.put(new QName(NAMESPACE_1_0, "domain"), Element.DOMAIN);
            elementsMap.put(new QName(NAMESPACE_1_0, "artifacts"), Element.ARTIFACTS);
            elementsMap.put(new QName(NAMESPACE_1_0, "file-permissions"), Element.FILE_PERMISSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, "permission"), Element.PERMISSION);
            elementsMap.put(new QName(NAMESPACE_1_0, "property"), Element.PROPERTY);
            elementsMap.put(new QName(NAMESPACE_1_0, "mkdirs"), Element.MKDIRS);
            elementsMap.put(new QName(NAMESPACE_1_0, "dir"), Element.DIR);
            elementsMap.put(new QName(NAMESPACE_1_0, "line-endings"), Element.LINE_ENDINGS);
            elementsMap.put(new QName(NAMESPACE_1_0, "windows"), Element.WINDOWS);
            elementsMap.put(new QName(NAMESPACE_1_0, "unix"), Element.UNIX);
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
        NAME, PATTERN, INCLUDE, TRANSITIVE,
        ARTIFACT, TO_LOCATION, EXTRACT, EXTRACT_SCHEMA, TEMPLATE, SUBSYSTEMS, OUTPUT_FILE,
        COPY_MODULE_ARTIFACTS, VALUE,

        // default unknown attribute
        UNKNOWN;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName("name"), NAME);
            attributesMap.put(new QName("pattern"), PATTERN);
            attributesMap.put(new QName("include"), INCLUDE);
            attributesMap.put(new QName("transitive"), TRANSITIVE);
            attributesMap.put(new QName("artifact"), ARTIFACT);
            attributesMap.put(new QName("to-location"), TO_LOCATION);
            attributesMap.put(new QName("extract-schema"), EXTRACT_SCHEMA);
            attributesMap.put(new QName("extract"), EXTRACT);
            attributesMap.put(new QName("template"), TEMPLATE);
            attributesMap.put(new QName("subsystems"), SUBSYSTEMS);
            attributesMap.put(new QName("output-file"), OUTPUT_FILE);
            attributesMap.put(new QName("copy-module-artifacts"), COPY_MODULE_ARTIFACTS);
            attributesMap.put(new QName("value"), VALUE);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    BuildModelParser10(Properties properties) {
        this.propertyReplacer = new BuildPropertyReplacer(properties);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final Build result) throws XMLStreamException {
        boolean extractSchema = true;
        boolean copyModuleArtifacts = true;
        final Set<Attribute> required = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case EXTRACT_SCHEMA:
                    extractSchema = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                case COPY_MODULE_ARTIFACTS:
                    copyModuleArtifacts = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        result.setExtractSchema(extractSchema);
        result.setCopyModuleArtifacts(copyModuleArtifacts);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());

                    switch (element) {
                        case SERVERS:
                            parseServers(reader, result);
                            break;
                        case ARTIFACTS:
                            parseArtifacts(reader, result);
                            break;
                        case CONFIG:
                            parseConfig(reader, result);
                            break;
                        case FILE_PERMISSIONS:
                            parseFilePermissions(reader, result);
                            break;
                        case MKDIRS:
                            parseMkdirs(reader, result);
                            break;
                        case LINE_ENDINGS:
                            parseLineEndings(reader, result);
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

    private void parseCopyArtifact(XMLStreamReader reader, Build result) throws XMLStreamException {
        String artifact = null;
        String location = null;
        boolean extract = false;
        final Set<Attribute> required = EnumSet.of(Attribute.ARTIFACT, Attribute.TO_LOCATION);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case ARTIFACT:
                    artifact = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case TO_LOCATION:
                    location = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case EXTRACT:
                    extract = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        CopyArtifact copyArtifact = new CopyArtifact(artifact, location, extract);
        result.getCopyArtifacts().add(copyArtifact);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, copyArtifact.getFilters());
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


    private void parseLineEnding(XMLStreamReader reader, LineEndings result) throws XMLStreamException {
        if(reader.getAttributeCount() != 0) {
            throw unexpectedContent(reader);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, result.getFilters());
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

    private void parsePermission(XMLStreamReader reader, Build result) throws XMLStreamException {
        String permission = null;
        final Set<Attribute> required = EnumSet.of(Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case VALUE:
                    permission = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        FilePermission filePermission = new FilePermission(permission);
        result.getFilePermissions().add(filePermission);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, filePermission.getFilters());
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

    private void parseServers(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case SERVER:
                            Server server = new Server();
                            result.getServers().add(server);
                            parseServer(reader, server);
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

    private void parseArtifacts(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case COPY_ARTIFACT:
                            parseCopyArtifact(reader, result);
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


    private void parseLineEndings(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case WINDOWS:
                            parseLineEnding(reader, result.getWindows());
                            break;
                        case UNIX:
                            parseLineEnding(reader, result.getUnix());
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


    private void parseServer(final XMLStreamReader reader, final Server result) throws XMLStreamException {
        Set<Element> visited = EnumSet.noneOf(Element.class);
        boolean locationSpecified = false;
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
                        case PATH:
                            if (locationSpecified) {
                                throw new XMLStreamException("<server> can only have a single <path> or <artifact> element");
                            }
                            locationSpecified = true;
                            result.setPath(parseName(reader));
                            break;
                        case ARTIFACT:
                            if (locationSpecified) {
                                throw new XMLStreamException("<server> can only have a single <path> or <artifact> element");
                            }
                            locationSpecified = true;
                            result.setArtifact(parseName(reader));
                            break;
                        case FILES:
                            parseFiles(reader, result);
                            break;
                        case MODULES:
                            parseModules(reader, result);
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

    private String parseName(final XMLStreamReader reader) throws XMLStreamException {
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
        parseNoContent(reader);
        return propertyReplacer.replaceProperties(name);
    }

    private void parseFiles(final XMLStreamReader reader, Server result) throws XMLStreamException {
        // xsd:all
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, result.getFilters());
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

    private void parseFilter(XMLStreamReader reader, List<FileFilter> filters) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
        final Set<Attribute> required = EnumSet.of(Attribute.PATTERN, Attribute.INCLUDE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATTERN:
                    pattern = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        parseNoContent(reader);

        filters.add(new FileFilter(wildcardToJavaRegexp(pattern), include));
    }


    private void parseModules(final XMLStreamReader reader, Server result) throws XMLStreamException {
        // xsd:all
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseModulesFilter(reader, result);
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

    private void parseModulesFilter(XMLStreamReader reader, Server result) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
        boolean transitive = true;
        final Set<Attribute> required = EnumSet.of(Attribute.PATTERN, Attribute.INCLUDE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATTERN:
                    pattern = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                case TRANSITIVE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        parseNoContent(reader);

        result.getModules().add(new ModuleFilter(wildcardToJavaRegexp(pattern), include, transitive));

    }


    private void parseConfig(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case STANDALONE:
                            parseConfigFile(reader, result, false);
                            break;
                        case DOMAIN:
                            parseConfigFile(reader, result, true);
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

    private void parseConfigFile(XMLStreamReader reader, Build result, boolean domain) throws XMLStreamException {
        String template = null;
        String subsystems = null;
        String outputFile = null;
        final Set<Attribute> required = EnumSet.of(Attribute.TEMPLATE, Attribute.SUBSYSTEMS, Attribute.OUTPUT_FILE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case TEMPLATE:
                    template = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case SUBSYSTEMS:
                    subsystems = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case OUTPUT_FILE:
                    outputFile = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        ConfigFile file = new ConfigFile(template, subsystems, outputFile);

        parseProperties(reader, file.getProperties());
        if (domain) {
            result.getDomainConfigs().add(file);
        } else {
            result.getStandaloneConfigs().add(file);
        }
    }


    private void parseMkdirs(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DIR:
                            result.getMkDirs().add(parseName(reader));
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

    private void parseProperties(XMLStreamReader reader, Properties properties) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PROPERTY:
                            parseProperty(reader, properties);
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

    private void parseProperty(XMLStreamReader reader, Properties result) throws XMLStreamException {
        String name = null;
        String value = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VALUE:
                    value = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }

        parseNoContent(reader);

        result.put(name, value);

    }

    private void parseFilePermissions(final XMLStreamReader reader, final Build result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PERMISSION:
                            parsePermission(reader, result);
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

        return new XMLStreamException("unexpected content: " + kind + (reader.hasName() ? reader.getName() : null) +
                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document ", location);
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder();
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException("Missing required attributes " + b.toString(), location);
    }


    private static String wildcardToJavaRegexp(String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }
}
