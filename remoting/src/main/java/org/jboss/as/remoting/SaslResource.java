/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SECURITY;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.xnio.sasl.SaslQop;
import org.xnio.sasl.SaslStrength;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SaslResource extends SimpleResourceDefinition {
    static final PathElement SASL_CONFIG_PATH = PathElement.pathElement(SECURITY, SASL);

    static final SaslResource INSTANCE = new SaslResource();

    static final AttributeDefinition INCLUDE_MECHANISMS_ATTRIBUTE = new SaslListAttributeDefinition(Element.INCLUDE_MECHANISMS, INCLUDE_MECHANISMS, true);
    static final AttributeDefinition QOP_ATTRIBUTE = new SaslListAttributeDefinition(Element.QOP, QOP, true, QopParameterValidation.INSTANCE);
    static final AttributeDefinition STRENGTH_ATTRIBUTE = new SaslListAttributeDefinition(Element.STRENGTH, STRENGTH, true, StrengthParameterValidation.INSTANCE);
    static final AttributeDefinition REUSE_SESSION_ATTRIBUTE = new NamedValueAttributeDefinition(REUSE_SESSION, Attribute.VALUE, new ModelNode().set(false), ModelType.BOOLEAN, true);
    static final AttributeDefinition SERVER_AUTH_ATTRIBUTE = new NamedValueAttributeDefinition(SERVER_AUTH, Attribute.VALUE, new ModelNode().set(false), ModelType.BOOLEAN, true);


    private SaslResource() {
        super(SASL_CONFIG_PATH,
                RemotingExtension.getResourceDescriptionResolver(SASL),
                SaslAdd.INSTANCE,
                SaslRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final ReloadRequiredWriteAttributeHandler writeHandler =
                new ReloadRequiredWriteAttributeHandler(INCLUDE_MECHANISMS_ATTRIBUTE, QOP_ATTRIBUTE, STRENGTH_ATTRIBUTE,
                        REUSE_SESSION_ATTRIBUTE, SERVER_AUTH_ATTRIBUTE);
        resourceRegistration.registerReadWriteAttribute(INCLUDE_MECHANISMS_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(QOP_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(STRENGTH_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(REUSE_SESSION_ATTRIBUTE, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SERVER_AUTH_ATTRIBUTE, null, writeHandler);
    }

    private static class SaslListAttributeDefinition extends ListAttributeDefinition {
        final Element element;

        SaslListAttributeDefinition(Element element, String name, boolean allowNull) {
            this(element, name, allowNull, new StringLengthValidator(1));
        }

        SaslListAttributeDefinition(Element element, String name, boolean allowNull, ParameterValidator validator) {
            super(name, allowNull, validator);
            this.element = element;
        }

        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            setValueType(node);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(getName())) {
                List<ModelNode> list = resourceModel.get(getName()).asList();
                if (list.size() > 0) {
                    writer.writeEmptyElement(element.getLocalName());
                    StringBuilder sb = new StringBuilder();
                    for (ModelNode child : list) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(child.asString());
                    }
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), sb.toString());
                }
            }
        }

        private void setValueType(ModelNode node) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }
    }

    private abstract static class SaslEnumValidator extends StringLengthValidator implements AllowedValuesValidator {
        final List<ModelNode> allowedValues = new ArrayList<ModelNode>();

        SaslEnumValidator(Enum<?>[] src, boolean toLowerCase) {
            super(1);
            for (Enum<?> e : src) {
                allowedValues.add(new ModelNode().set(toLowerCase ? e.name().toLowerCase() : e.name()));
            }
        }

        @Override
        public List<ModelNode> getAllowedValues() {
            return allowedValues;
        }

    }

    private static class QopParameterValidation extends SaslEnumValidator implements AllowedValuesValidator {
        static final QopParameterValidation INSTANCE = new QopParameterValidation();
        public QopParameterValidation() {
            super(SaslQop.values(), false);
        }
    }

    private static class StrengthParameterValidation extends SaslEnumValidator implements AllowedValuesValidator {
        static final StrengthParameterValidation INSTANCE = new StrengthParameterValidation();
        public StrengthParameterValidation() {
            super(SaslStrength.values(), true);
        }
    }
}
