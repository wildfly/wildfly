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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * Test util.
 *
 * @author Emanuel Muckenhuber
 */
class SubsystemInitialization {

    private final String subsystemName;
    private final ResourceDefinition definition;
    private final SubsystemParser parser;
    private final boolean allowExpressions;

    static AttributeDefinition TEST_ATTRIBUTE = SimpleAttributeDefinitionBuilder.create("test-attribute", ModelType.STRING).build();

    SubsystemInitialization(final String subsystemName, boolean allowExpressions) {
        this.subsystemName = subsystemName;
        this.allowExpressions = allowExpressions;
        definition = createResourceDefinition(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystemName));
        parser = new SubsystemParser(subsystemName);
    }

    /**
     * Initialize the subsystem with a few common attributes.
     *
     * @param context the extension context
     * @param version the model version
     * @return the subsystem registration for further use
     */
    protected RegistrationResult initializeSubsystem(final ExtensionContext context, ModelVersion version) {
        final SubsystemRegistration subsystem = context.registerSubsystem(subsystemName,  version.getMajor(), version.getMinor());
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(definition);

        // Test attribute
        registration.registerReadWriteAttribute(TEST_ATTRIBUTE, null, new BasicAttributeWriteHandler(TEST_ATTRIBUTE));

        // Other basic handlers
        final AttributeDefinition integer = SimpleAttributeDefinitionBuilder.create("int", ModelType.INT).setAllowExpression(allowExpressions).build();
        final AttributeDefinition string = SimpleAttributeDefinitionBuilder.create("string", ModelType.STRING).setAllowExpression(allowExpressions).build();
        registration.registerReadWriteAttribute(integer, null, new BasicAttributeWriteHandler(integer));
        registration.registerReadWriteAttribute(string, null, new BasicAttributeWriteHandler(string));
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        return new RegistrationResult() {
            @Override
            public SubsystemRegistration getSubsystemRegistration() {
                return subsystem;
            }

            @Override
            public ManagementResourceRegistration getResourceRegistration() {
                return registration;
            }
        };
    }

    void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(subsystemName, subsystemName, parser);
    }

    protected ResourceDefinition createResourceDefinition(final PathElement element) {
        return VersionedExtensionCommon.createResourceDefinition(element);
    }

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

    interface RegistrationResult {

        SubsystemRegistration getSubsystemRegistration();
        ManagementResourceRegistration getResourceRegistration();

    }


}
