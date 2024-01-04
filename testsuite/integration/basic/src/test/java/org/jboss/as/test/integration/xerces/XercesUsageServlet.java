/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.xerces;

import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * User: jpai
 */
@WebServlet (urlPatterns = XercesUsageServlet.URL_PATTERN)
public class XercesUsageServlet extends HttpServlet {

    public static final String URL_PATTERN = "/xercesServlet";

    public static final String XML_RESOURCE_NAME_PARAMETER = "xml";

    public static final String SUCCESS_MESSAGE = "Success";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String xmlResource = req.getParameter(XML_RESOURCE_NAME_PARAMETER);
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(xmlResource);
        if (inputStream == null) {
            throw new ServletException(xmlResource + " could not be found");
        }
        try {
            this.parse(inputStream);
            resp.getOutputStream().print(SUCCESS_MESSAGE);
        } catch (SAXException saxe) {
            throw new ServletException(saxe);
        }
    }

    private void parse(final InputStream inputStream) throws IOException, SAXException {
        DOMParser domParser = new DOMParser();
        domParser.parse(new InputSource(inputStream));
    }


}
