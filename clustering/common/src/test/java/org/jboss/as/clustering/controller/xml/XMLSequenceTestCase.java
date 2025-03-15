/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
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
        // Validate xs:sequence with minOccurs = 1, maxOccurs = 1
        XMLSequence<Void, Void> sequence = this.factory.sequence()
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(sequence).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");

            // Verify minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/></container>");

            // Verify sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/></container>");

            // Verify sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Negative tests

            // Missing sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container/>"));

            // Incomplete sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));

            // Out-of-order sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><repeated/><required/></container>"));

            // Non-repeatable elements
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/><repeated/><repeated/></container>"));

            // Non-repeatable sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><required/><repeated/></container>"));

            // Unexpected element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><unexpected/></container>"));

            // Preview element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><preview/></container>"));

            // Experimental element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><experimental/></container>"));

            // Disabled element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><disabled/></container>"));
        }
    }

    @Test
    public void testOptionalSequence() throws XMLStreamException {
        // Validate xs:sequence with minOccurs = 0, maxOccurs = 1
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(sequence).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify optional sequence
            tester.readElement("<container/>");

            // Verify sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");

            // Verify minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/></container>");

            // Verify sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/></container>");

            // Verify sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Negative tests

            // Incomplete sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));

            // Out-of-order sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><repeated/><required/></container>"));

            // Non-repeatable elements
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/><repeated/><repeated/></container>"));

            // Non-repeatable sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><required/><repeated/></container>"));

            // Unexpected element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><unexpected/></container>"));

            // Preview element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><preview/></container>"));

            // Experimental element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><experimental/></container>"));

            // Disabled element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><disabled/></container>"));
        }
    }

    @Test
    public void testRepeatableSequence() throws XMLStreamException {
        // Validate xs:sequence with minOccurs = 0, maxOccurs = unbounded
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(sequence).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify optional sequence
            tester.readElement("<container/>");

            // Verify sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");

            // Verify repeated sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/><optional/><required/><repeatable/><repeated/></container>");

            // Verify minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/></container>");

            // Verify repeated minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/><required/><repeated/></container>");

            // Verify sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/></container>");

            // Verify repeated sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/><required/><repeated/><repeated/></container>");

            // Verify sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Verify repeated sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Negative tests

            // Incomplete sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));

            // Incomplete repeated sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><required/></container>"));

            // Out-of-order sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><repeated/><required/></container>"));

            // Non-repeatable elements
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/><repeated/><repeated/></container>"));

            // Unexpected element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><unexpected/></container>"));

            // Preview element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><preview/></container>"));

            // Experimental element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><experimental/></container>"));

            // Disabled element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><disabled/></container>"));
        }
    }

    @Test
    public void testRepeatedSequence() throws XMLStreamException {
        // Validate xs:sequence with minOccurs = 1, maxOccurs = unbounded
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(sequence).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");

            // Verify repeated sequence of permissible elements
            tester.readElement("<container><optional/><required/><repeatable/><repeated/><optional/><required/><repeatable/><repeated/></container>");

            // Verify minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/></container>");

            // Verify repeated minimal sequence of required elements only
            tester.readElement("<container><required/><repeated/><required/><repeated/></container>");

            // Verify sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/></container>");

            // Verify repeated sequence of required repeatable elements
            tester.readElement("<container><required/><repeated/><repeated/><required/><repeated/><repeated/></container>");

            // Verify sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Verify repeated sequence of permissible, repeatable elements
            tester.readElement("<container><optional/><required/><repeatable/><repeatable/><repeated/><repeated/><optional/><required/><repeatable/><repeatable/><repeated/><repeated/></container>");

            // Negative tests

            // Missing sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container/>"));

            // Incomplete sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));

            // Incomplete repeated sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><required/></container>"));

            // Out-of-order sequence
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><repeated/><required/></container>"));

            // Non-repeatable elements
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/><repeated/><repeated/></container>"));

            // Unexpected element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><unexpected/></container>"));

            // Preview element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><preview/></container>"));

            // Experimental element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><experimental/></container>"));

            // Disabled element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><repeated/><disabled/></container>"));
        }
    }

    @Test
    public void testDisabledSequence() throws XMLStreamException {
        // Validate xs:sequence with minOccurs = 0, maxOccurs = 0
        XMLSequence<Void, Void> sequence = this.factory.sequence().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(new QName("required")).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(sequence).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify empty sequence
            tester.readElement("<container/>");

            // Negative tests

            // Sequence not enabled
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));
        }
    }
}
