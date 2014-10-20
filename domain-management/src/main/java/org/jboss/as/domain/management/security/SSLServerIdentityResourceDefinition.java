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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.ModelDescriptionConstants.TLS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.TLSV1;
import static org.jboss.as.domain.management.ModelDescriptionConstants.TLSV1_1;
import static org.jboss.as.domain.management.ModelDescriptionConstants.TLSV1_2;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a management security realm's SSL-based server identity resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SSLServerIdentityResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition PROTOCOL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PROTOCOL, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(TLS)).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final StringListAttributeDefinition ENABLED_CIPHER_SUITES = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.ENABLED_CIPHER_SUITES)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAttributeMarshaller(new StringListMarshaller(Attribute.ENABLED_CIPHER_SUITES.getLocalName()))
            .build();

    public static final StringListAttributeDefinition ENABLED_PROTOCOLS = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.ENABLED_PROTOCOLS)
            .setDefaultValue(new ModelNode().add(TLSV1).add(TLSV1_1).add(TLSV1_2))
            .setAllowExpression(true)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAttributeMarshaller(new StringListMarshaller(Attribute.ENABLED_PROTOCOLS.getLocalName()))
            .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {
            PROTOCOL, ENABLED_CIPHER_SUITES, ENABLED_PROTOCOLS, KeystoreAttributes.KEYSTORE_PASSWORD, KeystoreAttributes.KEYSTORE_PATH, KeystoreAttributes.KEYSTORE_RELATIVE_TO,
            KeystoreAttributes.ALIAS, KeystoreAttributes.KEY_PASSWORD, KeystoreAttributes.KEYSTORE_PROVIDER
    };

    public SSLServerIdentityResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SERVER_IDENTITY, ModelDescriptionConstants.SSL),
                ControllerResolver.getResolver("core.management.security-realm.server-identity.ssl"),
                new SecurityRealmChildAddHandler(false, false, ATTRIBUTE_DEFINITIONS),
                new SecurityRealmChildRemoveHandler(false),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(ATTRIBUTE_DEFINITIONS);
        handler.registerAttributes(resourceRegistration);
    }

    private static class StringListMarshaller extends AttributeMarshaller {

        private final String xmlAttributeName;

        private StringListMarshaller(final String xmlAttributeName) {
            this.xmlAttributeName = xmlAttributeName;
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault,
                XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (ModelNode child : list) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(child.asString());
                    }
                    writer.writeAttribute(xmlAttributeName, sb.toString());
                }
            }
        }
    }
}
