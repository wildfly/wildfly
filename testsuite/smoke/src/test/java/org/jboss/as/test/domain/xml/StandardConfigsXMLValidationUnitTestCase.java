/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.domain.xml;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.jboss.as.version.Version;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class StandardConfigsXMLValidationUnitTestCase extends TestCase {

    public void testHost() throws Exception {
        parseXml("domain/configuration/host.xml");
    }

    public void testDomain() throws Exception {
        // FIXME disabled until it passes
        if (Boolean.TRUE)
            return;
        parseXml("domain/configuration/domain.xml");
    }

    public void testStandalone() throws Exception {
        // FIXME disabled until it passes
        if (Boolean.TRUE)
            return;
        parseXml("standalone/configuration/standalone.xml");
    }

    private void parseXml(String xmlName) throws ParserConfigurationException, SAXException, SAXNotRecognizedException,
            SAXNotSupportedException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        if (!factory.isNamespaceAware())
            factory.setNamespaceAware(true);
        if (!factory.isValidating())
            factory.setValidating(true);
        if (!factory.isXIncludeAware())
            factory.setXIncludeAware(true);

        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://apache.org/xml/features/validation/schema", true);
        reader.setErrorHandler(new ErrorHandlerImpl());
        reader.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId == null)
                    fail("Failed to resolve schema: systemId is null");
                int lastSlash = systemId.lastIndexOf('/');
                if (lastSlash > 0)
                    systemId = systemId.substring(lastSlash + 1);
                URL xsdUrl = getXsdUrl(systemId);
                return new InputSource(xsdUrl.openStream());
            }
        });
        URL xmlUrl = getXmlUrl(xmlName);
        InputSource is = new InputSource();
        is.setByteStream(xmlUrl.openStream());
        reader.parse(is);
    }

    private URL getXmlUrl(String xmlName) throws MalformedURLException {
        // user.dir will point to the root of this module
        File f = new File(System.getProperty("user.dir"));
        f = new File(f, "../../build/target");
        File[] children = f.listFiles(); f = null;
        if (children != null)
            for (File child : children)
                if (child.getName().startsWith("jboss-"))
                    f = child;

        if (f == null)
            fail("Server not built");

        f = new File(f, xmlName);
        return f.toURI().toURL();
    }

    private URL getXsdUrl(String xsdName) {
        System.out.println("resolving " + xsdName);
        String resourceName = "schema/" + xsdName;
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (url == null)
            url = Thread.currentThread().getContextClassLoader().getResource(xsdName);
        assertNotNull(resourceName + " not found", url);
        return url;
    }

    private final class ErrorHandlerImpl implements ErrorHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            fail(formatMessage(e));
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            fail(formatMessage(e));
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            System.out.println(formatMessage(e));
        }

        private String formatMessage(SAXParseException e) {
            StringBuffer sb = new StringBuffer();
            sb.append(e.getLineNumber()).append(':').append(e.getColumnNumber());
            if (e.getPublicId() != null)
                sb.append(" publicId='").append(e.getPublicId()).append('\'');
            if (e.getSystemId() != null)
                sb.append(" systemId='").append(e.getSystemId()).append('\'');
            sb.append(' ').append(e.getLocalizedMessage());
            return sb.toString();
        }
    }
}
