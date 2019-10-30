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

package org.jboss.as.test.integration.xerces;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
