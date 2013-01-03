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
package org.jboss.as.jdr.util;

import org.jboss.vfs.VirtualFileFilter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class XMLSanitizer extends AbstractSanitizer {

    private XPathExpression expression;
    private DocumentBuilder builder;
    private Transformer transformer;

    public XMLSanitizer(String pattern, VirtualFileFilter filter) throws Exception {
        this.filter = filter;
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        expression = xpath.compile(pattern);

        DocumentBuilderFactory DBfactory = DocumentBuilderFactory.newInstance();
        DBfactory.setNamespaceAware(true);
        builder = DBfactory.newDocumentBuilder();
        builder.setErrorHandler(null);

        TransformerFactory transformerFactory = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", null);
        transformer = transformerFactory.newTransformer();
    }

    public InputStream sanitize(InputStream in) throws Exception {
        try {
            Document doc = builder.parse(in);
            Object result = expression.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                nodes.item(i).setTextContent("");
            }
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StreamResult outStream = new StreamResult(output);
            transformer.transform(source, outStream);
            return new ByteArrayInputStream(output.toByteArray());
        } catch (Exception e) {
            return in;
        }
    }
}
