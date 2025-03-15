/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.SubsystemResourceRegistration;
import org.jboss.as.clustering.controller.xml.QNameResolver;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLElementTester;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshallers;
import org.jboss.as.controller.AttributeParsers;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Unit test for {@link ResourceRegistrationXMLElement}.
 */
public class ResourceRegistrationXMLElementTestCase implements FeatureRegistry, QNameResolver {
    private static final String NAMESPACE_URI = "wildfly:test:1.0";
    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this, this);

    @Override
    public Stability getStability() {
        return Stability.COMMUNITY;
    }

    @Override
    public QName resolve(String localName) {
        return new QName(NAMESPACE_URI, localName);
    }

    @Test
    public void singleton() throws XMLStreamException, OperationFailedException {
        SubsystemResourceRegistration subsystem = SubsystemResourceRegistration.of("test");
        ResourceRegistration child = ResourceRegistration.of(PathElement.pathElement("component", "child"));
        ResourceRegistration alternateChild = ResourceRegistration.of(PathElement.pathElement("component", "alternate"));

        AttributeDefinition requiredAttribute = new SimpleAttributeDefinitionBuilder("required", ModelType.STRING).setRequired(true).build();
        AttributeDefinition optionalAttribute = new SimpleAttributeDefinitionBuilder("optional", ModelType.STRING).setRequired(false).build();
        AttributeDefinition experimentalAttribute = new SimpleAttributeDefinitionBuilder("experimental", ModelType.STRING).setRequired(false).setStability(Stability.EXPERIMENTAL).build();

        ResourceRegistrationXMLElement childElement = this.factory.singletonElement(child).addAttributes(List.of(requiredAttribute, optionalAttribute, experimentalAttribute)).build();
        // Alternate child has same attributes, but parse/marshal as elements
        ResourceRegistrationXMLElement alternateChildElement = this.factory.singletonElement(alternateChild)
                .withContent(this.factory.all()
                        .withMarshallers(Map.of(requiredAttribute, AttributeMarshallers.SIMPLE_ELEMENT, optionalAttribute, AttributeMarshallers.SIMPLE_ELEMENT, experimentalAttribute, AttributeMarshallers.SIMPLE_ELEMENT))
                        .withParsers(Map.of(requiredAttribute, AttributeParsers.SIMPLE_ELEMENT, optionalAttribute, AttributeParsers.SIMPLE_ELEMENT, experimentalAttribute, AttributeParsers.SIMPLE_ELEMENT))
                        .addElements(List.of(requiredAttribute, optionalAttribute, experimentalAttribute))
                        .build())
                .build();
        ResourceRegistrationXMLElement subsystemElement = this.factory.subsystemElement(subsystem).withContent(this.factory.choice().addElement(childElement).addElement(alternateChildElement).build()).build();

        try (XMLElementTester<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> tester = XMLElementTester.of(subsystemElement, () -> Map.entry(PathAddress.EMPTY_ADDRESS, new LinkedHashMap<>()))) {
            PathAddress subsystemAddress = PathAddress.pathAddress(subsystem.getPathElement());
            PathAddress childAddress = subsystemAddress.append(child.getPathElement());
            PathAddress alternateChildAddress = subsystemAddress.append(alternateChild.getPathElement());

            // Positive tests

            // Verify attribute-based child with required attributes only
            String xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child required="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo");
            Map<PathAddress, ModelNode> operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddress), List.copyOf(operations.keySet()));
            ModelNode operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", requiredAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertEquals(operations.toString(), null, optionalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operations.toString(), experimentalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            ModelNode model = new ModelNode();
            ModelNode subsystemModel = new ModelNode();
            subsystemModel.get(child.getPathElement().getKeyValuePair()).get(requiredAttribute.getName()).set("foo");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify attribute-based child with permissible attributes
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child required="%s" optional="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddress), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", requiredAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertEquals(operations.toString(), "bar", optionalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operations.toString(), experimentalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            model.clear();
            subsystemModel.clear();
            ModelNode childModel = subsystemModel.get(child.getPathElement().getKeyValuePair());
            childModel.get(requiredAttribute.getName()).set("foo");
            childModel.get(optionalAttribute.getName()).set("bar");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify element-based child with required attributes only
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <required>%s</required>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, alternateChildAddress), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(alternateChildAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), alternateChildAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", requiredAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertEquals(operations.toString(), null, optionalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operations.toString(), experimentalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            model.clear();
            subsystemModel.clear();
            childModel.clear();
            childModel = subsystemModel.get(alternateChild.getPathElement().getKeyValuePair());
            childModel.get(requiredAttribute.getName()).set("foo");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify element-based child with permissible attributes
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <required>%s</required>
                            <optional>%s</optional>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, alternateChildAddress), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(alternateChildAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), alternateChildAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", requiredAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertEquals(operations.toString(), "bar", optionalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operations.toString(), experimentalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            model.clear();
            subsystemModel.clear();
            childModel.clear();
            childModel = subsystemModel.get(alternateChild.getPathElement().getKeyValuePair());
            childModel.get(requiredAttribute.getName()).set("foo");
            childModel.get(optionalAttribute.getName()).set("bar");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify element-based child with permissible attributes, unordered
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <optional>%s</optional>
                            <required>%s</required>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, alternateChildAddress), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(alternateChildAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), alternateChildAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "bar", requiredAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertEquals(operations.toString(), "foo", optionalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operations.toString(), experimentalAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            // Skip writeElement verification
            // Written order will not match input order

            // Negative tests

            // Missing child resource (choice was required)
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s"/>
                    """, NAMESPACE_URI)));

            // Missing required attribute/element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child optional="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <optional>%s</optional>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo")));

            // Unexpected attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" required="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate name="%s">
                            <required>%s</required>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));

            // Unexpected element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child required="%s">
                            <required>%s</required>
                        </child>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <required>%s</required>
                            <unexpected>%s</unexpected>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));

            // Experimental attribute/element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child required="%s" experimental="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <alternate>
                            <required>%s</required>
                            <experimental>%s</experimental>
                        </alternate>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));
        }
    }

    @Test
    public void wildcard() throws XMLStreamException, OperationFailedException {
        SubsystemResourceRegistration subsystem = SubsystemResourceRegistration.of("test");
        ResourceRegistration child = ResourceRegistration.of(PathElement.pathElement("child"));
        // An unwrapped properties attribute will have unbounded cardinality
        PropertiesAttributeDefinition repeatableElement = new PropertiesAttributeDefinition.Builder("repeatable-element").setRequired(false).build();

        ResourceRegistrationXMLElement childElement = this.factory.namedElement(child).withContent(this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL).addElement(repeatableElement).build()).build();
        ResourceRegistrationXMLElement subsystemElement = this.factory.subsystemElement(subsystem).withContent(this.factory.choice().withCardinality(XMLCardinality.Unbounded.OPTIONAL).addElement(childElement).build()).build();

        OperationContext context = Mockito.mock(OperationContext.class);
        Mockito.doAnswer(AdditionalAnswers.returnsFirstArg()).when(context).resolveExpressions(ArgumentMatchers.any());

        try (XMLElementTester<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> tester = XMLElementTester.of(subsystemElement, () -> Map.entry(PathAddress.EMPTY_ADDRESS, new LinkedHashMap<>()))) {
            PathAddress subsystemAddress = PathAddress.pathAddress(subsystem.getPathElement());
            PathElement[] childPaths = IntStream.range(0, 2).mapToObj(String::valueOf).map(value -> PathElement.pathElement(child.getPathElement().getKey(), value)).toArray(PathElement[]::new);
            PathAddress[] childAddresses = Stream.of(childPaths).map(subsystemAddress::append).toArray(PathAddress[]::new);

            // Positive tests

            // Verify zero children
            String xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s"/>
                    """, NAMESPACE_URI);
            Map<PathAddress, ModelNode> operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 1, operations.size());
            ModelNode operation = operations.get(subsystemAddress);
            Assert.assertNotNull(operations.toString(), operation);
            Assert.assertEquals(ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));

            ModelNode model = new ModelNode();
            model.get(subsystem.getPathElement().getKeyValuePair()).setEmptyObject();
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify multiple resources
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s"/>
                        <child name="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), childPaths[1].getValue());
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 3, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddresses[0], childAddresses[1]), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddresses[0]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[0].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertFalse(operations.toString(), repeatableElement.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());
            operation = operations.get(childAddresses[1]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[1].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertFalse(operations.toString(), repeatableElement.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());

            model.clear();
            ModelNode subsystemModel = new ModelNode();
            subsystemModel.get(childPaths[0].getKeyValuePair()).setEmptyObject();
            subsystemModel.get(childPaths[1].getKeyValuePair()).setEmptyObject();
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify repeatable elements
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s">
                            <property name="%s">%s</property>
                            <property name="%s">%s</property>
                        </child>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo", "bar", "baz", "qux");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddresses[0]), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddresses[0]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[0].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertTrue(operations.toString(), repeatableElement.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).isDefined());
            Assert.assertEquals(Map.of("foo", "bar", "baz", "qux"), repeatableElement.resolve(context, operation));

            model.clear();
            subsystemModel.clear();
            ModelNode childModel = subsystemModel.get(childPaths[0].getKeyValuePair());
            childModel.get(repeatableElement.getName()).add("foo", "bar");
            childModel.get(repeatableElement.getName()).add("baz", "qux");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Negative tests

            // Missing name attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child/>
                    </subsystem>
                    """, NAMESPACE_URI, "foo", "bar")));

            // Unexpected attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" unexpected="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo")));

            // Unexpected content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s">
                            <unexpected/>
                        </child>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue())));
        }
    }

    @Test
    public void override() throws XMLStreamException, OperationFailedException {
        SubsystemResourceRegistration subsystem = SubsystemResourceRegistration.of("test");
        ResourceRegistration child = ResourceRegistration.of(PathElement.pathElement("child"));
        ResourceRegistration override = ResourceRegistration.of(PathElement.pathElement("child", "override"));
        ResourceRegistration experimental = ResourceRegistration.of(PathElement.pathElement("child", "experimental"), Stability.EXPERIMENTAL);

        AttributeDefinition childAttribute = new SimpleAttributeDefinitionBuilder("attribute", ModelType.STRING).setRequired(true).build();
        AttributeDefinition overrideAttribute = new SimpleAttributeDefinitionBuilder("override", ModelType.STRING).setRequired(true).build();
        AttributeDefinition experimentalAttribute = new SimpleAttributeDefinitionBuilder("experimental", ModelType.STRING).setRequired(true).build();

        NamedResourceRegistrationXMLElement childElement = this.factory.namedElement(child).addAttribute(childAttribute).build();
        NamedResourceRegistrationXMLElement overrideElement = this.factory.namedElement(override).withElementLocalName(ResourceXMLElementLocalName.KEY).addAttribute(overrideAttribute).build();
        NamedResourceRegistrationXMLElement experimentalElement = this.factory.namedElement(experimental).withElementLocalName(ResourceXMLElementLocalName.KEY).addAttribute(experimentalAttribute).build();
        ResourceRegistrationXMLElement subsystemElement = this.factory.subsystemElement(subsystem).withContent(this.factory.namedChoice(childElement).addElement(overrideElement).addElement(experimentalElement).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build()).build();

        try (XMLElementTester<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> tester = XMLElementTester.of(subsystemElement, () -> Map.entry(PathAddress.EMPTY_ADDRESS, new LinkedHashMap<>()))) {
            PathAddress subsystemAddress = PathAddress.pathAddress(subsystem.getPathElement());
            PathElement[] childPaths = IntStream.range(0, 2).mapToObj(String::valueOf).map(value -> PathElement.pathElement(child.getPathElement().getKey(), value)).toArray(PathElement[]::new);
            PathAddress[] childAddresses = Stream.of(childPaths).map(subsystemAddress::append).toArray(PathAddress[]::new);
            PathAddress overrideAddress = subsystemAddress.append(override.getPathElement());

            // Positive tests

            // Verify zero children (choice is optional)
            String xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s"/>
                    """, NAMESPACE_URI);
            Map<PathAddress, ModelNode> operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 1, operations.size());
            ModelNode operation = operations.get(subsystemAddress);
            Assert.assertNotNull(operations.toString(), operation);
            Assert.assertEquals(ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));

            ModelNode model = new ModelNode();
            model.get(subsystem.getPathElement().getKeyValuePair()).setEmptyObject();
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify wildcard resource
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" attribute="%s"/>
                        <child name="%s" attribute="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo", childPaths[1].getValue(), "bar");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 3, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddresses[0], childAddresses[1]), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddresses[0]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[0].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", childAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operation.toString(), operation.hasDefined(overrideAttribute.getName()));
            operation = operations.get(childAddresses[1]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[1].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "bar", childAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operation.toString(), operation.hasDefined(overrideAttribute.getName()));

            model.clear();
            ModelNode subsystemModel = new ModelNode();
            subsystemModel.get(childPaths[0].getKeyValuePair()).get(childAttribute.getName()).set("foo");
            subsystemModel.get(childPaths[1].getKeyValuePair()).get(childAttribute.getName()).set("bar");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify override resource
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" override="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, override.getPathElement().getValue(), "foo");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 2, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, overrideAddress), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(overrideAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), overrideAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", overrideAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            Assert.assertFalse(operation.toString(), operation.hasDefined(childAttribute.getName()));

            model.clear();
            subsystemModel.clear();
            subsystemModel.get(override.getPathElement().getKeyValuePair()).get(overrideAttribute.getName()).set("foo");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Verify multiple wildcard/override children
            xml = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" attribute="%s"/>
                        <child name="%s" override="%s"/>
                        <child name="%s" attribute="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo", override.getPathElement().getValue(), "bar", childPaths[1].getValue(), "baz");
            operations = tester.readElement(xml).getValue();
            Assert.assertEquals(operations.toString(), 4, operations.size());
            // Verify order of operations
            Assert.assertEquals(operations.toString(), List.of(subsystemAddress, childAddresses[0], overrideAddress, childAddresses[1]), List.copyOf(operations.keySet()));
            operation = operations.get(subsystemAddress);
            Assert.assertEquals(ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(subsystemAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            operation = operations.get(childAddresses[0]);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[0].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "foo", childAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            operation = operations.get(overrideAddress);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), overrideAddress.toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "bar", overrideAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());
            operation = operations.get(childAddresses[1]);
            Assert.assertNotNull(operations.toString(), operation);
            Assert.assertEquals(operations.toString(), ModelDescriptionConstants.ADD, operation.get(ModelDescriptionConstants.OP).asStringOrNull());
            Assert.assertEquals(operations.toString(), childAddresses[1].toModelNode(), operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertEquals(operations.toString(), "baz", childAttribute.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, operation).asStringOrNull());

            model.clear();
            subsystemModel.clear();
            subsystemModel.get(childPaths[0].getKeyValuePair()).get(childAttribute.getName()).set("foo");
            subsystemModel.get(override.getPathElement().getKeyValuePair()).get(overrideAttribute.getName()).set("bar");
            subsystemModel.get(childPaths[1].getKeyValuePair()).get(childAttribute.getName()).set("baz");
            model.get(subsystem.getPathElement().getKeyValuePair()).set(subsystemModel);
            assertXMLEquals(xml, tester.writeElement(model));

            // Negative tests

            // Missing name attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child attribute="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue())));

            // Missing required attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue())));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, override.getPathElement().getValue())));

            // Unexpected attribute
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" override="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" attribute="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, override.getPathElement().getValue(), "foo")));

            // Unexpected content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" attribute="%s">
                            <unexpected/>
                        </child>
                    </subsystem>
                    """, NAMESPACE_URI, childPaths[0].getValue(), "foo")));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" override="%s">
                            <unexpected/>
                        </child>
                    </subsystem>
                    """, NAMESPACE_URI, override.getPathElement().getValue(), "foo")));

            // Experimental choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement(String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <subsystem xmlns="%s">
                        <child name="%s" experimental="%s"/>
                    </subsystem>
                    """, NAMESPACE_URI, experimental.getPathElement().getValue(), "foo")));
        }
    }

    private static void assertXMLEquals(String expected, String actual) {
        Assert.assertEquals(actual, trim(expected), trim(actual));
    }

    private static String trim(String xml) {
        // Trim whitespace between elements
        return xml.strip().replaceAll(Pattern.quote(">") + "\\s+" + Pattern.quote("<"), "><");
    }
}
