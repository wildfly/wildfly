/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.List;
import java.util.OptionalInt;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test validating read semantics of {@link XMLAll}.
 */
public class XMLAllTestCase implements FeatureRegistry {
    private final XMLParticleFactory<Void, Void> factory = XMLParticleFactory.newInstance(this);

    @Override
    public Stability getStability() {
        return Stability.COMMUNITY;
    }

    @Test
    public void testRequiredAll() throws XMLStreamException {
        QName container = new QName("container");
        QName required = new QName("required");
        QName optional = new QName("optional");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:all with minOccurs = 1, maxOccurs = 1
        XMLAll<Void, Void> all = this.factory.all()
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(all).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify all enabled elements present
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify that order does not matter
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify that optional elements need not be present
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Missing content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container))), null));

        // Missing required element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.end(container))), null));

        // Duplicate element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Unexpected content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Disabled content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));

        // Experimental content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testOptionalAll() throws XMLStreamException {
        QName container = new QName("container");
        QName required = new QName("required");
        QName optional = new QName("optional");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:all with minOccurs = 0, maxOccurs = 1
        XMLAll<Void, Void> all = this.factory.all().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(all).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify that content may be omitted
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify all enabled elements present
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify that order does not matter
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify that optional elements need not be present
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Missing required element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.end(container))), null));

        // Duplicate element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Unexpected content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Disabled content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));

        // Experimental content
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testDisabledAll() throws XMLStreamException {
        QName container = new QName("container");
        QName required = new QName("required");

        // Validate xs:all with minOccurs = 0, maxOccurs = 0
        XMLAll<Void, Void> all = this.factory.all().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(required).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(all).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify that content may be omitted
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Content not allowed
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testIllegalAll() {
        XMLAll.Builder<Void, Void> builder = this.factory.all();

        // xs:all does not allow repeated elements
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.addElement(this.factory.element(new QName("unbounded")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build()));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.addElement(this.factory.element(new QName("unbounded")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build()));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.addElement(this.factory.element(new QName("unbounded")).withCardinality(XMLCardinality.of(0, OptionalInt.of(2))).build()));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.addElement(this.factory.element(new QName("unbounded")).withCardinality(XMLCardinality.of(1, OptionalInt.of(2))).build()));
        // xs:all is not repeatable
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.withCardinality(XMLCardinality.Unbounded.OPTIONAL));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.withCardinality(XMLCardinality.Unbounded.REQUIRED));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.withCardinality(XMLCardinality.of(0, OptionalInt.of(2))));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.withCardinality(XMLCardinality.of(1, OptionalInt.of(2))));
    }
}
