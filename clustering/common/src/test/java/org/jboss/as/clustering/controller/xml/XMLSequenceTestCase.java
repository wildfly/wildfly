/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test validating read semantics of {@link XMLSequence}.
 */
public class XMLSequenceTestCase implements FeatureRegistry {
    private final XMLParticleFactory<Void, Void> factory = XMLParticleFactory.newInstance(this);

    @Override
    public Stability getStability() {
        return Stability.COMMUNITY;
    }

    @Test
    public void testRequiredSequence() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:sequence with minOccurs = 1, maxOccurs = 1
        XMLSequence<Void, Void> sequence = this.factory.sequence()
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(sequence).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Missing sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container))), null));

        // Incomplete sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Out-of-order sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable elements
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Unexpected element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Experimental element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));

        // Disabled element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testOptionalSequence() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:sequence with minOccurs = 0, maxOccurs = 1
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(sequence).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify empty sequence
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Incomplete sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Out-of-order sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable elements
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Unexpected element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Experimental element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));

        // Disabled element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testRepeatableSequence() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:sequence with minOccurs = 0, maxOccurs = unbounded
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(sequence).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify empty sequence
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal repeated sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Incomplete sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Incomplete repeated sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Out-of-order sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable elements
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Unexpected element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Experimental element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));

        // Disabled element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testRepeatedSequence() throws XMLStreamException {
        QName container = new QName("container");
        QName optional = new QName("optional");
        QName required = new QName("required");
        QName repeatable = new QName("repeatable");
        QName repeated = new QName("repeated");
        QName experimental = new QName("experimental");
        QName disabled = new QName("disabled");
        QName unexpected = new QName("unexpected");

        // Validate xs:sequence with minOccurs = 1, maxOccurs = unbounded
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.factory.element(optional).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(required).build())
                .addElement(this.factory.element(repeatable).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(repeated).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(experimental, Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(disabled).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(sequence).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Positive tests

        // Verify sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of permissible elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify minimal repeated sequence of required elements only
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of required repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Verify repeated sequence of repeatable elements
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(optional),
                XMLStreamEvent.end(optional),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeatable),
                XMLStreamEvent.end(repeatable),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Missing sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container))), null));

        // Incomplete sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Incomplete repeated sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Out-of-order sequence
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));

        // Non-repeatable elements
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.end(container))), null));

        // Unexpected element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(unexpected),
                XMLStreamEvent.end(unexpected),
                XMLStreamEvent.end(container))), null));

        // Experimental element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(experimental),
                XMLStreamEvent.end(experimental),
                XMLStreamEvent.end(container))), null));

        // Disabled element
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.start(repeated),
                XMLStreamEvent.end(repeated),
                XMLStreamEvent.start(disabled),
                XMLStreamEvent.end(disabled),
                XMLStreamEvent.end(container))), null));
    }

    @Test
    public void testDisabledSequence() throws XMLStreamException {
        QName container = new QName("container");
        QName required = new QName("required");

        // Validate xs:sequence with minOccurs = 0, maxOccurs = 0
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(required).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(container).withContent(sequence).build();
        XMLElementReader<Void> containerReader = containerElement.getReader();

        // Verify empty seqeuence
        try (SimpleXMLExtendedStreamReader reader = new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.end(container)))) {
            containerReader.readElement(reader, null);
        }

        // Negative tests

        // Sequence not enabled
        Assert.assertThrows(XMLStreamException.class, () -> containerReader.readElement(new SimpleXMLExtendedStreamReader(List.of(
                XMLStreamEvent.start(container),
                XMLStreamEvent.start(required),
                XMLStreamEvent.end(required),
                XMLStreamEvent.end(container))), null));
    }
}
