/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.OptionalInt;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.FeatureRegistry;
import org.jboss.as.version.Stability;
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
        // Validate xs:all with minOccurs = 1, maxOccurs = 1
        XMLAll<Void, Void> all = this.factory.all()
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(all).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify all enabled elements present
            tester.readElement("<container><required/><optional/></container>");

            // Verify that order does not matter
            tester.readElement("<container><optional/><required/></container>");

            // Verify that optional elements need not be present
            tester.readElement("<container><required/></container>");

            // Negative tests

            // Missing content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container/>"));

            // Missing required element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/></container>"));

            // Duplicate element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><optional/><required/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><required/><optional/></container>"));

            // Unexpected content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><unexpected/></container>"));

            // Disabled content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><disabled/></container>"));

            // Preview content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><preview/></container>"));

            // Experimental content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><experimental/></container>"));
        }
    }

    @Test
    public void testOptionalAll() throws XMLStreamException {
        // Validate xs:all with minOccurs = 0, maxOccurs = 1
        XMLAll<Void, Void> all = this.factory.all().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.factory.element(new QName("required"), Stability.COMMUNITY).build())
                .addElement(this.factory.element(new QName("optional")).withCardinality(XMLCardinality.Single.OPTIONAL).build())
                .addElement(this.factory.element(new QName("preview"), Stability.PREVIEW).build())
                .addElement(this.factory.element(new QName("experimental"), Stability.EXPERIMENTAL).build())
                .addElement(this.factory.element(new QName("disabled")).withCardinality(XMLCardinality.DISABLED).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(all).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify that content may be omitted
            tester.readElement("<container/>");

            // Verify all enabled elements present
            tester.readElement("<container><required/><optional/></container>");

            // Verify that order does not matter
            tester.readElement("<container><optional/><required/></container>");

            // Verify that optional elements need not be present
            tester.readElement("<container><required/></container>");

            // Negative tests

            // Missing required element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/></container>"));

            // Duplicate element
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><optional/><required/></container>"));
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><optional/><required/><optional/></container>"));

            // Unexpected content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><unexpected/></container>"));

            // Disabled content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><disabled/></container>"));

            // Preview content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><preview/></container>"));

            // Experimental content
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/><experimental/></container>"));
        }
    }

    @Test
    public void testDisabledAll() throws XMLStreamException {
        // Validate xs:all with minOccurs = 0, maxOccurs = 0
        XMLAll<Void, Void> all = this.factory.all().withCardinality(XMLCardinality.DISABLED)
                .addElement(this.factory.element(new QName("required")).build())
                .build();
        XMLElement<Void, Void> containerElement = this.factory.element(new QName("container")).withContent(all).build();

        try (XMLElementTester<Void, Void> tester = XMLElementTester.of(containerElement)) {
            // Positive tests

            // Verify that content may be omitted
            tester.readElement("<container/>");

            // Negative tests

            // Content not allowed
            Assert.assertThrows(XMLStreamException.class, () -> tester.readElement("<container><required/></container>"));
        }
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
