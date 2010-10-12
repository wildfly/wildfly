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
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.model.JvmElement;
import org.jboss.as.model.JvmOptionsElement;
import org.jboss.as.model.JvmType;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.ModelParsingSupport;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link JvmElement} as a child of
 * a {@link ServerGroupElement}.
 *
 * @author Brian Stansberry
 */
public class JvmElementTestCommon extends Assert {
    ContentAndElementGetter getter;
    public JvmElementTestCommon(ContentAndElementGetter getter) {
        this.getter = getter;
    }

    public void testSimpleParse() throws Exception {
        String fullcontent = getter.getFullContent("<jvm name=\"test\"/>");
        JvmElement testee = getter.getTestJvmElement(fullcontent);
        assertEquals("test", testee.getName());
    }

    public void testBothAgentPathAndAgentLibFails() throws Exception {
        String testContent = "<jvm>";
        testContent += "<agent-lib value=\"/usr/test\"/>";
        testContent += "<agent-path value=\"/usr/test\"/>";
        testContent += "</jvm>";

        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse both agent-lib and agent-path");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }

    }

    public void testFullParse() throws Exception {
        String testContent = "<jvm name=\"test\" type=\"SUN\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<permgen size=\"64M\" max-size=\"96M\"/>";
        testContent += "<heap size=\"128M\" max-size=\"256M\"/>";
        testContent += "<stack size=\"1M\"/>";
        testContent += "<agent-lib value=\"/usr/agent\"/>";
        testContent += "<java-agent value=\"/usr/javaagent.jar\"/>";
        testContent += "<jvm-options><option value=\"-Xone\"/><option value=\"-Xtwo\"/></jvm-options>";
        testContent += "<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\" value=\"value2\"/></system-properties>";
        testContent += "<environment-variables><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop2\" value=\"value2\"/></environment-variables>";
        testContent += "</jvm>";

        System.out.println(testContent);

        String fullcontent = getter.getFullContent(testContent);
        JvmElement testee = getter.getTestJvmElement(fullcontent);
        assertEquals("test", testee.getName());
        assertEquals(JvmType.SUN, testee.getJvmType());
        assertEquals("/home/test", testee.getJavaHome());
        assertTrue(testee.isDebugEnabled());
        assertEquals("Debug Me", testee.getDebugOptions());
        assertFalse(testee.isEnvClasspathIgnored());
        assertEquals("64M", testee.getPermgenSize());
        assertEquals("96M", testee.getMaxPermgen());
        assertEquals("128M", testee.getHeapSize());
        assertEquals("256M", testee.getMaxHeap());
        assertEquals("1M",testee.getStack());
        assertEquals("/usr/agent", testee.getAgentLib());
        assertEquals("/usr/javaagent.jar", testee.getJavaagent());

        validateOptions(testee.getJvmOptions());
        validateProperties(testee.getEnvironmentVariables());
        validateProperties(testee.getSystemProperties());
    }

    public void testMissingName() throws Exception {
        String fullcontent = getter.getFullContent("<jvm/>");
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Bogus child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    private void validateProperties(PropertiesElement testee) throws Exception {
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

    private void validateOptions(JvmOptionsElement testee) throws Exception {
        List<String> options = testee.getOptions();
        assertEquals(2, options.size());
        assertEquals("-Xone", options.get(0));
        assertEquals("-Xtwo", options.get(1));
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#testSerializationDeserialization()
     */
    public void testSerializationDeserialization() throws Exception {
        String testContent = "<jvm name=\"test\" type=\"IBM\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<permgen size=\"64M\" max-size=\"96M\"/>";
        testContent += "<heap size=\"128M\" max-size=\"256M\"/>";
        testContent += "<stack size=\"1M\"/>";
        testContent += "<agent-path value=\"/usr/agent\"/>";
        testContent += "<java-agent value=\"/usr/javaagent.jar\"/>";
        testContent += "<jvm-options><option value=\"-Xone\"/><option value=\"-Xtwo\"/></jvm-options>";
        testContent += "<system-properties><property name=\"prop1\" value=\"value1\"/><property name=\"prop2\" value=\"value2\"/></system-properties>";
        testContent += "<environment-variables><variable name=\"prop1\" value=\"value1\"/><variable name=\"prop2\" value=\"value2\"/></environment-variables>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement testee = getter.getTestJvmElement(fullcontent);

        byte[] bytes = DomainModelElementTestBase.serialize(testee);
        JvmElement testee1 = DomainModelElementTestBase.deserialize(bytes, JvmElement.class);

        assertEquals("test", testee1.getName());
        assertEquals(JvmType.IBM, testee.getJvmType());
        assertEquals("/home/test", testee1.getJavaHome());
        assertTrue(testee.isDebugEnabled());
        assertEquals("Debug Me", testee.getDebugOptions());
        assertFalse(testee.isEnvClasspathIgnored());
        assertEquals("64M", testee.getPermgenSize());
        assertEquals("96M", testee.getMaxPermgen());
        assertEquals("128M", testee1.getHeapSize());
        assertEquals("256M", testee1.getMaxHeap());
        assertEquals("1M",testee.getStack());
        assertEquals("/usr/agent", testee.getAgentPath());
        assertEquals("/usr/javaagent.jar", testee.getJavaagent());

        validateOptions(testee.getJvmOptions());
        validateProperties(testee1.getEnvironmentVariables());
        validateProperties(testee1.getSystemProperties());
    }

    public void testJvmInvalidAtribute() throws Exception {
        String testContent = "<jvm name=\"test\" wrong=\"/xxx\" />";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testJvmInvalidType() throws Exception {
        String testContent = "<jvm name=\"test\" type=\"BAD\" />";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testJvmDefaultType() throws Exception {
        String testContent = "<jvm name=\"test\"/>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement testee = getter.getTestJvmElement(fullcontent);

        byte[] bytes = DomainModelElementTestBase.serialize(testee);
        JvmElement testee1 = DomainModelElementTestBase.deserialize(bytes, JvmElement.class);

        assertEquals("test", testee1.getName());
        assertEquals(JvmType.SUN, testee.getJvmType());
    }

    public void testPermgenMissingSize() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<permgen max-size=\"96M\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement jvm = getter.getTestJvmElement(fullcontent);
        assertEquals("test", jvm.getName());
        assertNull(jvm.getPermgenSize());
        assertEquals("96M", jvm.getMaxPermgen());
    }


    public void testPermgenMissingMax() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<permgen size=\"64M\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement jvm = getter.getTestJvmElement(fullcontent);
        assertEquals("test", jvm.getName());
        assertEquals("64M", jvm.getPermgenSize());
        assertNull(jvm.getMaxPermgen());
    }


    public void testPermgenInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<permgen size=\"64M\" max-size=\"96M\" wrong=\"zzzzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testHeapMissingSize() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<heap max-size=\"256M\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement jvm = getter.getTestJvmElement(fullcontent);
        assertEquals("test", jvm.getName());
        assertNull(jvm.getHeapSize());
        assertEquals("256M", jvm.getMaxHeap());
        assertNull(jvm.getMaxPermgen());
    }

    public void testHeapMissingMax() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<heap size=\"128M\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        JvmElement jvm = getter.getTestJvmElement(fullcontent);
        assertEquals("test", jvm.getName());
        assertEquals("128M", jvm.getHeapSize());
        assertNull(jvm.getMaxHeap());
    }

    public void testHeapInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<heap size=\"128M\" max-size=\"256M\" wrong=\"zzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
         // TODO validate the location stuff in the exception message
        }
    }

    public void testStackMissingSize() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<stack/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should have detected missing size");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testStackInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<stack size=\"1M\" wrong=\"zzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testAgentPathMissingValue() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<agent-path/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should have detected missing value");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testAgentPathInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<agent-path value=\"/usr/agent\" wrong=\"zzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testAgentLibMissingValue() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<agent-lib/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should have detected missing value");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testAgentLibInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<agent-lib value=\"/usr/agent\" wrong=\"zzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testJavaagentMissingValue() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<java-agent/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should have detected missing value");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    public void testJavaagentInvalidAttribute() throws Exception {
        String testContent = "<jvm name=\"test\" java-home=\"/home/test\" debug-enabled=\"true\" debug-options=\"Debug Me\" env-classpath-ignored=\"false\">";
        testContent += "<java-agent value=\"/usr/javaagent.jar\" wrong=\"zzz\"/>";
        testContent += "</jvm>";
        String fullcontent = getter.getFullContent(testContent);
        try {
            ModelParsingSupport.parseDomainModel(getter.getMapper(), fullcontent);
            fail("Should not be able to parse 'wrong' attribute");
        }
        catch (XMLStreamException good) {
            System.out.println("Caught expected exception " + good.getMessage());
         // TODO validate the location stuff in the exception message
        }
    }

    static abstract class ContentAndElementGetter extends Assert {
        final XMLMapper mapper;
        final String targetNamespace;
        final String targetNamespaceLocation;

        public ContentAndElementGetter(XMLMapper mapper, String targetNamespace, String targetNamespaceLocation) {
            this.mapper = mapper;
            this.targetNamespace = targetNamespace;
            this.targetNamespaceLocation = targetNamespaceLocation;
        }

        XMLMapper getMapper() {
            return mapper;
        }
        abstract String getFullContent(String testContent);
        abstract JvmElement getTestJvmElement(String fullcontent) throws XMLStreamException, FactoryConfigurationError, UpdateFailedException;
    }

}
