/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Element;
import org.jboss.as.model.ProfileIncludeElement;
import org.jboss.as.model.base.util.MockRootElement;
import org.jboss.as.model.base.util.MockRootElementParser;
import org.jboss.as.model.base.util.TestXMLElementReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link ProfileIncludeElement}.
 *
 * @author Brian Stansberry
 */
public abstract class ProfileIncludeElementTestBase extends DomainModelElementTestBase {

    /**
     * @param name
     */
    public ProfileIncludeElementTestBase(String name) {
        super(name);
    }

    protected XMLMapper createXMLMapper() throws Exception{

        XMLMapper mapper = XMLMapper.Factory.create();
        MockRootElementParser.registerXMLElementReaders(mapper, getTargetNamespace());
        mapper.registerRootElement(new QName(getTargetNamespace(), Element.INCLUDE.getLocalName()),
                new TestXMLElementReader<ProfileIncludeElement>(ProfileIncludeElement.class));
        return mapper;
    }

    public void testSimpleParse() throws Exception {
        String testContent = "<include profile=\"foo\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        ProfileIncludeElement testee = (ProfileIncludeElement) root.getChild(getTargetNamespace(), Element.INCLUDE.getLocalName());
        assertEquals("foo", testee.getProfile());
    }

    public void testNoProfileParse() throws Exception {
        String testContent = "<include/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);

        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing 'profile' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadAttributeParse() throws Exception {
        String testContent = "<include name=\"foo\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);

        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }

        testContent = "<include profile=\"foo\" name=\"bar\"/>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);

        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadChildElement() throws Exception {
        String testContent = "<include profile=\"foo\"><bogus/></include>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);

        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testSerializationDeserialization() throws Exception {
        String testContent = "<include profile=\"foo\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        ProfileIncludeElement testee = (ProfileIncludeElement) root.getChild(getTargetNamespace(), Element.INCLUDE.getLocalName());

        byte[] bytes = serialize(testee);
        ProfileIncludeElement testee1 = deserialize(bytes, ProfileIncludeElement.class);

        assertEquals(testee.getProfile(), testee1.getProfile());
    }

//    public void testXMLRoundTrip() throws Exception {
//        String testContent = "<include profile=\"foo\"/>";
//        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
//        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
//        ProfileIncludeElement testee = (ProfileIncludeElement) root.getChild(getTargetNamespace(), Element.INCLUDE.getLocalName());
//
//        XMLExtendedStreamWriter xmlWriter = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(new StringWriter()));
//        xmlWriter.setDefaultNamespace(getTargetNamespace());
//        xmlWriter.writeStartElement(MockRootElement.ELEMENT_NAME);
//        xmlWriter.writeDefaultNamespace(getTargetNamespace());
//        root.writeContent(xmlWriter);
//        xmlWriter.close();
//
//        MockRootElement root1 = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(writer.toString()));
//        ProfileIncludeElement testee1 = (ProfileIncludeElement) root1.getChild(getTargetNamespace(), Element.INCLUDE.getLocalName());
//
//        assertEquals(testee.elementHash(), testee1.elementHash());
//        assertEquals(testee.getProfile(), testee1.getProfile());
//    }

}
