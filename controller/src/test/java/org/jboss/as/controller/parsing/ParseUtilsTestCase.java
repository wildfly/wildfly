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

package org.jboss.as.controller.parsing;

import java.io.InputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link ParseUtils}.
 * 
 * @author steve.coy
 */
public class ParseUtilsTestCase {

    @Test
    public void testRequireNoAttributesWithoutSchemaLocation() throws XMLStreamException, FactoryConfigurationError {
        InputStream xmlInput = ParseUtilsTestCase.class.getResourceAsStream("testRequireNoAttributesWithoutSchemaLocation.xml");
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlInput);
        try {
            xmlReader.nextTag();
            ParseUtils.requireNoAttributes(xmlReader);
        } finally {
            xmlReader.close();
        }
    }

    @Test
    public void testRequireNoAttributesWithoutSchemaLocationFail() throws XMLStreamException, FactoryConfigurationError {
        InputStream xmlInput = ParseUtilsTestCase.class.getResourceAsStream("testRequireNoAttributesWithoutSchemaLocationFail.xml");
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlInput);
        try {
            xmlReader.nextTag();
            try {
                ParseUtils.requireNoAttributes(xmlReader);
                Assert.fail("XMLStreamException expected");
            } catch (XMLStreamException e) {
                Assert.assertTrue("Unexpected attribute", e.getMessage().contains("JBAS014788"));
            }
        } finally {
            xmlReader.close();
        }
    }

    @Test
    public void testRequireNoAttributesWithSchemaLocation() throws XMLStreamException, FactoryConfigurationError {
        InputStream xmlInput = ParseUtilsTestCase.class.getResourceAsStream("testRequireNoAttributesWithSchemaLocation.xml");
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlInput);
        try {
            xmlReader.nextTag();
            ParseUtils.requireNoAttributes(xmlReader);
        } finally {
            xmlReader.close();
        }
    }

    @Test
    public void testRequireNoAttributesWithSchemaLocationFail() throws XMLStreamException, FactoryConfigurationError {
        InputStream xmlInput = ParseUtilsTestCase.class.getResourceAsStream("testRequireNoAttributesWithSchemaLocationFail.xml");
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlInput);
        try {
            xmlReader.nextTag();
            try {
                ParseUtils.requireNoAttributes(xmlReader);
                Assert.fail("XMLStreamException expected");
            } catch (XMLStreamException e) {
                Assert.assertTrue("Unexpected attribute", e.getMessage().contains("JBAS014788"));
            }
        } finally {
            xmlReader.close();
        }
    }
}
