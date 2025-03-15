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
        // Validate xs:choice with minOccurs = 1, maxOccurs = 1
        XMLChoice<Void, Void> choice = this.factory.choice()
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(choice).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify each permissible element
            tester.readElement("<container><optional/></container>");
            tester.readElement("<container><required/></container>");
            tester.readElement("<container><repeatable/></container>");
            tester.readElement("<container><repeated/></container>");

            // Verify permissible repeatable elements
            tester.readElement("<container><repeatable/><repeatable/></container>");
            tester.readElement("<container><repeated/><repeated/></container>");

            // Negative tests

            // Missing choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container/>"));

            // Multiple choices
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><required/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><optional/></container>"));

            // Non-repeatable choices
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><optional/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/></container>"));

            // Unexpected choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><unexpected/></container>"));

            // Preview choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><preview/></container>"));

            // Experimental choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><experimental/></container>"));

            // Disabled choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><disabled/></container>"));
        }
    }

    @Test
    public void testOptionalChoice() throws XMLStreamException {
        // Validate xs:choice with minOccurs = 0, maxOccurs = 1
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(choice).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify empty choice
            tester.readElement("<container/>");

            // Verify each permissible element
            tester.readElement("<container><optional/></container>");
            tester.readElement("<container><required/></container>");
            tester.readElement("<container><repeatable/></container>");
            tester.readElement("<container><repeated/></container>");

            // Verify permissible repeatable elements
            tester.readElement("<container><repeatable/><repeatable/></container>");
            tester.readElement("<container><repeated/><repeated/></container>");

            // Negative tests

            // Multiple choices
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><required/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><optional/></container>"));

            // Non-repeatable choices
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><optional/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><required/></container>"));

            // Unexpected choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><unexpected/></container>"));

            // Preview choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><preview/></container>"));

            // Experimental choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><experimental/></container>"));

            // Disabled choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><disabled/></container>"));
        }
    }

    @Test
    public void testRepeatableChoice() throws XMLStreamException {
        // Validate xs:choice with minOccurs = 0, maxOccurs = unbounded
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(choice).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify empty choice
            tester.readElement("<container/>");

            // Verify each permissible element
            tester.readElement("<container><optional/></container>");
            tester.readElement("<container><required/></container>");
            tester.readElement("<container><repeatable/></container>");
            tester.readElement("<container><repeated/></container>");

            // Verify all permissible choices are effectively repeatable, since the choice is repeatable
            tester.readElement("<container><optional/><optional/></container>");
            tester.readElement("<container><required/><required/></container>");
            tester.readElement("<container><repeatable/><repeatable/></container>");
            tester.readElement("<container><repeated/><repeated/></container>");

            // Verify permissible elements may be present any number of times, in any order, since the choice is repeatable
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");
            tester.readElement("<container><repeated/><repeatable/><required/><optional/></container>");
            tester.readElement("<container><optional/><required/><repeatable/><repeated/><repeated/><repeatable/><required/><optional/><optional/><required/><repeatable/><repeated/></container>");

            // Negative tests

            // Unexpected choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><unexpected/></container>"));

            // Preview choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><preview/></container>"));

            // Experimental choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><experimental/></container>"));

            // Disabled choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><disabled/></container>"));
        }
    }

    @Test
    public void testRepeatedChoice() throws XMLStreamException {
        // Validate xs:choice with minOccurs = 0, maxOccurs = unbounded
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("repeatable")).withCardinality(XMLCardinality.Unbounded.OPTIONAL).build())
                .addElement(this.factory.element(new QName("repeated")).withCardinality(XMLCardinality.Unbounded.REQUIRED).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(choice).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify each permissible element
            tester.readElement("<container><optional/></container>");
            tester.readElement("<container><required/></container>");
            tester.readElement("<container><repeatable/></container>");
            tester.readElement("<container><repeated/></container>");

            // Verify all permissible choices are effectively repeatable, since the choice is repeatable
            tester.readElement("<container><optional/><optional/></container>");
            tester.readElement("<container><required/><required/></container>");
            tester.readElement("<container><repeatable/><repeatable/></container>");
            tester.readElement("<container><repeated/><repeated/></container>");

            // Verify permissible elements may be present any number of times, in any order, since the choice is repeatable
            tester.readElement("<container><optional/><required/><repeatable/><repeated/></container>");
            tester.readElement("<container><repeated/><repeatable/><required/><optional/></container>");
            tester.readElement("<container><optional/><required/><repeatable/><repeated/><repeated/><repeatable/><required/><optional/><optional/><required/><repeatable/><repeated/></container>");

            // Negative tests

            // Missing choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container/>"));

            // Unexpected choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><unexpected/></container>"));

            // Preview choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><preview/></container>"));

            // Experimental choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><experimental/></container>"));

            // Disabled choice
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><disabled/></container>"));
        }
    }

    @Test
    public void testDisabledChoice() throws XMLStreamException {
        // Validate xs:all with minOccurs = 0, maxOccurs = 0
        XMLChoice<Void, Void> choice = this.factory.choice().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(new QName("required")).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(choice).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify empty choice
            tester.readElement("<container/>");

            // Negative tests

            // Choice not enabled
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));
        }
    }
}
