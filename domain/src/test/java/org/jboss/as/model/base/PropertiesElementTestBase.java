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
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Element;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.base.util.MockAnyElementParser;
import org.jboss.as.model.base.util.MockRootElement;
import org.jboss.as.model.base.util.MockRootElementParser;
import org.jboss.as.model.base.util.ReadElementCallback;
import org.jboss.as.model.base.util.TestXMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link PropertiesElement}.
 * 
 * @author Brian Stansberry
 */
public abstract class PropertiesElementTestBase extends DomainModelElementTestBase {

    /** Test methods can change the value of this ref to control how 'callback' works */
    private boolean allowNullValue = true;
    /** Test methods can change the value of this ref to control how 'callback' works */
    private  Element propertyType = Element.PROPERTY;
    
    /** Callback that creates a PropertiesElement configured per the values in the above Atomic fields */
    private final ReadElementCallback<PropertiesElement> callback = new ReadElementCallback<PropertiesElement>() {
        @Override
        public PropertiesElement readElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            return new PropertiesElement(reader, propertyType, allowNullValue);
        }
        
    };
    
    /**
     * @param name
     */
    public PropertiesElementTestBase(String name) {
        super(name);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#createXMLMapper()
     */
    @Override
    protected XMLMapper createXMLMapper() throws Exception {

        XMLMapper mapper = XMLMapper.Factory.create();
        MockRootElementParser.registerXMLElementReaders(mapper, getTargetNamespace());
        mapper.registerRootElement(new QName(getTargetNamespace(), Element.SYSTEM_PROPERTIES.getLocalName()), 
                new TestXMLElementReader<PropertiesElement>(callback));  
        MockAnyElementParser.registerXMLElementReaders(mapper);
        return mapper;
    }
    
    public void testBasicProperties() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\" value=\"value2\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = true;
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        PropertiesElement testee = (PropertiesElement) root.getChild(getTargetNamespace(), Element.SYSTEM_PROPERTIES.getLocalName());
        Map<String, String> props = testee.getProperties();
        assertEquals(2, props.size());
        assertEquals("value1", props.get("prop1"));
        assertEquals("value1", testee.getProperty("prop1"));
        assertEquals("value2", props.get("prop2"));
        assertEquals("value2", testee.getProperty("prop2"));
        Set<String> names = testee.getPropertyNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("prop1"));
        assertTrue(names.contains("prop2"));
    }
    
    public void testNullProperties() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\"/><property name=\"prop2\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = true;
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        PropertiesElement testee = (PropertiesElement) root.getChild(getTargetNamespace(), Element.SYSTEM_PROPERTIES.getLocalName());
        Map<String, String> props = testee.getProperties();
        assertEquals(2, props.size());
        assertNull(props.get("prop1"));
        assertNull(testee.getProperty("prop1"));
        assertNull(props.get("prop2"));
        assertNull(testee.getProperty("prop2"));
        Set<String> names = testee.getPropertyNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("prop1"));
        assertTrue(names.contains("prop2"));
    }
    
    public void testRejectNullProperties() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = false;
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
            fail("Missing property value did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }
    
    public void testMissingName() throws Exception {
        String testContent = "<system-properties><property value=\"value1\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = false;
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
            fail("Missing name attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }        
    }
    
    public void testBogusAttribute() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\" value=\"value1\" bogus=\"bad\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = false;
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
            fail("Bogus attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }        
    }
    
    public void testBogusChild() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\" value=\"value1\"/><variable name=\"prop1\" value=\"value1\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = false;
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
            fail("Bogus child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }        
    }
    
    public void testNoChildren() throws Exception {
        String testContent = "<system-properties/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = false;
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
            fail("Missing child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }        
        
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#testSerializationDeserialization()
     */
    @Override
    public void testSerializationDeserialization() throws Exception {
        String testContent = "<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\"/></system-properties>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        propertyType = Element.PROPERTY;
        allowNullValue = true;
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        PropertiesElement testee = (PropertiesElement) root.getChild(getTargetNamespace(), Element.SYSTEM_PROPERTIES.getLocalName());
        
        byte[] bytes = serialize(testee);        
        PropertiesElement testee1 = deserialize(bytes, PropertiesElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());        
        assertEquals(testee.size(), testee1.size());
        for (String name : testee.getPropertyNames())
        {
            assertEquals("Property values match for " + name, testee.getProperty(name), testee1.getProperty(name));
        }
        
        testContent = "<system-properties><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop2\" value=\"value2\"/></system-properties>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        System.out.println(fullcontent);
        propertyType = Element.VARIABLE;
        allowNullValue = false;
        root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        testee = (PropertiesElement) root.getChild(getTargetNamespace(), Element.SYSTEM_PROPERTIES.getLocalName());
        
        bytes = serialize(testee);        
        testee1 = deserialize(bytes, PropertiesElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());        
        assertEquals(testee.size(), testee1.size());
        for (String name : testee.getPropertyNames())
        {
            assertEquals("Property values match for " + name, testee.getProperty(name), testee1.getProperty(name));
        }
    }

}
