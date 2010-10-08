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

import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.model.JvmElement;
import org.jboss.as.model.JvmOptionsElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.ModelParsingSupport;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link JvmOptionsElement} as the set of
 * jvm options in a server group {@link JvmElement}.
 *
 * @author Kabir Khan
 */
public class JvmOptionsTestCommon extends Assert {

    ContentAndPropertiesGetter getter;

    public JvmOptionsTestCommon(ContentAndPropertiesGetter getter) {
        this.getter = getter;
    }

    public void testBasicProperties() throws Exception {
        String fullcontent = getter.getFullContent("<jvm-options><option value=\"-Xone\"/><option value=\"-Xtwo\"/></jvm-options>");
        JvmOptionsElement testee = getter.getTestOptions(fullcontent);
        List<String> options = testee.getOptions();
        assertEquals(2, options.size());
        assertEquals("-Xone", options.get(0));
        assertEquals("-Xtwo", options.get(1));
    }

    public void testMissingValue() throws Exception {
        String fullcontent = getter.getFullContent("<jvm-options><option value=\"-Xone\"/><option/></jvm-options>");
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Missing value attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testInvalidAttribute() throws Exception {
        String fullcontent = getter.getFullContent("<jvm-options><option value=\"-Xone\"/><option value=\"-Xtwo\" wrong\"zzz\"/></jvm-options>");
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Invalid 'wrong' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testNoChildren() throws Exception {
        String fullcontent = getter.getFullContent("<jvm-options/>");

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
        String fullcontent = getter.getFullContent("<jvm-options><option value=\"-Xone\"/><option value=\"-Xtwo\"/></jvm-options>");
        JvmOptionsElement testee = getter.getTestOptions(fullcontent);

        byte[] bytes = DomainModelElementTestBase.serialize(testee);
        JvmOptionsElement testee1 = DomainModelElementTestBase.deserialize(bytes, JvmOptionsElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());
        List<String> options = testee1.getOptions();
        assertEquals(2, options.size());
        assertEquals("-Xone", options.get(0));
        assertEquals("-Xtwo", options.get(1));

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
        abstract JvmOptionsElement getTestOptions(String fullcontent) throws XMLStreamException, FactoryConfigurationError, UpdateFailedException;
    }
}
