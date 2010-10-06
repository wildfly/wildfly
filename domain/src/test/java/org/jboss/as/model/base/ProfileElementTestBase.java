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

import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.ProfileElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.MockSubsystemElement;
import org.jboss.as.model.base.util.ModelParsingSupport;

/**
 * Base class for unit tests of {@link ProfileElement}.
 *
 * @author Brian Stansberry
 */
public abstract class ProfileElementTestBase extends DomainModelElementTestBase {


    /**
     * @param name
     */
    public ProfileElementTestBase(String name) {
        super(name);
    }

    public void testSimpleParse() throws Exception {
        String fullcontent = getFullContent("<profile name=\"test\">" + MockSubsystemElement.getFullXmlContent() + "</profile>");
        ProfileElement testee = getTestProfile(fullcontent);

        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee.getSubsystems();
        assertEquals(2, subsystems.size());
        boolean gotMock = false;
        boolean gotAnotherMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockSubsystemElement.MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotMock = true;
            }
            else if (MockSubsystemElement.ANOTHER_MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
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
        String testContent = "<profile name=\"foo\">" + MockSubsystemElement.getAnotherSubsystemXmlContent() + "</profile>";
        testContent += "<profile name=\"test\"><include profile=\"foo\"/>" + MockSubsystemElement.getSingleSubsystemXmlContent() + "</profile>";
        String fullcontent = getFullContent(testContent);
        ProfileElement testee = getTestProfile(fullcontent);
        Set<String> includes = testee.getIncludedProfiles();
        assertEquals(1, includes.size());
        String include = includes.iterator().next();
        assertEquals("foo", include);
        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee.getSubsystems();
        assertEquals(1, subsystems.size());
        boolean gotMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockSubsystemElement.MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotMock = true;
            }
            else {
                fail("Unknown subsystem QName " + subsystem.getElementName());
            }
        }
        assertTrue(gotMock);
    }

    public void testNoNameParse() throws Exception {
        String fullcontent = getFullContent("<profile>" + MockSubsystemElement.getSingleSubsystemXmlContent() + "</profile>");
        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadAttributeParse() throws Exception {
        String fullcontent = getFullContent("<profile bogus=\"bogus\">" + MockSubsystemElement.getSingleSubsystemXmlContent() + "</profile>");
        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }

        fullcontent = getFullContent("<profile name=\"test\" bogus=\"bogus\">" + MockSubsystemElement.getSingleSubsystemXmlContent() + "</profile>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testNoSubsystemParse() throws Exception {
        String fullcontent = getFullContent("<profile name=\"test\"/>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing children did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }

        fullcontent = getFullContent("<profile name=\"test\"><include profile=\"foo\"/></profile>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing children did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadChildElement() throws Exception {
        String fullcontent = getFullContent("<profile name=\"test\"><bogus/></profile>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }

        fullcontent = getFullContent("<profile name=\"test\">" + MockSubsystemElement.getSingleSubsystemXmlContent() + "<bogus/></profile>");

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    @Override
    public void testSerializationDeserialization() throws Exception {

        String testContent = "<profile name=\"foo\">" + MockSubsystemElement.getSingleSubsystemXmlContent() + "</profile>";
        testContent += "<profile name=\"test\"><include profile=\"foo\"/>" + MockSubsystemElement.getAnotherSubsystemXmlContent() + "</profile>";
        String fullcontent = getFullContent(testContent);
        ProfileElement testee = getTestProfile(fullcontent);

        byte[] bytes = serialize(testee);
        ProfileElement testee1 = deserialize(bytes, ProfileElement.class);

        assertEquals(testee.getName(), testee1.getName());
        Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = testee1.getSubsystems();
        assertEquals(1, subsystems.size());
        boolean gotAnotherMock = false;
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems) {
            if (MockSubsystemElement.ANOTHER_MOCK_ELEMENT_QNAME.equals(subsystem.getElementName())) {
                gotAnotherMock = true;
            }
            else {
                fail("Unknown subsystem QName " + subsystem.getElementName());
            }
        }
        assertTrue(gotAnotherMock);
    }

    private String getFullContent(String testContent) {
        testContent = ModelParsingSupport.wrap(Element.PROFILES.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);
        return fullcontent;
    }

    private ProfileElement getTestProfile(String fullcontent) throws XMLStreamException, FactoryConfigurationError,
            UpdateFailedException {
        DomainModel root = ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
        ProfileElement testee = root.getProfile("test");
        assertNotNull(testee);
        assertEquals("test", testee.getName());
        return testee;
    }

}
