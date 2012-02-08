/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.stream.XMLOutputFactory;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemParserTestCase {
    @Test
    public void testEmptySubsystem() throws Exception {
        testSubsystem("empty.xml", "org.jboss.as.empty");
    }

    @Test
    public void testEmptyWithAttributesSubsystem() throws Exception {
        testSubsystem("empty-with-attributes.xml", "org.jboss.as.empty");
    }

    @Test
    public void testSimpleSubsystem() throws Exception {
        testSubsystem("simple.xml", "org.jboss.as.simple");
    }

    @Test
    public void testSimpleComment() throws Exception {
        testSubsystem("simple-comment.xml", "org.jboss.as.simple");
    }

    @Ignore("CDATA not reported propery. Look into if we start using that in our config files")
    @Test
    public void testSimpleCData() throws Exception {
        testSubsystem("simple-cdata.xml", "org.jboss.as.simple");
    }

    @Test
    public void testLoggingSubsystem() throws Exception {
        testSubsystem("logging.xml", "org.jboss.as.logging");
    }

    @Test
    public void testTextAndCommentsSubsystem() throws Exception {
        testSubsystem("simple-with-text-and-comments.xml", "org.jboss.as.simple");
    }

    @Test
    public void testEjb3Subsystem() throws Exception {
        testSubsystem("ejb3.xml", "org.jboss.as.ejb3");
    }

    @Test
    public void testSupplementDefault() throws Exception {
        SubsystemParser parser = testSubsystem("simple-with-supplements.xml", "org.jboss.as.simple", null, false);
        String marshalled = marshall(parser);
        String expected =
                "<?xml version=\"1.0\" ?>" +
                "<subsystem xmlns=\"urn:jboss:domain:simple-with-text-and-comments:1.0\">" +
                "   <some-element value=\"true\"/>" +
                "</subsystem>";
        Assert.assertEquals(normalizeXML(expected), normalizeXML(marshalled));
    }

    @Test
    public void testSupplementFull() throws Exception {
        SubsystemParser parser = testSubsystem("simple-with-supplements.xml", "org.jboss.as.simple", "full", false);
        String marshalled = marshall(parser);
        String expected =
                "<?xml version=\"1.0\" ?>" +
                "<subsystem xmlns=\"urn:jboss:domain:simple-with-text-and-comments:1.0\">" +
                "   <some-element value=\"false\"/>" +
                "  <childA childA-attr=\"child-one\">Hello</childA>" +
                "  <childB ohildB-attr=\"child two\">" +
                "    <childB1/>" +
                "  </childB>" +
                "</subsystem>";

        Assert.assertEquals(normalizeXML(expected), normalizeXML(marshalled));
    }

    @Test
    public void testSupplementHa() throws Exception {
        SubsystemParser parser = testSubsystem("simple-with-supplements.xml", "org.jboss.as.simple", "ha", false);
        String marshalled = marshall(parser);
        String expected =
                "<?xml version=\"1.0\" ?>" +
                "<subsystem xmlns=\"urn:jboss:domain:simple-with-text-and-comments:1.0\">" +
                "   <some-element value=\"true\"/>" +
                "   <childC ohildC-attr=\"child two\">" +
                "      <childC1>Yo</childC1>" +
                "   </childC>" +
                "</subsystem>";

        Assert.assertEquals(normalizeXML(expected), normalizeXML(marshalled));
    }

    @Test
    public void testSupplementFullHa() throws Exception {
        SubsystemParser parser = testSubsystem("simple-with-supplements.xml", "org.jboss.as.simple", "full-ha", false);
        String marshalled = marshall(parser);
        String expected =
                "<?xml version=\"1.0\" ?>" +
                "<subsystem xmlns=\"urn:jboss:domain:simple-with-text-and-comments:1.0\">" +
                "   <some-element value=\"true\"/>" +
                "   <childA childA-attr=\"child-one\">Hello</childA>" +
                "   <childB ohildB-attr=\"child two\">" +
                "      <childB1/>" +
                "   </childB>" +
                "   <childC ohildC-attr=\"child two\">" +
                "      <childC1>Overridden by full-ha</childC1>" +
                "   </childC>" +
                "</subsystem>";

        Assert.assertEquals(normalizeXML(expected), normalizeXML(marshalled));
    }

    private SubsystemParser testSubsystem(String xml, String extensionModule) throws Exception {
        return testSubsystem(xml, extensionModule, null, true);
    }

    private SubsystemParser testSubsystem(String xml, String extensionModule, String supplement, boolean compareWithOriginal) throws Exception {
        URL url = this.getClass().getResource(xml);
        Assert.assertNotNull(url);
        SubsystemParser subsystemParser = new SubsystemParser(null, supplement, new File(url.toURI()));
        subsystemParser.parse();

        Assert.assertNotNull(subsystemParser.getExtensionModule());
        Assert.assertEquals(extensionModule, subsystemParser.getExtensionModule());

        if (compareWithOriginal) {
            String marshalled = marshall(subsystemParser);
            Assert.assertEquals(normalizeXML(trimOriginalXml(url)), normalizeXML(marshalled));
        }
        return subsystemParser;
    }

    private String marshall(SubsystemParser subsystemParser) throws Exception {
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(factory.createXMLStreamWriter(stringWriter));
        try {
            Assert.assertNotNull(subsystemParser.getSubsystem());
            writer.writeStartDocument();
            subsystemParser.getSubsystem().marshall(writer);
            writer.writeEndDocument();
        } finally {
            writer.close();
        }

        System.out.println(stringWriter.getBuffer().toString());
        return stringWriter.getBuffer().toString();
    }

    private String trimOriginalXml(URL url) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
        try {
            String s = reader.readLine();
            while (s != null) {
                if (!s.contains("config>") && ! s.contains("<extension-module")) {
                    sb.append(s);
                    sb.append("\n");
                }
                if (s.contains("</subsystem>")) {
                    break;
                }
                s = reader.readLine();
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Normalize and pretty-print XML so that it can be compared using string
     * compare. The following code does the following: - Removes comments -
     * Makes sure attributes are ordered consistently - Trims every element -
     * Pretty print the document
     *
     * @param xml
     *            The XML to be normalized
     * @return The equivalent XML, but now normalized
     */
    protected String normalizeXML(String xml) throws Exception {
        // Remove all white space adjoining tags ("trim all elements")
        xml = xml.replaceAll("\\s*<", "<");
        xml = xml.replaceAll(">\\s*", ">");

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
        LSParser lsParser = domLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        LSInput input = domLS.createLSInput();
        input.setStringData(xml);
        Document document = lsParser.parse(input);

        LSSerializer lsSerializer = domLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("comments", Boolean.TRUE);
        lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        return lsSerializer.writeToString(document);
    }

}
