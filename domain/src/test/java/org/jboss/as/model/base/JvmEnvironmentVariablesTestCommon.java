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

import junit.framework.Assert;

import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.ModelParsingSupport;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link PropertiesElement} as the set of
 * environment variables in a server group {@link JvmElement}.
 *
 * @author Brian Stansberry
 * @author Kabir Khan
 */
public class JvmEnvironmentVariablesTestCommon extends Assert {

    ContentAndPropertiesGetter getter;

    public JvmEnvironmentVariablesTestCommon(ContentAndPropertiesGetter getter) {
        this.getter = getter;
    }

    public void testBasicProperties() throws Exception {
        String fullcontent = getter.getFullContent("<environment-variables><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop2\" value=\"value2\"/></environment-variables>");
        PropertiesElement testee = getter.getTestProperties(fullcontent);
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
        String fullcontent = getter.getFullContent("<environment-variables><variable name=\"prop1\"/><variable name=\"prop2\"/></environment-variables>");
        PropertiesElement testee = getter.getTestProperties(fullcontent);
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
        String fullcontent = getter.getFullContent("<environment-variables><variable value=\"value1\"/></environment-variables>");

        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Missing name attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testBogusAttribute() throws Exception {
        String fullcontent = getter.getFullContent("<environment-variables><variable name=\"prop1\" value=\"value1\" bogus=\"bad\"/></environment-variables>");

        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Bogus attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testBogusChild() throws Exception {
        String fullcontent = getter.getFullContent("<environment-variables><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop1\" value=\"value1\"/></environment-variables>");

        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Bogus child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testNoChildren() throws Exception {
        String fullcontent = getter.getFullContent("<environment-variables/>");

        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Missing child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }

    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#testSerializationDeserialization()
     */
    public void testSerializationDeserialization() throws Exception {
        String fullcontent = getter.getFullContent("<environment-variables><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop2\" value=\"value2\"/></environment-variables>");
        PropertiesElement testee = getter.getTestProperties(fullcontent);

        byte[] bytes = DomainModelElementTestBase.serialize(testee);
        PropertiesElement testee1 = DomainModelElementTestBase.deserialize(bytes, PropertiesElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());
        assertEquals(testee.size(), testee1.size());
        for (String name : testee.getPropertyNames()) {
            assertEquals("Property values match for " + name, testee.getProperty(name), testee1.getProperty(name));
        }
    }

    abstract static class ContentAndPropertiesGetter extends Assert {
        final XMLMapper mapper;
        final String targetNamespace;
        final String targetNamespaceLocation;

        public ContentAndPropertiesGetter(XMLMapper mapper, String targetNamespace, String targetNamespaceLocation) {
            this.mapper = mapper;
            this.targetNamespace = targetNamespace;
            this.targetNamespaceLocation = targetNamespaceLocation;
        }

        XMLMapper getMapper() {
            return mapper;
        }
        abstract String getFullContent(String testContent);
        abstract PropertiesElement getTestProperties(String fullcontent) throws XMLStreamException, FactoryConfigurationError, UpdateFailedException;
    }
}
