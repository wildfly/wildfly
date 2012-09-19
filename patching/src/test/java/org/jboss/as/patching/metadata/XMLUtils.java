/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.metadata;

import static org.junit.Assert.assertEquals;

import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/**
 * FIXME: copied from ModelTestUtils. This class can not be used since adding a dependency to jboss-as-subsystem-test creates
 * a circular dependencies in Maven modules
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat inc
 *
 */
public class XMLUtils {

    /**
     * Normalize and pretty-print XML so that it can be compared using string
     * compare. The following code does the following: - Removes comments -
     * Makes sure attributes are ordered consistently - Trims every element -
     * Pretty print the document
     *
     * @param xml The XML to be normalized
     * @return The equivalent XML, but now normalized
     */
    public static String normalizeXML(String xml) throws Exception {
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
        lsSerializer.getDomConfig().setParameter("comments", Boolean.FALSE);
        lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        return lsSerializer.writeToString(document);
    }

    public static void compareXml(final String original, final String marshalled, final boolean ignoreNamespace) throws Exception {
        final String xmlOriginal;
        final String xmlMarshalled;
        if (ignoreNamespace) {
            xmlOriginal = XMLUtils.removeNamespace(original);
            xmlMarshalled = XMLUtils.removeNamespace(marshalled);
        } else {
            xmlOriginal = original;
            xmlMarshalled = marshalled;
        }

        assertEquals(normalizeXML(xmlOriginal), normalizeXML(xmlMarshalled));
    }

    static String removeNamespace(String xml) {
        int start = xml.indexOf(" xmlns=\"");
        int end = xml.indexOf('"', start + "xmlns=\"".length() + 1);
        if (start != -1) {
            StringBuilder sb = new StringBuilder(xml.substring(0, start));
            sb.append(xml.substring(end + 1));
            return sb.toString();
        }
        return xml;
    }

}
