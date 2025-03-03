/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test validating read semantics of {@link XMLChoice}.
 */
public class XMLChoiceTestCase implements FeatureRegistry {
    private final XMLParticleFactory<Void, Void> factory = XMLParticleFactory.newInstance(this);

    @Override
    public Stability getStability() {
        return Stability.COMMUNITY;
    }

    @Test
    public void testRequiredChoice() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:choice with minOccurs = 1, maxOccurs = 1
        XMLChoice<Void, Void> choice = this.factory.choice()
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(choice).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify each permissible element
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible repeatable elements
        for (QName element : Set.of(repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Negative tests

        // Missing choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable choices
        for (QName element : Set.of(optional, required)) {
            Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container))), null));
        }

        // Unexpected choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Experimental choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));

        // Disabled choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testOptionalChoice() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:choice with minOccurs = 0, maxOccurs = 1
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(choice).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify empty choice
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify permissible choices
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible repeatable choices
        for (QName element : Set.of(repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Negative tests

        // Non-repeatable choices
        for (QName element : Set.of(optional, required)) {
            Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container))), null));
        }

        // Unexpected choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Disabled choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));

        // Experimental choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testRepeatableChoice() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:choice with minOccurs = 0, maxOccurs = unbounded
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(choice).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify empty choice
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify permissible choices
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible choices are effectively repeatable, since the choice is repeatable
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible elements may be present any number of times, in any order, since the choice is repeatable
        List<QName> elements = List.of(optional, required, repeatable, repeated);
        List<XMLStreamEvent> events = new LinkedList<>();
        events.add(XMLStreamEvent.start(container));
        Random random = new Random();
        for (int i = 0; i < 100; ++i) {
            QName element = elements.get(random.nextInt(elements.size()));
            events.add(XMLStreamEvent.start(element));
            events.add(XMLStreamEvent.end(element));
        }
        events.add(XMLStreamEvent.end(container));
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(events)) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Unexpected choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Disabled choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));

        // Experimental choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testRepeatedChoice() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:choice with minOccurs = 0, maxOccurs = unbounded
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(choice).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify permissible choices
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible choices are effectively repeatable, since the choice is repeatable
        for (QName element : Set.of(optional, required, repeatable, repeated)) {
            try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                    XMLStreamEvent.start(container),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.start(element),
                    XMLStreamEvent.end(element),
                    XMLStreamEvent.end(container)))) {
                containerReader.readElement(reader, null);
            }
        }

        // Verify permissible elements may be present any number of times, in any order, since the choice is repeatable
        List<QName> elements = List.of(optional, required, repeatable, repeated);
        List<XMLStreamEvent> events = new LinkedList<>();
        events.add(XMLStreamEvent.start(container));
        Random random = new Random();
        for (int i = 0; i < 100; ++i) {
            QName element = elements.get(random.nextInt(elements.size()));
            events.add(XMLStreamEvent.start(element));
            events.add(XMLStreamEvent.end(element));
        }
        events.add(XMLStreamEvent.end(container));
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(events)) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Missing choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container))), null));

        // Unexpected choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Disabled choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));

        // Experimental choice
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testDisabledChoice() throws XMLStreamException {
        QName container = new QName("container");
        QName required = new QName("required");

        // Validate xs:all with minOccurs = 0, maxOccurs = 0
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(required).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(choice).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Verify empty choice
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Choice not enabled
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));
    }
}
