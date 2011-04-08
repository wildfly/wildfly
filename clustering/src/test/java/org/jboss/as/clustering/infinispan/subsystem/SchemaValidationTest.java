/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Paul Ferraro
 */
public class SchemaValidationTest {

    @Test
    public void validate() throws Exception {
        validateXsd("jboss-infinispan.xsd");
    }

    private void validateXsd(String xsdName) throws SAXException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("schema/" + xsdName);
        Assert.assertNotNull(url);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.newSchema(url);
    }

    private final class ErrorHandlerImpl implements ErrorHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            Assert.fail(formatMessage(e));
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            Assert.fail(formatMessage(e));
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            System.out.println(formatMessage(e));
        }

        private String formatMessage(SAXParseException e) {
            StringBuilder builder = new StringBuilder();
            builder.append(e.getLineNumber()).append(':').append(e.getColumnNumber());
            if(e.getPublicId() != null) {
                builder.append(" publicId='").append(e.getPublicId()).append('\'');
            }
            if(e.getSystemId() != null) {
                builder.append(" systemId='").append(e.getSystemId()).append('\'');
            }
            builder.append(' ').append(e.getLocalizedMessage());
            return builder.toString();
        }
    }
}
