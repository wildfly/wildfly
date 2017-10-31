/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.security.Constants.*;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.APPLY_ROLE_MAPPERS;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.LEGACY_JAAS_CONFIG;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.LEGACY_JSSE_CONFIG;

import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A {@link XMLElementWriter} that is responsible for writing the configuration of the legacy security subsystem. The
 * subsystem is written according to the current (latest) version of the schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecuritySubsystemPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final SecuritySubsystemPersister INSTANCE = new SecuritySubsystemPersister();

    protected SecuritySubsystemPersister() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        if (SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.isMarshallable(node) ||
                SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC.isMarshallable(node)) {
            writer.writeEmptyElement(Element.SECURITY_MANAGEMENT.getLocalName());
            if (SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.isMarshallable(node)) {
                SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.marshallAsAttribute(node, writer);
            }
            if (SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC.isMarshallable(node)) {
                SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC.marshallAsAttribute(node, writer);
            }
        }

        if (node.hasDefined(SECURITY_DOMAIN) && node.get(SECURITY_DOMAIN).asInt() > 0) {
            writer.writeStartElement(Element.SECURITY_DOMAINS.getLocalName());
            ModelNode securityDomains = node.get(SECURITY_DOMAIN);
            for (String policy : securityDomains.keys()) {
                writer.writeStartElement(Element.SECURITY_DOMAIN.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), policy);
                ModelNode policyDetails = securityDomains.get(policy);
                SecurityDomainResourceDefinition.CACHE_TYPE.marshallAsAttribute(policyDetails, writer);
                writeSecurityDomainContent(writer, policyDetails);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (node.hasDefined(Constants.VAULT)) {
            ModelNode vault = node.get(Constants.VAULT, Constants.CLASSIC);
            writer.writeStartElement(Element.VAULT.getLocalName());
            VaultResourceDefinition.CODE.marshallAsAttribute(vault, writer);

            if (vault.hasDefined(Constants.VAULT_OPTIONS)) {
                ModelNode properties = vault.get(Constants.VAULT_OPTIONS);
                for (Property prop : properties.asPropertyList()) {
                    writer.writeEmptyElement(Element.VAULT_OPTION.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
                }
            }
            writer.writeEndElement();
        }

        writeElytronIntegration(writer, node);
        writer.writeEndElement();
    }

    private void writeSecurityDomainContent(XMLExtendedStreamWriter writer, ModelNode policyDetails) throws XMLStreamException {
        Set<String> keys = policyDetails.keys();
        keys.remove(NAME);
        keys.remove(CACHE_TYPE);

        for (String key : keys) {
            Element element = Element.forName(key);
            switch (element) {
                case AUTHENTICATION: {
                    ModelNode kind = policyDetails.get(AUTHENTICATION);
                    for (Property prop : kind.asPropertyList()) {
                        if (CLASSIC.equals(prop.getName())) {
                            writeAuthentication(writer, prop.getValue());
                        } else if (JASPI.equals(prop.getName())) {
                            writeAuthenticationJaspi(writer, prop.getValue());
                        }
                    }

                    break;
                }
                case AUTHORIZATION: {
                    writeAuthorization(writer, policyDetails.get(AUTHORIZATION, CLASSIC));
                    break;
                }
                case ACL: {
                    writeACL(writer, policyDetails.get(ACL, CLASSIC));
                    break;
                }
                case AUDIT: {
                    writeAudit(writer, policyDetails.get(AUDIT, CLASSIC));
                    break;
                }
                case IDENTITY_TRUST: {
                    writeIdentityTrust(writer, policyDetails.get(IDENTITY_TRUST, CLASSIC));
                    break;
                }
                case MAPPING: {
                    writeMapping(writer, policyDetails.get(MAPPING, CLASSIC));
                    break;
                }
                case JSSE: {
                    writeJSSE(writer, policyDetails.get(JSSE, CLASSIC));
                    break;
                }
            }
        }
    }

    private void writeAuthentication(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
            writeLoginModule(writer, modelNode, Constants.LOGIN_MODULE);
            writer.writeEndElement();
        }
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
            writeLoginModule(writer, modelNode, Constants.POLICY_MODULE, Element.POLICY_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeACL(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.ACL.getLocalName());
            writeLoginModule(writer, modelNode, Constants.ACL_MODULE, Element.ACL_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeAudit(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUDIT.getLocalName());
            writeLoginModule(writer, modelNode, Constants.PROVIDER_MODULE, Element.PROVIDER_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeIdentityTrust(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.IDENTITY_TRUST.getLocalName());
            writeLoginModule(writer, modelNode, Constants.TRUST_MODULE, Element.TRUST_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeMapping(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.MAPPING.getLocalName());
            writeLoginModule(writer, modelNode, Constants.MAPPING_MODULE, Constants.MAPPING_MODULE);
            writer.writeEndElement();
        }
    }


    private void writeAuthenticationJaspi(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.AUTHENTICATION_JASPI.getLocalName());
            ModelNode moduleStack = modelNode.get(LOGIN_MODULE_STACK);
            writeLoginModuleStack(writer, moduleStack);
            writeLoginModule(writer, modelNode, Constants.AUTH_MODULE, Element.AUTH_MODULE.getLocalName());
            writer.writeEndElement();
        }
    }

    private void writeLoginModuleStack(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            List<Property> stacks = modelNode.asPropertyList();
            for (Property stack : stacks) {
                writer.writeStartElement(Element.LOGIN_MODULE_STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), stack.getName());
                writeLoginModule(writer, stack.getValue(), Constants.LOGIN_MODULE);
                writer.writeEndElement();
            }
        }
    }

    private void writeLoginModule(XMLExtendedStreamWriter writer, ModelNode modelNode, String key) throws XMLStreamException {
        writeLoginModule(writer, modelNode, key, Element.LOGIN_MODULE.getLocalName());
    }

    private void writeLoginModule(XMLExtendedStreamWriter writer, ModelNode modelNode, String key, final String elementName) throws XMLStreamException {
        if (!modelNode.hasDefined(key)){
            return;
        }
        final ModelNode modules = modelNode.get(key);
        for (Property moduleProp : modules.asPropertyList()) {
            ModelNode module = moduleProp.getValue();
            writer.writeStartElement(elementName);
            if (!moduleProp.getName().equals(module.get(CODE).asString())) {
                writer.writeAttribute(NAME, moduleProp.getName());
            }
            LoginModuleResourceDefinition.CODE.marshallAsAttribute(module, writer);
            LoginModuleResourceDefinition.FLAG.marshallAsAttribute(module, writer);
            MappingModuleDefinition.TYPE.marshallAsAttribute(module, writer);
            JASPIMappingModuleDefinition.LOGIN_MODULE_STACK_REF.marshallAsAttribute(module, writer);
            LoginModuleResourceDefinition.MODULE.marshallAsAttribute(module, false, writer);
            if (module.hasDefined(Constants.MODULE_OPTIONS)) {
                for (ModelNode option : module.get(Constants.MODULE_OPTIONS).asList()) {
                    writer.writeEmptyElement(Element.MODULE_OPTION.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), option.asProperty().getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), option.asProperty().getValue().asString());
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeJSSE(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException {
        if (modelNode.isDefined() && modelNode.asInt() > 0) {
            writer.writeStartElement(Element.JSSE.getLocalName());
            JSSEResourceDefinition.KEYSTORE.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.TRUSTSTORE.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.KEYMANAGER.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.TRUSTMANAGER.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CIPHER_SUITES.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.SERVER_ALIAS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.SERVICE_AUTH_TOKEN.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CLIENT_ALIAS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.CLIENT_AUTH.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.PROTOCOLS.marshallAsAttribute(modelNode, false, writer);
            JSSEResourceDefinition.ADDITIONAL_PROPERTIES.marshallAsElement(modelNode, writer);
            writer.writeEndElement();
        }
    }

    private void writeElytronIntegration(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        boolean integrationStarted = false;
        integrationStarted = integrationStarted | writeSecurityRealms(writer, modelNode, integrationStarted);
        integrationStarted = integrationStarted | writeTLS(writer, modelNode, integrationStarted);

        if (integrationStarted) {
            writer.writeEndElement();
        }
    }

    private boolean writeSecurityRealms(final XMLExtendedStreamWriter writer, final ModelNode modelNode,
                                       final boolean integrationStarted) throws XMLStreamException {
        if (modelNode.hasDefined(ELYTRON_REALM)) {
            if (integrationStarted == false) {
                writer.writeStartElement(ELYTRON_INTEGRATION);
            }
            writer.writeStartElement(SECURITY_REALMS);
            ModelNode elytronRealms = modelNode.require(ELYTRON_REALM);
            for (String realmName : elytronRealms.keys()) {
                writer.writeStartElement(ELYTRON_REALM);
                writer.writeAttribute(NAME, realmName);
                LEGACY_JAAS_CONFIG.marshallAsAttribute(elytronRealms.require(realmName), writer);
                APPLY_ROLE_MAPPERS.marshallAsAttribute(elytronRealms.require(realmName), false, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
            return true;
        }
        return false;
    }

    private boolean writeTLS(final XMLExtendedStreamWriter writer, final ModelNode modelNode,
                                            final boolean integrationStarted) throws XMLStreamException {
        if (modelNode.hasDefined(ELYTRON_KEY_STORE) || modelNode.hasDefined(ELYTRON_TRUST_STORE) ||
                modelNode.hasDefined(ELYTRON_KEY_MANAGER) || modelNode.hasDefined(ELYTRON_TRUST_MANAGER)) {
            if (integrationStarted == false) {
                writer.writeStartElement(ELYTRON_INTEGRATION);
            }
            writer.writeStartElement(TLS);
            writeTLSEntity(writer, modelNode, ELYTRON_KEY_STORE);
            writeTLSEntity(writer, modelNode, ELYTRON_TRUST_STORE);
            writeTLSEntity(writer, modelNode, ELYTRON_KEY_MANAGER);
            writeTLSEntity(writer, modelNode, ELYTRON_TRUST_MANAGER);
            writer.writeEndElement();
            return true;
        }
        return false;
    }

    private void writeTLSEntity(final XMLExtendedStreamWriter writer, final ModelNode modelNode,
                                          final String tlsEntityName) throws XMLStreamException {
        if (modelNode.hasDefined(tlsEntityName)) {
            ModelNode tlsEntities = modelNode.require(tlsEntityName);
            for (String entityName : tlsEntities.keys()) {
                writer.writeStartElement(tlsEntityName);
                writer.writeAttribute(NAME, entityName);
                LEGACY_JSSE_CONFIG.marshallAsAttribute(tlsEntities.require(entityName), writer);
                writer.writeEndElement();
            }
        }
    }
}
