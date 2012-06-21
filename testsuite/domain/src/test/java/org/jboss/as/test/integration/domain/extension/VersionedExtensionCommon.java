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

package org.jboss.as.test.integration.domain.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class VersionedExtensionCommon implements Extension {

    public static final String SUBSYSTEM_NAME = "test-subsystem";
    public static final String EXTENSION_NAME = "org.jboss.as.test.transformers";

    static SubsystemParser PARSER = new SubsystemParser(EXTENSION_NAME);
    static PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM,  SUBSYSTEM_NAME);
    static DescriptionProvider DESCRIPTION_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };
    static AttributeDefinition TEST_ATTRIBUTE = SimpleAttributeDefinitionBuilder.create("test-attribute", ModelType.STRING).build();

    public SubsystemParser getParser() {
        return PARSER;
    }

    protected ManagementResourceRegistration initializeSubsystem(final SubsystemRegistration registration) {
        // Common subsystem tasks
        final ResourceDefinition def = new SimpleResourceDefinition(SUBSYSTEM_PATH, new TestResourceDescriptionResolver(),
                SUBSYSTEM_ADD, SUBSYSTEM_REMOVE, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
        registration.registerXMLElementWriter(getParser());

        final ManagementResourceRegistration reg = registration.registerSubsystemModel(def);
        reg.registerReadWriteAttribute(TEST_ATTRIBUTE, null, new BasicAttributeWriteHandler(TEST_ATTRIBUTE));
        return reg;
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, EXTENSION_NAME, PARSER);
    }

    private static OperationStepHandler SUBSYSTEM_ADD = new AbstractAddStepHandler() {
        @Override
        protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            //
        }
    };

    private static OperationStepHandler SUBSYSTEM_REMOVE = new AbstractRemoveStepHandler() {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
        }
    };

    private static class BasicAttributeWriteHandler extends AbstractWriteAttributeHandler<Void> {

        protected BasicAttributeWriteHandler(AttributeDefinition def) {
            super(def);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {

        }
    }

    private static class SubsystemParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        private final String namespace;

        private SubsystemParser(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> value) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ParseUtils.requireNoContent(reader);
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(namespace, false);
            streamWriter.writeEndElement();
        }
    }

    static class TestResourceDescriptionResolver implements ResourceDescriptionResolver {
        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            return new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                    return key;
                }

                @Override
                public Enumeration<String> getKeys() {
                    return new Enumeration<String>() {
                        @Override
                        public boolean hasMoreElements() {
                            return false;
                        }

                        @Override
                        public String nextElement() {
                            return null;
                        }
                    };
                }
            };
        }

        @Override
        public String getResourceDescription(Locale locale, ResourceBundle bundle) {
            return "description";
        }

        @Override
        public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
            return attributeName;
        }

        @Override
        public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return attributeName;
        }

        @Override
        public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
            return operationName;
        }

        @Override
        public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
            return operationName + "-" + paramName;
        }

        @Override
        public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return operationName + "-" + paramName;
        }

        @Override
        public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
            return operationName;
        }

        @Override
        public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return operationName;
        }

        @Override
        public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
            return childType;
        }
    }


}
