/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model.parser;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.XMLElement;
import org.wildfly.extension.picketlink.common.parser.ModelXMLElementWriter;
import org.wildfly.extension.picketlink.idm.Namespace;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.FILE_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.JPA_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.PARTITION_MANAGER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPES;

/**
 * <p> XML Writer for the subsystem schema, version 1.0. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    private static final Map<String, ModelXMLElementWriter> writers = new HashMap<String, ModelXMLElementWriter>();

    static {
        // identity management elements writers
        registerWriter(PARTITION_MANAGER, COMMON_NAME);
        registerWriter(IDENTITY_CONFIGURATION, COMMON_NAME);
        registerWriter(JPA_STORE);
        registerWriter(FILE_STORE);
        registerWriter(LDAP_STORE);
        registerWriter(LDAP_STORE_MAPPING, COMMON_NAME, XMLElement.LDAP_MAPPINGS);
        registerWriter(LDAP_STORE_ATTRIBUTE);
        registerWriter(SUPPORTED_TYPES);
        registerWriter(SUPPORTED_TYPE, COMMON_NAME);
        registerWriter(IDENTITY_STORE_CREDENTIAL_HANDLER, COMMON_NAME, XMLElement.IDENTITY_STORE_CREDENTIAL_HANDLERS);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        // Start subsystem
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);

        ModelNode subsystemNode = context.getModelNode();

        if (subsystemNode.isDefined()) {
            List<ModelNode> identityManagement = subsystemNode.asList();

            for (ModelNode modelNode : identityManagement) {
                writers.get(PARTITION_MANAGER.getName()).write(writer, modelNode);
            }
        }

        // End subsystem
        writer.writeEndElement();
    }

    private static void registerWriter(final ModelElement element, final ModelElement keyAttribute) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, keyAttribute.getName(), writers));
    }

    private static void registerWriter(final ModelElement element) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, writers));
    }

    private static void registerWriter(final ModelElement element, final ModelElement keyAttribute, final XMLElement parent) {
        writers.put(element.getName(), new ModelXMLElementWriter(element, keyAttribute.getName(), parent, writers));
    }
}
