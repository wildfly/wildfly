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
 *
 */

package org.jboss.as.test.integration.xalan;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * A servlet which uses {@link Transformer transform} APIs to transform an xml using a xsl
 */
@WebServlet(urlPatterns = XalanUsageServlet.URL_MAPPING_PATTERN)
public class XalanUsageServlet extends HttpServlet {

    static final String URL_MAPPING_PATTERN = "/xalan-access-in-servlet";
    static final String SUCCESS_MESSAGE = "all went fine";
    static final String XML_RESOURCE_TO_TRANSFORM = "xalan-xml-to-transform.xml";
    static final String XSL_RESOURCE = "xalan-transform.xsl";
    private static final String EXPECTED_TRANSFORM_CONTENT = "the content was replaced by a xsl transform";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final InputStream xmlStream = this.getClass().getResourceAsStream("/" + XML_RESOURCE_TO_TRANSFORM);
        if (xmlStream == null) {
            throw new ServletException("Could not locate " + XML_RESOURCE_TO_TRANSFORM + " as a resource, from the servlet");
        }
        InputStream xslStream = null;
        try {
            xslStream = this.getClass().getResourceAsStream("/" + XSL_RESOURCE);
            if (xslStream == null) {
                throw new ServletException("Could not locate " + XSL_RESOURCE + " as a resource, from the servlet");
            }
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Source xml = new StreamSource(xmlStream);
            final Source xsl = new StreamSource(xslStream);
            final Transformer transformer = transformerFactory.newTransformer(xsl);
            final StringWriter sw = new StringWriter();
            // transform the xml using the xsl
            transformer.transform(xml, new StreamResult(sw));
            final String transformedOutput = sw.toString();
            if (transformedOutput != null && transformedOutput.contains(EXPECTED_TRANSFORM_CONTENT)) {
                // xml was properly transformed, consider it a success
                resp.getWriter().print(SUCCESS_MESSAGE);
                return;
            }
            resp.getWriter().print("XSL transform did not work");
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            xmlStream.close();
            if (xslStream != null) {
                xslStream.close();
            }
        }
    }
}
