/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.web.Constants.*;
import static org.jboss.as.web.WebExtension.ACCESS_LOG_PATH;
import static org.jboss.as.web.WebExtension.DIRECTORY_PATH;
import static org.jboss.as.web.WebExtension.SSL_PATH;
import static org.jboss.as.web.WebExtension.SSO_PATH;

/**
 * The web subsystem parser.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 * @author Tomaz Cerar
 */
class WebSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final WebSubsystemParser INSTANCE = new WebSubsystemParser();

    static WebSubsystemParser getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        WebDefinition.DEFAULT_VIRTUAL_SERVER.marshallAsAttribute(node, true, writer);
        WebDefinition.NATIVE.marshallAsAttribute(node, false, writer);
        WebDefinition.INSTANCE_ID.marshallAsAttribute(node, false, writer);
        if (node.hasDefined(CONFIGURATION)) {
            writeContainerConfig(writer, node.get(CONFIGURATION));
        }
        if (node.hasDefined(CONNECTOR)) {
            for (final Property connector : node.get(CONNECTOR).asPropertyList()) {
                final ModelNode config = connector.getValue();
                writer.writeStartElement(Element.CONNECTOR.getLocalName());
                writer.writeAttribute(NAME, connector.getName());
                for (SimpleAttributeDefinition attr : WebConnectorDefinition.CONNECTOR_ATTRIBUTES) {
                    attr.marshallAsAttribute(config, false, writer);
                }
                if (config.get(SSL_PATH.getKey(), SSL_PATH.getValue()).isDefined()) {
                    ModelNode sslConfig = config.get(SSL_PATH.getKey(), SSL_PATH.getValue());
                    writer.writeStartElement(Element.SSL.getLocalName());
                    for (SimpleAttributeDefinition attr : WebSSLDefinition.SSL_ATTRIBUTES) {
                        attr.marshallAsAttribute(sslConfig, false, writer);
                    }
                    writer.writeEndElement();
                }
                if (config.hasDefined(VIRTUAL_SERVER)) {
                    for (final ModelNode virtualServer : config.get(VIRTUAL_SERVER).asList()) {
                        writer.writeEmptyElement(VIRTUAL_SERVER);
                        writer.writeAttribute(NAME, virtualServer.asString());
                    }
                }

                writer.writeEndElement();
            }
        }
        if (node.hasDefined(VIRTUAL_SERVER)) {
            for (final Property host : node.get(VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode config = host.getValue();
                writer.writeStartElement(Element.VIRTUAL_SERVER.getLocalName());
                writer.writeAttribute(NAME, host.getName());
                WebVirtualHostDefinition.DEFAULT_WEB_MODULE.marshallAsAttribute(config, true, writer);
                WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.marshallAsAttribute(config, true, writer);

                if (config.hasDefined(ALIAS)) {
                    for (final ModelNode alias : config.get(ALIAS).asList()) {
                        writer.writeEmptyElement(ALIAS);
                        writer.writeAttribute(NAME, alias.asString());
                    }
                }
                if (config.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue()).isDefined()) {
                    ModelNode accessLog = config.get(ACCESS_LOG_PATH.getKey(), ACCESS_LOG_PATH.getValue());
                    writer.writeStartElement(Element.ACCESS_LOG.getLocalName());


                    for (SimpleAttributeDefinition attr : WebAccessLogDefinition.ACCESS_LOG_ATTRIBUTES) {
                        attr.marshallAsAttribute(accessLog, false, writer);
                    }

                    if (accessLog.get(DIRECTORY_PATH.getKey(), DIRECTORY_PATH.getValue()).isDefined()) {
                        ModelNode directory = accessLog.get(DIRECTORY_PATH.getKey(), DIRECTORY_PATH.getValue());
                        String name = Element.DIRECTORY.getLocalName();
                        boolean startwritten = false;
                        startwritten = writeAttribute(writer, WebAccessLogDirectoryDefinition.PATH, directory, startwritten, name);
                        startwritten = writeAttribute(writer, WebAccessLogDirectoryDefinition.RELATIVE_TO, directory, startwritten, name);
                        if (startwritten) { writer.writeEndElement(); }
                    }
                    writer.writeEndElement();
                }

                if (config.hasDefined(REWRITE)) {
                    for (final ModelNode rewritenode : config.get(REWRITE).asList()) {
                        ModelNode rewrite = rewritenode.asProperty().getValue();
                        writer.writeStartElement(REWRITE);
                        WebReWriteDefinition.PATTERN.marshallAsAttribute(rewrite, false, writer);
                        WebReWriteDefinition.SUBSTITUTION.marshallAsAttribute(rewrite, false, writer);
                        WebReWriteDefinition.FLAGS.marshallAsAttribute(rewrite, false, writer);

                        if (rewrite.hasDefined(CONDITION)) {
                            for (final ModelNode conditionnode : rewrite.get(CONDITION).asList()) {
                                ModelNode condition = conditionnode.asProperty().getValue();
                                writer.writeStartElement(CONDITION);
                                WebReWriteConditionDefinition.TEST.marshallAsAttribute(condition, false, writer);
                                WebReWriteConditionDefinition.PATTERN.marshallAsAttribute(condition, false, writer);
                                WebReWriteConditionDefinition.FLAGS.marshallAsAttribute(condition, false, writer);
                                writer.writeEndElement();
                            }
                        }
                        writer.writeEndElement();
                    }
                }

                if (config.get(SSO_PATH.getKey(), SSO_PATH.getValue()).isDefined()) {
                    final ModelNode sso;
                    sso = config.get(SSO_PATH.getKey(), SSO_PATH.getValue());
                    writer.writeStartElement(SSO);
                    for (SimpleAttributeDefinition attr : WebSSODefinition.SSO_ATTRIBUTES) {
                        attr.marshallAsAttribute(sso, false, writer);
                    }
                    writer.writeEndElement();
                }

                // End of the VIRTUAL_SERVER
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }


    private void writeContainerConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        boolean containerConfigStartWritten = false;
        if (config.hasDefined(STATIC_RESOURCES)) {
            containerConfigStartWritten = writeStaticResources(writer, config.get(STATIC_RESOURCES));
        }
        if (config.hasDefined(JSP_CONFIGURATION)) {
            containerConfigStartWritten = writeJSPConfiguration(writer, config.get(JSP_CONFIGURATION), containerConfigStartWritten) || containerConfigStartWritten;
        }
        ModelNode container = config;
        if (config.hasDefined(CONTAINER)) {
            // this has been added to get the stuff manageable
            container = config.get(CONTAINER);
        }
        if (container.hasDefined(MIME_MAPPING)) {
            if (!containerConfigStartWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                containerConfigStartWritten = true;
            }
            for (final Property entry : container.get(MIME_MAPPING).asPropertyList()) {
                writer.writeEmptyElement(Element.MIME_MAPPING.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().asString());
            }
        }
        if (container.hasDefined(WELCOME_FILE)) {
            if (!containerConfigStartWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                containerConfigStartWritten = true;
            }
            for (final ModelNode file : container.get(WELCOME_FILE).asList()) {
                writer.writeStartElement(Element.WELCOME_FILE.getLocalName());
                writer.writeCharacters(file.asString());
                writer.writeEndElement();
            }
        }
        if (containerConfigStartWritten) {
            writer.writeEndElement();
        }
    }

    private boolean writeStaticResources(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {

        boolean startWritten = false;
        for (SimpleAttributeDefinition def : WebStaticResources.STATIC_ATTRIBUTES) {
            startWritten = writeStaticResourceAttribute(writer, def, config, startWritten) || startWritten;
        }
        if (startWritten) {
            writer.writeEndElement();
        }
        return startWritten;
    }

    private boolean writeStaticResourceAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute, ModelNode config, boolean startWritten) throws XMLStreamException {
        if (attribute.isMarshallable(config, false)) {
            if (!startWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                writer.writeStartElement(Element.STATIC_RESOURCES.getLocalName());
            }
            attribute.marshallAsAttribute(config, false, writer);
            return true;
        }
        return false;
    }

    private boolean writeJSPConfiguration(XMLExtendedStreamWriter writer, ModelNode jsp, boolean containerConfigStartWritten) throws XMLStreamException {

        boolean startWritten = false;
        for (SimpleAttributeDefinition def : WebJSPDefinition.JSP_ATTRIBUTES) {
            startWritten = writeJspConfigAttribute(writer, def, jsp, startWritten, containerConfigStartWritten) || startWritten;
        }

        if (startWritten) {
            writer.writeEndElement();
        }

        return startWritten;
    }

    private boolean writeJspConfigAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute, ModelNode config,
                                            boolean startWritten, boolean containerConfigStartWritten) throws XMLStreamException {
        if (attribute.isMarshallable(config, false)) {
            if (!startWritten) {
                if (!containerConfigStartWritten) {
                    writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                }
                writer.writeStartElement(Element.JSP_CONFIGURATION.getLocalName());
            }
            attribute.marshallAsAttribute(config, false, writer);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME));


        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NATIVE:
                case DEFAULT_VIRTUAL_SERVER:
                case INSTANCE_ID:
                    subsystem.get(attribute.getLocalName()).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        list.add(subsystem);
        boolean containerConfigDefined = false;
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONTAINER_CONFIG: {
                            parseContainerConfig(reader, address, list);
                            containerConfigDefined = true;
                            break;
                        }
                        case CONNECTOR: {
                            parseConnector(reader, address, list);
                            break;
                        }
                        case VIRTUAL_SERVER: {
                            parseHost(reader, address, list);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!containerConfigDefined) {
            addDefaultContainerConfig(address, list);
        }

    }

    static void addDefaultContainerConfig(final PathAddress parent, List<ModelNode> list) {
        final ModelNode config = new ModelNode();
        PathAddress containerPath = PathAddress.pathAddress(parent, WebExtension.CONTAINER_PATH);
        config.get(OP).set(ADD);
        config.get(OP_ADDR).set(containerPath.toModelNode());
        list.add(config);
        addDefaultStaticConfiguration(parent, list);
        addDefaultJSPConfiguration(parent, list);

    }

    private static void addDefaultJSPConfiguration(final PathAddress parent, List<ModelNode> list) {
        final PathAddress jspAddress = PathAddress.pathAddress(parent, WebExtension.JSP_CONFIGURATION_PATH);
        final ModelNode jsp = new ModelNode();
        jsp.get(OP).set(ADD);
        jsp.get(OP_ADDR).set(jspAddress.toModelNode());
        list.add(jsp);
    }

    private static void addDefaultStaticConfiguration(final PathAddress parent, List<ModelNode> list) {
        PathAddress address = PathAddress.pathAddress(parent, WebExtension.STATIC_RESOURCES_PATH);
        final ModelNode resources = new ModelNode();
        resources.get(OP).set(ADD);
        resources.get(OP_ADDR).set(address.toModelNode());
        list.add(resources);
    }

    static void parseContainerConfig(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list) throws XMLStreamException {

        PathAddress address = PathAddress.pathAddress(parent, WebExtension.CONTAINER_PATH);
        final ModelNode config = new ModelNode();
        config.get(OP).set(ADD);
        config.get(OP_ADDR).set(address.toModelNode());
        // no attributes
        list.add(config);
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        // elements
        boolean staticResourcesConfigured = false;
        boolean jspConfigured = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STATIC_RESOURCES: {
                    parseStaticResources(reader, parent, list);
                    staticResourcesConfigured = true;
                    break;
                }
                case JSP_CONFIGURATION: {
                    parseJSPConfiguration(reader, parent, list);
                    jspConfigured = true;
                    break;
                }
                case MIME_MAPPING: {
                    //todo maybe create via add-mime
                    final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
                    config.get(MIME_MAPPING).get(array[0]).set(array[1]);

                    //config.get(MIME_MAPPING).add(mimeMapping);
                    requireNoContent(reader);
                    break;
                }
                case WELCOME_FILE: {
                    final String welcomeFile = reader.getElementText().trim();
                    config.get(WELCOME_FILE).add(welcomeFile);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        if (!staticResourcesConfigured) {
            addDefaultStaticConfiguration(parent, list);
        }
        if (!jspConfigured) {
            addDefaultJSPConfiguration(parent, list);
        }

    }

    static void parseJSPConfiguration(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(parent, WebExtension.JSP_CONFIGURATION_PATH);


        final ModelNode jsp = new ModelNode();
        jsp.get(OP).set(ADD);
        jsp.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEVELOPMENT:
                case DISABLED:
                case KEEP_GENERATED:
                case TRIM_SPACES:
                case TAG_POOLING:
                case MAPPED_FILE:
                case CHECK_INTERVAL:
                case MODIFICATION_TEST_INTERVAL:
                case RECOMPILE_ON_FAIL:
                case SMAP:
                case DUMP_SMAP:
                case GENERATE_STRINGS_AS_CHAR_ARRAYS:
                case ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE:
                case SCRATCH_DIR:
                case SOURCE_VM:
                case TARGET_VM:
                case JAVA_ENCODING:
                case X_POWERED_BY:
                case DISPLAY_SOURCE_FRAGMENT:
                    jsp.get(attribute.getLocalName()).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(jsp);
    }

    static void parseStaticResources(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(parent, WebExtension.STATIC_RESOURCES_PATH);
        final ModelNode resources = new ModelNode();
        resources.get(OP).set(ADD);
        resources.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case LISTINGS:
                    WebStaticResources.LISTINGS.parseAndSetParameter(value, resources, reader);
                    break;
                case SENDFILE:
                    WebStaticResources.SENDFILE.parseAndSetParameter(value, resources, reader);
                    break;
                case FILE_ENCODING:
                    WebStaticResources.FILE_ENCODING.parseAndSetParameter(value, resources, reader);
                    break;
                case READ_ONLY:
                    WebStaticResources.READ_ONLY.parseAndSetParameter(value, resources, reader);
                    break;
                case WEBDAV:
                    WebStaticResources.WEBDAV.parseAndSetParameter(value, resources, reader);
                    break;
                case SECRET:
                    WebStaticResources.SECRET.parseAndSetParameter(value, resources, reader);
                    break;
                case MAX_DEPTH:
                    WebStaticResources.MAX_DEPTH.parseAndSetParameter(value, resources, reader);
                    break;
                case DISABLED:
                    WebStaticResources.DISABLED.parseAndSetParameter(value, resources, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(resources);
    }

    static void parseHost(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        final ModelNode host = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case DEFAULT_WEB_MODULE:
                    WebVirtualHostDefinition.DEFAULT_WEB_MODULE.parseAndSetParameter(value, host, reader);
                    break;
                case ENABLE_WELCOME_ROOT:
                    WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.parseAndSetParameter(value, host, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        final PathAddress address = PathAddress.pathAddress(parent, PathElement.pathElement(VIRTUAL_SERVER, name));

        host.get(OP).set(ADD);
        host.get(OP_ADDR).set(address.toModelNode());
        list.add(host);
        int rewriteCount = 0;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {

            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ALIAS:
                            host.get(ALIAS).add(readStringAttributeElement(reader, Attribute.NAME.getLocalName()));
                            break;
                        case ACCESS_LOG:
                            parseHostAccessLog(reader, address, list);
                            break;
                        case REWRITE:
                            parseHostRewrite(reader, address, list, ++rewriteCount);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                case WEB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ALIAS:
                            host.get(ALIAS).add(readStringAttributeElement(reader, Attribute.NAME.getLocalName()));
                            break;
                        case ACCESS_LOG:
                            parseHostAccessLog(reader, address, list);
                            break;
                        case REWRITE:
                            parseHostRewrite(reader, address, list, ++rewriteCount);
                            break;
                        case SSO:
                            parseSso(reader, address, list);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    static void parseSso(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(parent, WebExtension.SSO_PATH);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE_CONTAINER:
                case CACHE_NAME:
                case DOMAIN:
                case REAUTHENTICATE:
                    operation.get(attribute.getLocalName()).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(operation);
    }

    static void parseHostRewrite(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list, int rewriteCount) throws XMLStreamException {
        final PathAddress address = PathAddress.pathAddress(parent, PathElement.pathElement(REWRITE, "rule-" + rewriteCount));

        final ModelNode rewrite = new ModelNode();
        rewrite.get(OP).set(ADD);
        rewrite.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATTERN:
                    rewrite.get(PATTERN).set(value);
                    break;
                case SUBSTITUTION:
                    rewrite.get(SUBSTITUTION).set(value);
                    break;
                case FLAGS:
                    rewrite.get(FLAGS).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        list.add(rewrite);
        int conditionCount = 0;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONDITION:

                            PathAddress condAddress = PathAddress.pathAddress(address);
                            final ModelNode condition = new ModelNode();
                            condition.get(OP).set(ADD);
                            condAddress = condAddress.append(PathElement.pathElement(CONDITION, "condition-" + conditionCount));
                            condition.get(OP_ADDR).set(condAddress.toModelNode());
                            final int count2 = reader.getAttributeCount();
                            for (int i = 0; i < count2; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final String value = reader.getAttributeValue(i);
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case TEST:
                                        condition.get(TEST).set(value);
                                        break;
                                    case PATTERN:
                                        condition.get(PATTERN).set(value);
                                        break;
                                    case FLAGS:
                                        condition.get(FLAGS).set(value);
                                        break;
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            requireNoContent(reader);
                            list.add(condition);
                            conditionCount++;
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    static void parseHostAccessLog(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(parent, ACCESS_LOG_PATH);
        final ModelNode log = new ModelNode();
        log.get(OP).set(ADD);
        log.get(OP_ADDR).set(address.toModelNode());

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATTERN:
                    log.get(PATTERN).set(value);
                    break;
                case RESOLVE_HOSTS:
                    log.get(RESOLVE_HOSTS).set(value);
                    break;
                case EXTENDED:
                    log.get(EXTENDED).set(value);
                    break;
                case PREFIX:
                    log.get(PREFIX).set(value);
                    break;
                case ROTATE:
                    log.get(ROTATE).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        list.add(log);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DIRECTORY:
                            final PathAddress dirAddress = PathAddress.pathAddress(address, WebExtension.DIRECTORY_PATH);
                            final ModelNode directory = new ModelNode();
                            directory.get(OP).set(ADD);
                            directory.get(OP_ADDR).set(dirAddress.toModelNode());
                            final int count2 = reader.getAttributeCount();
                            for (int i = 0; i < count2; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final String value = reader.getAttributeValue(i);
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case PATH:
                                        directory.get(PATH).set(value);
                                        break;
                                    case RELATIVE_TO:
                                        directory.get(RELATIVE_TO).set(value);
                                        break;
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            requireNoContent(reader);
                            list.add(directory);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    static void parseConnector(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        final ModelNode connector = new ModelNode();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case SOCKET_BINDING:
                    WebConnectorDefinition.SOCKET_BINDING.parseAndSetParameter(value, connector, reader);
                    break;
                case SCHEME:
                    WebConnectorDefinition.SCHEME.parseAndSetParameter(value, connector, reader);
                    break;
                case PROTOCOL:
                    WebConnectorDefinition.PROTOCOL.parseAndSetParameter(value, connector, reader);
                    break;
                case EXECUTOR:
                    WebConnectorDefinition.EXECUTOR.parseAndSetParameter(value, connector, reader);
                    break;
                case ENABLED:
                    WebConnectorDefinition.ENABLED.parseAndSetParameter(value, connector, reader);
                    break;
                case ENABLE_LOOKUPS:
                    WebConnectorDefinition.ENABLE_LOOKUPS.parseAndSetParameter(value, connector, reader);
                    break;
                case PROXY_NAME:
                    WebConnectorDefinition.PROXY_NAME.parseAndSetParameter(value, connector, reader);
                    break;
                case PROXY_PORT:
                    WebConnectorDefinition.PROXY_PORT.parseAndSetParameter(value, connector, reader);
                    break;
                case MAX_POST_SIZE:
                    WebConnectorDefinition.MAX_POST_SIZE.parseAndSetParameter(value, connector, reader);
                    break;
                case MAX_SAVE_POST_SIZE:
                    WebConnectorDefinition.MAX_SAVE_POST_SIZE.parseAndSetParameter(value, connector, reader);
                    break;
                case SECURE:
                    WebConnectorDefinition.SECURE.parseAndSetParameter(value, connector, reader);
                    break;
                case REDIRECT_PORT:
                    WebConnectorDefinition.REDIRECT_PORT.parseAndSetParameter(value, connector, reader);
                    break;
                case MAX_CONNECTIONS:
                    WebConnectorDefinition.MAX_CONNECTIONS.parseAndSetParameter(value, connector, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        connector.get(OP).set(ADD);
        PathAddress address = PathAddress.pathAddress(parent, PathElement.pathElement(CONNECTOR, name));
        connector.get(OP_ADDR).set(address.toModelNode());
        list.add(connector);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SSL:
                            parseSsl(reader, address, list);
                            break;
                        case VIRTUAL_SERVER:
                            connector.get(VIRTUAL_SERVER).add(readStringAttributeElement(reader, Attribute.NAME.getLocalName()));
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }


    }

    static void parseSsl(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(parent, WebExtension.SSL_PATH);
        final ModelNode ssl = new ModelNode();
        ssl.get(OP).set(ADD);
        ssl.get(OP_ADDR).set(address.toModelNode());
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    WebSSLDefinition.NAME.parseAndSetParameter(value, ssl, reader);
                    break;
                case KEY_ALIAS:
                    WebSSLDefinition.KEY_ALIAS.parseAndSetParameter(value, ssl, reader);
                    break;
                case PASSWORD:
                    WebSSLDefinition.PASSWORD.parseAndSetParameter(value, ssl, reader);
                    break;
                case CERTIFICATE_KEY_FILE:
                    WebSSLDefinition.CERTIFICATE_KEY_FILE.parseAndSetParameter(value, ssl, reader);
                    break;
                case CIPHER_SUITE:
                    WebSSLDefinition.CIPHER_SUITE.parseAndSetParameter(value, ssl, reader);
                    break;
                case PROTOCOL:
                    WebSSLDefinition.PROTOCOL.parseAndSetParameter(value, ssl, reader);
                    break;
                case VERIFY_CLIENT:
                    WebSSLDefinition.VERIFY_CLIENT.parseAndSetParameter(value, ssl, reader);
                    break;
                case VERIFY_DEPTH:
                    WebSSLDefinition.VERIFY_DEPTH.parseAndSetParameter(value, ssl, reader);
                    break;
                case CERTIFICATE_FILE:
                    WebSSLDefinition.CERTIFICATE_FILE.parseAndSetParameter(value, ssl, reader);
                    break;
                case CA_CERTIFICATE_FILE:
                    WebSSLDefinition.CA_CERTIFICATE_FILE.parseAndSetParameter(value, ssl, reader);
                    break;
                case CA_REVOCATION_URL:
                    WebSSLDefinition.CA_REVOCATION_URL.parseAndSetParameter(value, ssl, reader);
                    break;
                case SESSION_CACHE_SIZE:
                    WebSSLDefinition.SESSION_CACHE_SIZE.parseAndSetParameter(value, ssl, reader);
                    break;
                case SESSION_TIMEOUT:
                    WebSSLDefinition.SESSION_TIMEOUT.parseAndSetParameter(value, ssl, reader);
                    break;
                case CA_CERTIFICATE_PASSWORD:
                    WebSSLDefinition.CA_CERTIFICATE_PASSWORD.parseAndSetParameter(value, ssl, reader);
                    break;
                case KEYSTORE_TYPE:
                    WebSSLDefinition.KEYSTORE_TYPE.parseAndSetParameter(value, ssl, reader);
                    break;
                case TRUSTSTORE_TYPE:
                    WebSSLDefinition.TRUSTSTORE_TYPE.parseAndSetParameter(value, ssl, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(ssl);
    }

    //todo,  attribute.marshallAsAttribute should return boolean
    private boolean writeAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute, ModelNode node, boolean startWriten, String origin) throws XMLStreamException {

        if (attribute.isMarshallable(node, false)) {
            if (!startWriten) {
                startWriten = true;
                writer.writeStartElement(origin);
            }
            attribute.marshallAsAttribute(node, false, writer);
        }
        return startWriten;
    }

}
