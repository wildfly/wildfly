/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElementReader;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Encapsulates an XML element for a subsystem resource registration.
 * @author Paul Ferraro
 */
public interface SubsystemResourceRegistrationXMLElement extends ResourceRegistrationXMLElement {

    interface Builder extends ResourceRegistrationXMLElement.Builder<SubsystemResourceRegistrationXMLElement, Builder> {
    }

    class DefaultBuilder extends ResourceRegistrationXMLElement.AbstractBuilder<SubsystemResourceRegistrationXMLElement, Builder> implements Builder {

        DefaultBuilder(ResourceRegistration registration, FeatureRegistry registry, QNameResolver resolver) {
            super(registration, registry, resolver);
            this.withElementLocalName(ResourceXMLElementLocalName.KEY);
        }

        @Override
        protected Builder builder() {
            return this;
        }

        @Override
        public SubsystemResourceRegistrationXMLElement build() {
            ResourceRegistration registration = this.getResourceRegistration();
            PathElement path = registration.getPathElement();
            QName name = this.getElementName().apply(path);

            Collection<AttributeDefinition> attributes = this.getAttributes();
            AttributeDefinitionXMLConfiguration configuration = this.getConfiguration();

            XMLCardinality cardinality = this.getCardinality();
            XMLElementReader<ModelNode> attributesReader = !attributes.isEmpty() ? new ResourceAttributesXMLContentReader(attributes, configuration) : ResourceXMLContainer.EMPTY_READER;
            XMLContentWriter<ModelNode> attributesWriter = !attributes.isEmpty() ? new ResourceAttributesXMLContentWriter(attributes, configuration) : ResourceXMLContainer.EMPTY_WRITER;
            XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content = this.getContent();

            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> resourceReader = new ResourceXMLContainerReader(attributesReader, content);
            XMLContentWriter<ModelNode> resourceWriter = new ResourceXMLContainerWriter<>(name, attributesWriter, Function.identity(), content);

            BiConsumer<Map<PathAddress, ModelNode>, PathAddress> operationTransformation = this.getOperationTransformation();
            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> elementReader = new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                    PathAddress parentOperationKey = context.getKey();
                    Map<PathAddress, ModelNode> operations = context.getValue();

                    ModelNode parentOperation = (parentOperationKey.size() > 0) ? operations.get(parentOperationKey) : null;
                    PathAddress parentAddress = (parentOperation != null) ? PathAddress.pathAddress(parentOperation.get(ModelDescriptionConstants.OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
                    PathAddress operationAddress = parentAddress.append(path);
                    PathAddress operationKey = parentOperationKey.append(path);
                    ModelNode operation = Util.createAddOperation(operationAddress);
                    operations.put(operationKey, operation);

                    resourceReader.readElement(reader, Map.entry(operationKey, operations));
                    operationTransformation.accept(operations, operationKey);
                }
            };
            XMLContentWriter<ModelNode> elementWriter = new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parentModel) throws XMLStreamException {
                    String[] pair = path.getKeyValuePair();
                    if (parentModel.has(pair)) {
                        resourceWriter.writeContent(writer, parentModel.get(pair));
                    }
                }

                @Override
                public boolean isEmpty(ModelNode parentModel) {
                    return false;
                }
            };
            return new DefaultSubsystemResourceRegistrationXMLElement(registration, name, cardinality, elementReader, elementWriter);
        }
    }

    class DefaultSubsystemResourceRegistrationXMLElement extends DefaultResourceRegistrationXMLElement implements SubsystemResourceRegistrationXMLElement {

        DefaultSubsystemResourceRegistrationXMLElement(ResourceRegistration registration, QName name, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer) {
            super(registration, name, cardinality, reader, writer);
        }
    }
}
