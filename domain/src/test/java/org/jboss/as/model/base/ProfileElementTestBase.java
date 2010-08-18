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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.Element;
import org.jboss.as.model.ProfileElement;
import org.jboss.as.model.ProfileIncludeElement;
import org.jboss.as.model.RefResolver;
import org.jboss.as.model.base.util.MockAnyElement;
import org.jboss.as.model.base.util.MockAnyElementParser;
import org.jboss.as.model.base.util.MockRootElement;
import org.jboss.as.model.base.util.MockRootElementParser;
import org.jboss.as.model.base.util.ReadElementCallback;
import org.jboss.as.model.base.util.TestXMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link ProfileElement}.
 * 
 * @author Brian Stansberry
 */
public abstract class ProfileElementTestBase extends DomainModelElementTestBase {

    private static final RefResolver<String, ProfileElement> refResolver = new RefResolver<String, ProfileElement>() {

        private static final long serialVersionUID = 1L;

        @Override
        public ProfileElement resolveRef(String ref) {
            return null;
        }
        
    };
    
    private static final ReadElementCallback<ProfileElement> callback = new ReadElementCallback<ProfileElement>() {

        @Override
        public ProfileElement readElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            return new ProfileElement(reader, refResolver);
        }
        
    };
    
    
    /**
     * @param name
     */
    public ProfileElementTestBase(String name) {
        super(name);
    }
    
    protected XMLMapper createXMLMapper() throws Exception{

        XMLMapper mapper = XMLMapper.Factory.create();
        MockRootElementParser.registerXMLElementReaders(mapper, getTargetNamespace());
        mapper.registerRootElement(new QName(getTargetNamespace(), Element.PROFILE.getLocalName()), 
                new TestXMLElementReader<ProfileElement>(callback));  
        MockAnyElementParser.registerXMLElementReaders(mapper);
        return mapper;
    }

    public void testSimpleParse() throws Exception {
        String testContent = "<profile name=\"test\">" + MockAnyElement.getFullXmlContent() + "</profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        ProfileElement testee = (ProfileElement) root.getChild(getTargetNamespace(), Element.PROFILE.getLocalName());
        assertEquals("test", testee.getName());
        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee.getSubsystems();
        assertEquals(2, subsystems.size());
        boolean gotMock = false;
        boolean gotAnotherMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockAnyElement.MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotMock = true;
            }
            else if (MockAnyElement.ANOTHER_MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotAnotherMock = true;
            }
            else {
                fail("Unknown subsystem QName " + subsystem.getElementName());
            }
        }
        assertTrue(gotMock);
        assertTrue(gotAnotherMock);
    }

    public void testParseWithInclude() throws Exception {
        String testContent = "<profile name=\"test\"><include profile=\"foo\"/>" + MockAnyElement.getFullXmlContent() + "</profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        ProfileElement testee = (ProfileElement) root.getChild(getTargetNamespace(), Element.PROFILE.getLocalName());
        assertEquals("test", testee.getName());
        Set<ProfileIncludeElement> includes = testee.getIncludedProfiles();
        assertEquals(1, includes.size());
        ProfileIncludeElement include = includes.iterator().next();
        assertEquals("foo", include.getProfile());
        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee.getSubsystems();
        assertEquals(2, subsystems.size());
        boolean gotMock = false;
        boolean gotAnotherMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockAnyElement.MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotMock = true;
            }
            else if (MockAnyElement.ANOTHER_MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotAnotherMock = true;
            }
            else {
                fail("Unknown subsystem QName " + subsystem.getElementName());
            }
        }
        assertTrue(gotMock);
        assertTrue(gotAnotherMock);
    }
    
    public void testNoNameParse() throws Exception {
        String testContent = "<profile>" + MockAnyElement.getSimpleXmlContent() + "</profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testBadAttributeParse() throws Exception {
        String testContent = "<profile bogus=\"bogus\">" + MockAnyElement.getSimpleXmlContent() + "</profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
        
        testContent = "<profile name=\"test\" bogus=\"bogus\">" + MockAnyElement.getSimpleXmlContent() + "</profile>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testNoSubsystemParse() throws Exception {
        String testContent = "<profile name=\"test\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing children did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
        
        testContent = "<profile name=\"test\"><include profile=\"foo\"/></profile>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing children did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadChildElement() throws Exception {
        String testContent = "<profile name=\"test\"><bogus/></profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
        
        testContent = "<profile name=\"test\">" + MockAnyElement.getSimpleXmlContent() + "<bogus/></profile>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testSerializationDeserialization() throws Exception {
        String testContent = "<profile name=\"test\"><include profile=\"foo\"/>" + MockAnyElement.getFullXmlContent() + "</profile>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), true, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        ProfileElement testee = (ProfileElement) root.getChild(getTargetNamespace(), Element.PROFILE.getLocalName());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(testee);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ProfileElement testee1 = (ProfileElement) ois.readObject();
        bais.close();
        assertEquals(testee.elementHash(), testee1.elementHash());
        assertEquals(testee.getName(), testee1.getName());
        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee1.getSubsystems();
        assertEquals(2, subsystems.size());
        boolean gotMock = false;
        boolean gotAnotherMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockAnyElement.MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotMock = true;
            }
            else if (MockAnyElement.ANOTHER_MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotAnotherMock = true;
            }
            else {
                fail("Unknown subsystem QName " + subsystem.getElementName());
            }
        }
        assertTrue(gotMock);
        assertTrue(gotAnotherMock);
    }

}
