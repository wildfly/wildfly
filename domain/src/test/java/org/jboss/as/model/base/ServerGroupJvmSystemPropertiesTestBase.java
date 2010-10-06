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

import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.ModelParsingSupport;

/**
 * Base class for unit tests of {@link PropertiesElement} as the set of
 * system properties in a server group {@link JvmElement}.
 *
 * @author Brian Stansberry
 */
public abstract class ServerGroupJvmSystemPropertiesTestBase extends DomainModelElementTestBase {

    /**
     * @param name
     */
    public ServerGroupJvmSystemPropertiesTestBase(String name) {
        super(name);
    }


    public void testBasicProperties() throws Exception {
        String fullcontent = getFullContent("<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\" value=\"value2\"/></system-properties>");
        PropertiesElement testee = getTestProperties(fullcontent);
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
        String fullcontent = getFullContent("<system-properties><property name=\"prop1\"/><property name=\"prop2\"/></system-properties>");
        PropertiesElement testee = getTestProperties(fullcontent);
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

    public void testMissingName() throws Exception {
        String fullcontent = getFullContent("<system-properties><property value=\"value1\"/></system-properties>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing name attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testBogusAttribute() throws Exception {
        String fullcontent = getFullContent("<system-properties><property name=\"prop1\" value=\"value1\" bogus=\"bad\"/></system-properties>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Bogus attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testBogusChild() throws Exception {
        String fullcontent = getFullContent("<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop1\" value=\"value1\"/></system-properties>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Bogus child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testNoChildren() throws Exception {
        String fullcontent = getFullContent("<system-properties/>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
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
        String fullcontent = getFullContent("<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\" value=\"value2\"/></system-properties>");
        PropertiesElement testee = getTestProperties(fullcontent);

        byte[] bytes = serialize(testee);
        PropertiesElement testee1 = deserialize(bytes, PropertiesElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());
        assertEquals(testee.size(), testee1.size());
        for (String name : testee.getPropertyNames()) {
            assertEquals("Property values match for " + name, testee.getProperty(name), testee1.getProperty(name));
        }
    }

    private String getFullContent(String testContent) {
        testContent = ModelParsingSupport.wrapJvm(testContent);
        testContent = ModelParsingSupport.wrapServerGroup(testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);
        return fullcontent;
    }

    private PropertiesElement getTestProperties(String fullcontent) throws XMLStreamException, FactoryConfigurationError,
            UpdateFailedException {
        DomainModel root = ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
        ServerGroupElement sge = root.getServerGroup("test");
        assertNotNull(sge);
        JvmElement jvm = sge.getJvm();
        assertNotNull(jvm);
        PropertiesElement testee = jvm.getSystemProperties();
        assertNotNull(testee);
        return testee;
    }

}
