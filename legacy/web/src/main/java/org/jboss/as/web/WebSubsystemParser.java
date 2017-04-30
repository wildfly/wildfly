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
import static org.jboss.as.web.Constants.ALIAS;
import static org.jboss.as.web.Constants.CONDITION;
import static org.jboss.as.web.Constants.CONFIGURATION;
import static org.jboss.as.web.Constants.CONNECTOR;
import static org.jboss.as.web.Constants.CONTAINER;
import static org.jboss.as.web.Constants.JSP_CONFIGURATION;
import static org.jboss.as.web.Constants.MIME_MAPPING;
import static org.jboss.as.web.Constants.NAME;
import static org.jboss.as.web.Constants.PARAM;
import static org.jboss.as.web.Constants.PATH;
import static org.jboss.as.web.Constants.RELATIVE_TO;
import static org.jboss.as.web.Constants.REWRITE;
import static org.jboss.as.web.Constants.SSO;
import static org.jboss.as.web.Constants.STATIC_RESOURCES;
import static org.jboss.as.web.Constants.VALVE;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;
import static org.jboss.as.web.Constants.WELCOME_FILE;
import static org.jboss.as.web.WebExtension.ACCESS_LOG_PATH;
import static org.jboss.as.web.WebExtension.DIRECTORY_PATH;
import static org.jboss.as.web.WebExtension.SSL_PATH;
import static org.jboss.as.web.WebExtension.SSO_PATH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The web subsystem parser.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 * @author Tomaz Cerar
 */
class WebSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    private static final String RULE_PREFIX = "rule-";
    private static final String CONDITION_PREFIX = "condition-";

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        WebDefinition.DEFAULT_VIRTUAL_SERVER.marshallAsAttribute(node, true, writer);
        WebDefinition.INSTANCE_ID.marshallAsAttribute(node, false, writer);
        WebDefinition.NATIVE.marshallAsAttribute(node, true, writer);
        WebDefinition.DEFAULT_SESSION_TIMEOUT.marshallAsAttribute(node, false, writer);
        if (node.hasDefined(CONFIGURATION)) {
            writeContainerConfig(writer, node.get(CONFIGURATION));
        }
        if (node.hasDefined(CONNECTOR)) {
            for (final Property connector : node.get(CONNECTOR).asPropertyList()) {
                final ModelNode config = connector.getValue();
                writer.writeStartElement(Element.CONNECTOR.getLocalName());
                writer.writeAttribute(NAME, connector.getName());
                 List<AttributeDefinition> connectorAttributes =  new ArrayList<>(Arrays.asList(WebConnectorDefinition.CONNECTOR_ATTRIBUTES));
                if(config.hasDefined(Constants.PROXY_BINDING)) {
                    connectorAttributes.remove(WebConnectorDefinition.PROXY_PORT);
                    connectorAttributes.remove(WebConnectorDefinition.PROXY_NAME);
                } else {
                    connectorAttributes.remove(WebConnectorDefinition.PROXY_BINDING);
                }
                if(config.hasDefined(Constants.REDIRECT_BINDING)) {
                    connectorAttributes.remove(WebConnectorDefinition.REDIRECT_PORT);
                } else {
                    connectorAttributes.remove(WebConnectorDefinition.REDIRECT_BINDING);
                }
                for (AttributeDefinition attr : connectorAttributes) {
                    if (attr instanceof SimpleAttributeDefinition) {
                        ((SimpleAttributeDefinition) attr).marshallAsAttribute(config, true, writer);
                    }
                }
                if (config.get(SSL_PATH.getKey(), SSL_PATH.getValue()).isDefined()) {
                    ModelNode sslConfig = config.get(SSL_PATH.getKey(), SSL_PATH.getValue());
                    writer.writeStartElement(Element.SSL.getLocalName());
                    WebSSLDefinition.NAME.marshallAsAttribute(sslConfig, writer);
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
                WebVirtualHostDefinition.ENABLE_WELCOME_ROOT.marshallAsAttribute(config, true, writer);
                WebVirtualHostDefinition.DEFAULT_WEB_MODULE.marshallAsAttribute(config, true, writer);

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
                        startwritten = writeAttribute(writer, WebAccessLogDirectoryDefinition.PATH, directory, startwritten,
                                name);
                        startwritten = writeAttribute(writer, WebAccessLogDirectoryDefinition.RELATIVE_TO, directory,
                                startwritten, name);
                        if (startwritten) {
                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement();
                }

                if (config.hasDefined(REWRITE)) {
                    for (final ModelNode rewritenode : config.get(REWRITE).asList()) {
                        Property prop = rewritenode.asProperty();
                        ModelNode rewrite = prop.getValue();
                        writer.writeStartElement(REWRITE);
                        writer.writeAttribute(NAME, prop.getName());
                        WebReWriteDefinition.PATTERN.marshallAsAttribute(rewrite, false, writer);
                        WebReWriteDefinition.SUBSTITUTION.marshallAsAttribute(rewrite, false, writer);
                        WebReWriteDefinition.FLAGS.marshallAsAttribute(rewrite, false, writer);
                        if (rewrite.hasDefined(CONDITION)) {
                            for (final ModelNode conditionnode : rewrite.get(CONDITION).asList()) {
                                Property conditionProp = conditionnode.asProperty();
                                ModelNode condition = conditionProp.getValue();
                                writer.writeStartElement(CONDITION);
                                writer.writeAttribute(NAME, conditionProp.getName());
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
            if (node.hasDefined(VALVE)) {
                for (final Property valve : node.get(VALVE).asPropertyList()) {
                    final ModelNode config = valve.getValue();
                    writer.writeStartElement(Element.VALVE.getLocalName());
                    writer.writeAttribute(NAME, valve.getName());
                    for (AttributeDefinition attr : WebValveDefinition.ATTRIBUTES) {
                        if (attr instanceof SimpleAttributeDefinition) {
                            ((SimpleAttributeDefinition) attr).marshallAsAttribute(config, false, writer);
                        }
                    }
                    if (config.hasDefined(PARAM)) {
                        for (final Property entry : config.get(PARAM).asPropertyList()) {
                            writer.writeEmptyElement(Element.PARAM.getLocalName());
                            writer.writeAttribute(Attribute.PARAM_NAME.getLocalName(), entry.getName());
                            writer.writeAttribute(Attribute.PARAM_VALUE.getLocalName(), entry.getValue().asString());
                        }
                    }
                    writer.writeEndElement();

                }
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
            containerConfigStartWritten = writeJSPConfiguration(writer, config.get(JSP_CONFIGURATION),
                    containerConfigStartWritten) || containerConfigStartWritten;
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
            WebContainerDefinition.MIME_MAPPINGS.marshallAsElement(container, writer);
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

    private boolean writeStaticResourceAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute,
                                                 ModelNode config, boolean startWritten) throws XMLStreamException {
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

    private boolean writeJSPConfiguration(XMLExtendedStreamWriter writer, ModelNode jsp, boolean containerConfigStartWritten)
            throws XMLStreamException {

        boolean startWritten = false;
        for (SimpleAttributeDefinition def : WebJSPDefinition.JSP_ATTRIBUTES) {
            startWritten = writeJspConfigAttribute(writer, def, jsp, startWritten, containerConfigStartWritten) || startWritten;
        }

        if (startWritten) {
            writer.writeEndElement();
        }

        return startWritten;
    }

    private boolean writeJspConfigAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute,
                                            ModelNode config, boolean startWritten, boolean containerConfigStartWritten) throws XMLStreamException {
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
                    WebDefinition.NATIVE.parseAndSetParameter(value, subsystem, reader);
                    break;
                case DEFAULT_VIRTUAL_SERVER:
                    WebDefinition.DEFAULT_VIRTUAL_SERVER.parseAndSetParameter(value, subsystem, reader);
                    break;
                case INSTANCE_ID:
                    WebDefinition.INSTANCE_ID.parseAndSetParameter(value, subsystem, reader);
                    break;
                case DEFAULT_SESSION_TIMEOUT:
                    attributeSupportedSince(Namespace.WEB_2_2, reader, i);
                    WebDefinition.DEFAULT_SESSION_TIMEOUT.parseAndSetParameter(value, subsystem, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        list.add(subsystem);
        boolean containerConfigDefined = false;
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case WEB_1_0:
                case WEB_1_1:
                case WEB_1_2:
                case WEB_1_3: {
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
                case WEB_1_4:
                case WEB_1_5:
                case WEB_2_0:
                case WEB_2_1:
                case WEB_2_2:
                {
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
                        case VALVE: {
                            parseValve(reader, address, list);
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

    private static void parseValve(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        final ModelNode valve = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case MODULE:
                    WebValveDefinition.MODULE.parseAndSetParameter(value, valve, reader);
                    break;
                case CLASS_NAME:
                    WebValveDefinition.CLASS_NAME.parseAndSetParameter(value, valve, reader);
                    break;
                case ENABLED:
                    WebValveDefinition.ENABLED.parseAndSetParameter(value, valve, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        valve.get(OP).set(ADD);
        PathAddress address = PathAddress.pathAddress(parent, PathElement.pathElement(VALVE, name));
        valve.get(OP_ADDR).set(address.toModelNode());
        list.add(valve);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PARAM:
                    final String[] array = requireAttributes(reader, Attribute.PARAM_NAME.getLocalName(),
                            Attribute.PARAM_VALUE.getLocalName());
                    valve.get(PARAM).get(array[0]).set(array[1]);
                    requireNoContent(reader);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static void parseDirOrFile(XMLExtendedStreamReader reader, PathAddress address, List<ModelNode> list,
                                       PathElement filePath) throws XMLStreamException {
        final PathAddress dirAddress = PathAddress.pathAddress(address, filePath);
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

    static void parseContainerConfig(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {

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
                    final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(),
                            Attribute.VALUE.getLocalName());
                    WebContainerDefinition.MIME_MAPPINGS.parseAndAddParameterElement(array[0], array[1], config, reader);

                    requireNoContent(reader);
                    break;
                }
                case WELCOME_FILE: {
                    final String welcomeFile = reader.getElementText().trim();
                    WebContainerDefinition.WELCOME_FILES.parseAndAddParameterElement(welcomeFile, config, reader);
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

    static void parseJSPConfiguration(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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
                    WebJSPDefinition.ATTRIBUTES_MAP.get(attribute.getLocalName()).parseAndSetParameter(value, jsp, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(jsp);
    }

    static void parseStaticResources(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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

    static void parseHost(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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
                case WEB_1_1:
                case WEB_1_2:
                case WEB_1_3: {
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
                case WEB_1_4:
                case WEB_1_5:
                case WEB_2_0:
                case WEB_2_1:
                case WEB_2_2: {
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
                        case VALVE:
                            parseValve(reader, address, list);
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

    static void parseSso(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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
                    WebSSODefinition.CACHE_CONTAINER.parseAndSetParameter(value, operation, reader);
                    break;
                case CACHE_NAME:
                    WebSSODefinition.CACHE_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                case DOMAIN:
                    WebSSODefinition.DOMAIN.parseAndSetParameter(value, operation, reader);
                    break;
                case REAUTHENTICATE:
                    WebSSODefinition.REAUTHENTICATE.parseAndSetParameter(value, operation, reader);
                    break;
                case HTTP_ONLY:
                    attributeSupportedSince(Namespace.WEB_2_2, reader, i);
                    WebSSODefinition.HTTP_ONLY.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        list.add(operation);
    }


    static void parseHostRewrite(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list, int rewriteCount) throws XMLStreamException {
        final ModelNode rewrite = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        String name = RULE_PREFIX + rewriteCount;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATTERN:
                    WebReWriteDefinition.PATTERN.parseAndSetParameter(value, rewrite, reader);
                    break;
                case SUBSTITUTION:
                    WebReWriteDefinition.SUBSTITUTION.parseAndSetParameter(value, rewrite, reader);
                    break;
                case FLAGS:
                    WebReWriteDefinition.FLAGS.parseAndSetParameter(value, rewrite, reader);
                    break;
                case NAME:
                    name = value;
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        final PathAddress address = PathAddress.pathAddress(parent, PathElement.pathElement(REWRITE, name));
        rewrite.get(OP_ADDR).set(address.toModelNode());
        list.add(rewrite);
        int conditionCount = 0;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1:
                case WEB_1_2:
                case WEB_1_3:
                case WEB_1_4:
                case WEB_1_5:
                case WEB_2_0:
                case WEB_2_1:
                case WEB_2_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONDITION:
                            final ModelNode condition = Util.createAddOperation();
                            String condName = CONDITION_PREFIX + conditionCount;
                            final int count2 = reader.getAttributeCount();
                            for (int i = 0; i < count2; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final String value = reader.getAttributeValue(i);
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME:
                                        condName = value;
                                        break;
                                    case TEST:
                                        WebReWriteConditionDefinition.TEST.parseAndSetParameter(value, condition, reader);
                                        break;
                                    case PATTERN:
                                        WebReWriteConditionDefinition.PATTERN.parseAndSetParameter(value, condition, reader);
                                        break;
                                    case FLAGS:
                                        WebReWriteConditionDefinition.FLAGS.parseAndSetParameter(value, condition, reader);
                                        break;
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            PathAddress condAddress = address.append(PathElement.pathElement(CONDITION, condName));
                            condition.get(OP_ADDR).set(condAddress.toModelNode());
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

    static void parseHostAccessLog(XMLExtendedStreamReader reader, final PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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
                    WebAccessLogDefinition.PATTERN.parseAndSetParameter(value, log, reader);
                    break;
                case RESOLVE_HOSTS:
                    WebAccessLogDefinition.RESOLVE_HOSTS.parseAndSetParameter(value, log, reader);
                    break;
                case EXTENDED:
                    WebAccessLogDefinition.EXTENDED.parseAndSetParameter(value, log, reader);
                    break;
                case PREFIX:
                    WebAccessLogDefinition.PREFIX.parseAndSetParameter(value, log, reader);
                    break;
                case ROTATE:
                    WebAccessLogDefinition.ROTATE.parseAndSetParameter(value, log, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        list.add(log);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1:
                case WEB_1_2:
                case WEB_1_3:
                case WEB_1_4:
                case WEB_1_5:
                case WEB_2_0:
                case WEB_2_1:
                case WEB_2_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DIRECTORY:
                            parseDirOrFile(reader, address, list, WebExtension.DIRECTORY_PATH);
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

    private static void attributeSupportedSince(Namespace namespace, XMLExtendedStreamReader reader, int i) throws XMLStreamException {
        Namespace currentNamespace= Namespace.forUri(reader.getNamespaceURI());
        if (currentNamespace.compareTo(namespace) >= 0) {
            return;
        }
        throw unexpectedAttribute(reader, i);
    }

    static void parseConnector(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list)
            throws XMLStreamException {
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
                case PROXY_BINDING:
                    attributeSupportedSince(Namespace.WEB_2_1, reader, i);
                    WebConnectorDefinition.PROXY_BINDING.parseAndSetParameter(value, connector, reader);
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
                case REDIRECT_BINDING:
                    attributeSupportedSince(Namespace.WEB_2_1, reader, i);
                    WebConnectorDefinition.REDIRECT_BINDING.parseAndSetParameter(value, connector, reader);
                    connector.remove(WebConnectorDefinition.REDIRECT_PORT.getName());
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
                case WEB_1_1:
                case WEB_1_2:
                case WEB_1_3:
                case WEB_1_4:
                case WEB_1_5:
                case WEB_2_0:
                case WEB_2_1:
                case WEB_2_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SSL:
                            parseSsl(reader, address, list);
                            break;
                        case VIRTUAL_SERVER:
                            String value = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                            WebConnectorDefinition.VIRTUAL_SERVER.parseAndAddParameterElement(value, connector, reader);
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
        final ModelNode ssl = Util.createAddOperation(address);
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
                case SSL_PROTOCOL:
                    WebSSLDefinition.SSL_PROTOCOL.parseAndSetParameter(value, ssl, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        if (namespace == Namespace.WEB_1_1 || namespace == Namespace.WEB_1_0) { // workaround to set default for old schema
            if (!ssl.hasDefined(WebSSLDefinition.KEY_ALIAS.getName())) {
                ssl.get(WebSSLDefinition.KEY_ALIAS.getName()).set("jboss");
            }
        }

        requireNoContent(reader);
        list.add(ssl);
    }

    // todo, attribute.marshallAsAttribute should return boolean
    private boolean writeAttribute(XMLExtendedStreamWriter writer, SimpleAttributeDefinition attribute, ModelNode node,
                                   boolean startWriten, String origin) throws XMLStreamException {

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
