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
package org.jboss.as.test.flat.xml;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * A XSDValidationUnitTestCase.
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public class XSDValidationUnitTestCase {

    @Test
    public void testJBoss70() throws Exception {
        validateXsd("jboss_7_0.xsd");
    }

    // @Test
    public void testJBossConnector() throws Exception {
        validateXsd("jboss-jca.xsd");
    }

    // FIXME disabled until it passes
    @Test
    @Ignore
    public void testJBossDatasources() throws Exception {
        validateXsd("jboss-datasources.xsd");
    }

    // FIXME disabled until it passes
    @Test
    @Ignore
    public void testJBossResourceAdapters() throws Exception {
        validateXsd("jboss-resource-adapters.xsd");
    }

    @Test
    public void testJBossJmx() throws Exception {
        validateXsd("jboss-jmx.xsd");
    }

    @Test
    public void testJBossLogging() throws Exception {
        validateXsd("jboss-logging.xsd");
    }

    @Test
    public void testJBossMessaging() throws Exception {
        validateXsd("jboss-messaging.xsd");
    }

    @Test
    public void testJBossJMS() throws Exception {
        validateXsd("jboss-jms.xsd");
    }

    @Test
    public void testJBossNaming() throws Exception {
        validateXsd("jboss-naming.xsd");
    }

    @Test
    public void testJBossRemoting() throws Exception {
        validateXsd("jboss-remoting.xsd");
    }

    @Test
    public void testJBossSar() throws Exception {
        validateXsd("jboss-sar.xsd");
    }

    @Test
    public void testJBossThreads() throws Exception {
        validateXsd("jboss-threads.xsd");
    }

    private void validateXsd(String xsdName) throws SAXException {
        URL jbossDomain = getXsdUrl(xsdName);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(new ErrorHandlerImpl());
        schemaFactory.newSchema(jbossDomain);
    }

    private URL getXsdUrl(String xsdName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("schema/" + xsdName);
        assertNotNull(url);
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
