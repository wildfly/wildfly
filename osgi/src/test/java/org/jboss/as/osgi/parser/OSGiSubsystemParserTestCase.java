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

package org.jboss.as.osgi.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.ParseResult;
import org.jboss.as.osgi.AbstractOSGiSubsystemTest;
import org.jboss.as.osgi.OSGiSubsystemSupport;
import org.jboss.as.osgi.parser.OSGiSubsystemState.OSGiModule;
import org.jboss.as.osgi.service.Configuration;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;

/**
 * Test OSGi subsystem element.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class OSGiSubsystemParserTestCase extends AbstractOSGiSubsystemTest {

    OSGiSubsystemSupport subsystemSupport;

    @Override
    protected OSGiSubsystemSupport getSubsystemSupport() {
        return subsystemSupport;
    }

    @Test
    public void testEmptyRootElement() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0'/>";
        Configuration config = getSubsystemConfiguration(content);
        assertNotNull("Properties not null", config.getProperties());
    }

    @Test
    public void testInvalidAttribute() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' foo='bar'/>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testInvalidContent() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0'>invalid</subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testInvalidElement() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0'><invalid/></subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testEmptyProperties() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0'><properties/></subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testValidProperty() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><properties><property name='foo'>bar</property></properties></subsystem>";
        Configuration config = getSubsystemConfiguration(content);
        Map<String, Object> properties = config.getProperties();
        assertNotNull("Properties not null", properties);
        assertEquals("bar", properties.get("foo"));
    }

    @Test
    public void testValidProperties() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><properties><property name='one'>two</property><property name='three'>four</property></properties></subsystem>";
        Configuration config = getSubsystemConfiguration(content);
        Map<String, Object> properties = config.getProperties();
        assertNotNull("Properties not null", properties);
        assertEquals("two", properties.get("one"));
        assertEquals("four", properties.get("three"));
    }

    @Test
    public void testMissingModule() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><modules/></subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testMissingModuleIdentifier() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><modules><module/></modules></subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testDuplicateModule() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><modules><module identifier='org.jboss.osgi'/><module identifier='org.jboss.osgi'/></modules></subsystem>";
        try {
            parseSubsystemConfig(content);
            fail("XMLStreamException expected");
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    @Test
    public void testValidModules() throws Exception {
        String content = "<subsystem xmlns='urn:jboss:domain:osgi:1.0' ><modules><module identifier='foo'/><module identifier='bar' start='true'/></modules></subsystem>";
        Configuration config = getSubsystemConfiguration(content);
        List<OSGiModule> modules = config.getModules();
        assertNotNull("Modules not null", modules);
        assertEquals("Two modules", 2, modules.size());
        assertEquals(ModuleIdentifier.fromString("foo"), modules.get(0).getIdentifier());
        assertFalse("Module start false", modules.get(0).isStart());
        assertEquals(ModuleIdentifier.fromString("bar"), modules.get(1).getIdentifier());
        assertTrue("Module start true", modules.get(1).isStart());
    }

    private Configuration getSubsystemConfiguration(String content) throws Exception {
        OSGiSubsystemAdd add = parseSubsystemConfig(content);
        subsystemSupport = new OSGiSubsystemSupport(add.getSubsystemState())
        {
            @Override
            public void setupServices(BatchBuilder batchBuilder) throws Exception {
                // For this test we only need the environment services
                super.setupEnvironmentServices(batchBuilder);
            }
        };
        return subsystemSupport.getSubsystemConfig();
    }

    private OSGiSubsystemAdd parseSubsystemConfig(String content) throws XMLStreamException {
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(getTargetNamespace(), "subsystem"), new OSGiSubsystemElementParser());
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(content));
        ParseResult<SubsystemConfiguration<OSGiSubsystemElement>> parseResult = new ParseResult<SubsystemConfiguration<OSGiSubsystemElement>>();
        mapper.parseDocument(parseResult, xmlReader);
        SubsystemConfiguration<OSGiSubsystemElement> result = parseResult.getResult();
        OSGiSubsystemAdd add = (OSGiSubsystemAdd) result.getSubsystemAdd();
        return add;
    }

    private String getTargetNamespace() {
        return Namespace.CURRENT.getUriString();
    }
}
