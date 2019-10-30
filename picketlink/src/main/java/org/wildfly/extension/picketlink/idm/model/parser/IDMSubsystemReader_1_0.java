/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model.parser;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.model.AttributedTypeEnum;
import org.wildfly.extension.picketlink.idm.model.CredentialHandlerResourceDefinition;
import org.wildfly.extension.picketlink.idm.model.CredentialTypeEnum;
import org.wildfly.extension.picketlink.idm.model.LDAPStoreAttributeResourceDefinition;
import org.wildfly.extension.picketlink.idm.model.LDAPStoreMappingResourceDefinition;
import org.wildfly.extension.picketlink.idm.model.SupportedTypeResourceDefinition;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_CLASS_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_CODE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE_MAPPING;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;

/**
 * <p> XML Reader for the subsystem schema, version 1.0. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMSubsystemReader_1_0 extends AbstractIDMSubsystemReader {

    @Override
    protected void parseLDAPMappingConfig(final XMLExtendedStreamReader reader, final ModelNode identityProviderNode,
                                               final List<ModelNode> addOperations) throws XMLStreamException {
        String name = reader.getAttributeValue("", COMMON_CLASS_NAME.getName());

        if (name == null) {
            name = reader.getAttributeValue("", COMMON_CODE.getName());

            if (name != null) {
                name = AttributedTypeEnum.forType(name);
            }
        }

        ModelNode ldapMappingConfig = parseConfig(reader, LDAP_STORE_MAPPING,
                                                         name, identityProviderNode,
                                                         LDAPStoreMappingResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                                     List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case LDAP_STORE_ATTRIBUTE:
                        parseConfig(reader, LDAP_STORE_ATTRIBUTE, LDAPStoreAttributeResourceDefinition.NAME.getName(),
                                       parentNode, LDAPStoreAttributeResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                }
            }
        }, LDAP_STORE_MAPPING, ldapMappingConfig, reader, addOperations);
    }

    @Override
    protected ModelNode parseCredentialHandlerConfig(XMLExtendedStreamReader reader, ModelNode identityProviderNode,
                                                          List<ModelNode> addOperations) throws XMLStreamException {
        String name = reader.getAttributeValue("", COMMON_CLASS_NAME.getName());

        if (name == null) {
            name = reader.getAttributeValue("", COMMON_CODE.getName());

            if (name != null) {
                name = CredentialTypeEnum.forType(name);
            }
        }

        return parseConfig(reader, IDENTITY_STORE_CREDENTIAL_HANDLER, name,
                                  identityProviderNode, CredentialHandlerResourceDefinition.INSTANCE.getAttributes(), addOperations);
    }

    @Override
    protected void parseSupportedTypeConfig(XMLExtendedStreamReader reader, ModelNode parentNode, List<ModelNode> addOperations)
            throws XMLStreamException {
        String name = reader.getAttributeValue("", COMMON_CLASS_NAME.getName());

        if (name == null) {
            name = reader.getAttributeValue("", COMMON_CODE.getName());

            if (name != null) {
                name = AttributedTypeEnum.forType(name);
            }
        }

        parseConfig(reader, SUPPORTED_TYPE, name, parentNode,SupportedTypeResourceDefinition.INSTANCE.getAttributes(), addOperations);
    }
}
