/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.xml.parser;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.osgi.xml.XMLParserCapability;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A test that uses a SAX parser to read an XML document.
 *
 * @see http://www.osgi.org/javadoc/r4v41/org/osgi/util/xml/XMLParserActivator.html
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Jul-2009
 */
@RunWith(Arquillian.class)
public class SAXParserTestCase {
    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-xml-parser");
        archive.addAsResource("osgi/xml/example-xml-parser.xml", "example-xml-parser.xml");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSAXParser() throws Exception {
        bundle.start();

        SAXParser saxParser = getSAXParser();
        URL resURL = bundle.getResource("example-xml-parser.xml");

        SAXHandler saxHandler = new SAXHandler();
        saxParser.parse(resURL.openStream(), saxHandler);
        assertEquals("content", saxHandler.getContent());
    }

    private SAXParser getSAXParser() throws SAXException, ParserConfigurationException, InvalidSyntaxException {
        BundleContext context = bundle.getBundleContext();

        // This service gets registerd by the jboss-osgi-apache-xerces service
        String filter = "(" + XMLParserCapability.PARSER_PROVIDER + "=" + XMLParserCapability.PROVIDER_JBOSS_OSGI + ")";
        ServiceReference[] srefs = context.getServiceReferences(SAXParserFactory.class.getName(), filter);
        if (srefs == null)
            throw new IllegalStateException("SAXParserFactory not available");

        SAXParserFactory factory = (SAXParserFactory) context.getService(srefs[0]);
        factory.setValidating(false);

        SAXParser saxParser = factory.newSAXParser();
        return saxParser;
    }

    static class SAXHandler extends DefaultHandler {
        private String content;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            content = new String(ch, start, length);
        }

        public String getContent() {
            return content;
        }
    }
}