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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
@Ignore
public class XMLValidationTestCase extends AbstractValidationTest {

    @Test
    public void testDomain() throws Exception {
        parseXml("jboss-osgi-example.xml");
    }

    private void parseXml(String xmlName) throws Exception {

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

}
