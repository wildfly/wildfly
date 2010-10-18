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

package org.jboss.as.metadata.parser.servlet;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.spec.WebMetaData;
import org.junit.Test;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 */
public class WebApp6EverythingUnitTestCase {

    @Test
    public void testEverything() throws Exception {
        for (int i = 0; i < 2; i++) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("WebApp6Everything_testEverything.xml");
            WebMetaData wmd = parseWithSTAX(is);
        }
    }

    public WebMetaData parseWithSTAX(InputStream is) throws XMLStreamException {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        MetaDataElementParser.DTDInfo dtd = new MetaDataElementParser.DTDInfo();
        inputFactory.setXMLResolver(new MetaDataElementParser.DTDInfo());
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
        long time2 = 0;
        long time = System.nanoTime();
        try {
            return org.jboss.as.metadata.parser.servlet.WebMetaDataParser.parse(xmlReader);
        } finally {
            time2 = System.nanoTime();
            xmlReader.close();
            System.out.println("STAX took: " + (time2 - time) / 1000000 + "ms");
        }
    }

}
