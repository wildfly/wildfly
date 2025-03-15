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
 * Encapsulates an XML element for a singleton (i.e. non-wildcard) resource registration.
 * @author Paul Ferraro
 */
public interface SingletonResourceRegistrationXMLElement extends ResourceRegistrationXMLElement {

    interface Builder extends ResourceRegistrationXMLElement.Builder<SingletonResourceRegistrationXMLElement, Builder> {
        /**
         * Indicates that the resource associated with this element should be implied if this element is absent.
         * An implicit element will generate an empty add operation when not present and will not be written if its attributes are undefined and any content is empty.
         * @return a reference to this builder.
         */
        Builder implyIfAbsent();
    }

    class DefaultBuilder extends ResourceRegistrationXMLElement.AbstractBuilder<SingletonResourceRegistrationXMLElement, Builder> implements Builder {
        private volatile boolean implied = false;

        DefaultBuilder(ResourceRegistration registration, FeatureRegistry registry, QNameResolver resolver) {
            super(registration, registry, resolver);
            this.withElementLocalName(ResourceXMLElementLocalName.VALUE);
        }

        @Override
        public Builder implyIfAbsent() {
            this.implied = true;
            return this;
        }

        @Override
        protected Builder builder() {
            return this;
        }

        @Override
        public SingletonResourceRegistrationXMLElement build() {
            boolean implied = this.implied;
            XMLCardinality cardinality = this.getCardinality();
            ResourceRegistration registration = this.getResourceRegistration();
            PathElement path = registration.getPathElement();
            PathElement pathKey = this.getOperationKey().orElse(path);
            QName name = this.getElementName().apply(path);

            Collection<AttributeDefinition> attributes = this.getAttributes();
            AttributeDefinitionXMLConfiguration configuration = this.getConfiguration();

            XMLElementReader<ModelNode> attributesReader = !attributes.isEmpty() ? new ResourceAttributesXMLContentReader(attributes, configuration) : ResourceXMLContainer.EMPTY_READER;
            XMLContentWriter<ModelNode> attributesWriter = !attributes.isEmpty() ? new ResourceAttributesXMLContentWriter(attributes, configuration) : ResourceXMLContainer.EMPTY_WRITER;
            XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> content = this.getContent();

            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> resourceReader = new ResourceXMLContainerReader(attributesReader, content);
            XMLContentWriter<ModelNode> resourceWriter = new ResourceXMLContainerWriter<>(name, attributesWriter, Function.identity(), content);

            BiConsumer<Map<PathAddress, ModelNode>, PathAddress> operationTransformation = this.getOperationTransformation();
            XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> elementReader = new XMLElementReader<>() {
                @Override
                public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
                    Map.Entry<PathAddress, Map<PathAddress, ModelNode>> entry = this.createOperationEntry(context);
                    resourceReader.readElement(reader, entry);
                    operationTransformation.accept(entry.getValue(), entry.getKey());
                }

                @Override
                public void handleAbsentElement(Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) {
                    if (implied) {
                        // Create add operation for implied resource if element is absent
                        this.createOperationEntry(context);
                    }
                }

                private Map.Entry<PathAddress, Map<PathAddress, ModelNode>> createOperationEntry(Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) {
                    PathAddress parentOperationKey = context.getKey();
                    Map<PathAddress, ModelNode> operations = context.getValue();
                    PathAddress operationKey = parentOperationKey.append(pathKey);
                    ModelNode parentOperation = (parentOperationKey.size() > 0) ? operations.get(parentOperationKey) : null;
                    PathAddress parentAddress = (parentOperation != null) ? PathAddress.pathAddress(parentOperation.get(ModelDescriptionConstants.OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
                    PathAddress operationAddress = parentAddress.append(path);
                    ModelNode operation = Util.createAddOperation(operationAddress);
                    operations.put(operationKey, operation);
                    return Map.entry(operationKey, operations);
                }
            };
            XMLContentWriter<ModelNode> elementWriter = new XMLContentWriter<>() {
                @Override
                public void writeContent(XMLExtendedStreamWriter writer, ModelNode parentModel) throws XMLStreamException {
                    String[] pair = path.getKeyValuePair();
                    if (parentModel.has(pair) && (!implied || !this.isEmpty(parentModel))) {
                        resourceWriter.writeContent(writer, parentModel.get(pair));
                    }
                }

                @Override
                public boolean isEmpty(ModelNode parentModel) {
                    String[] pair = path.getKeyValuePair();
                    return !parentModel.hasDefined(pair) || resourceWriter.isEmpty(parentModel.get(pair));
                }
            };
            return new DefaultSingletonResourceRegistrationXMLElement(registration, name, cardinality, elementReader, elementWriter);
        }
    }

    class DefaultSingletonResourceRegistrationXMLElement extends DefaultResourceRegistrationXMLElement implements SingletonResourceRegistrationXMLElement {

        DefaultSingletonResourceRegistrationXMLElement(ResourceRegistration registration, QName name, XMLCardinality cardinality, XMLElementReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> reader, XMLContentWriter<ModelNode> writer) {
            super(registration, name, cardinality, reader, writer);
        }
    }
}
