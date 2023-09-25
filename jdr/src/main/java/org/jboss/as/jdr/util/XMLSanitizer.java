/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.util;

import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

import org.jboss.vfs.VirtualFileFilter;
import org.wildfly.common.xml.DocumentBuilderFactoryUtil;
import org.wildfly.common.xml.TransformerFactoryUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * {@link Sanitizer} subclass that removes the contents of the matched xpath expression
 * in {@code pattern}.
 */
public class XMLSanitizer extends AbstractSanitizer {

    private XPathExpression expression;
    private DocumentBuilder builder;
    private Transformer transformer;

    public XMLSanitizer(String pattern, VirtualFileFilter filter) throws Exception {
        this.filter = filter;
        XPathFactory factory = XPathFactory.newInstance();
        factory.setFeature(FEATURE_SECURE_PROCESSING, true);
        XPath xpath = factory.newXPath();
        expression = xpath.compile(pattern);

        DocumentBuilderFactory DBfactory = DocumentBuilderFactoryUtil.create();
        DBfactory.setNamespaceAware(true);
        builder = DBfactory.newDocumentBuilder();
        builder.setErrorHandler(null);

        TransformerFactory transformerFactory = TransformerFactoryUtil.create();
        transformer = transformerFactory.newTransformer();

    }

    public InputStream sanitize(InputStream in) throws Exception {
        byte [] content = Utils.toByteArray(in);
        try {
            // storing the entire file in memory in case we need to bail.
            Document doc = builder.parse(new ByteArrayInputStream(content));
            doc.setXmlStandalone(true);
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
            ROOT_LOGGER.debug("Error while sanitizing an xml document", e);
            return new ByteArrayInputStream(content);
        }
    }
}
