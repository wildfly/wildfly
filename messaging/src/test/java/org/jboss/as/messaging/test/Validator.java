/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.SystemPropertyResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XML Schema Validator.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class Validator {

    private static ErrorHandler ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            fail("warning: " + exception.getMessage());

        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            fail("error: " + exception.getMessage());

        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            fail("fatal error: " + exception.getMessage());
        }
    };

    static void validateXML(String xmlContent, String xsdPath) throws Exception {
        String resolvedXml = resolve(xmlContent);

        URL xsdURL = Thread.currentThread().getContextClassLoader().getResource(xsdPath);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(ERROR_HANDLER);
        Schema schema = schemaFactory.newSchema(xsdURL);
        javax.xml.validation.Validator validator = schema.newValidator();
        validator.setErrorHandler(ERROR_HANDLER);
        validator.setFeature("http://apache.org/xml/features/validation/schema", true);
        validator.validate(new StreamSource(new StringReader(resolvedXml)));
    }

    /**
     * Subystem XML can contain expressions for simple XSD types (boolean, long, etc.) that
     * prevents to validate it against the schema.
     *
     * For XML validation, the XML is read and any expression is resolved (they must have a default value to
     * be properly resolved).
     */
    private static String resolve(String xmlContent) throws IOException {
        PropertyReplacer replacer = PropertyReplacers.resolvingReplacer(SystemPropertyResolver.INSTANCE);
        StringBuilder out = new StringBuilder();

        try(BufferedReader reader = new BufferedReader(new StringReader(xmlContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(replacer.replaceProperties(line));
                out.append('\n');
            }
        }
        return out.toString();
    }
}
