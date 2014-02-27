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

package org.jboss.as.testsuite.integration.secman.servlets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet, which prints file content. By default it prints value of property {@value #DEFAULT_FILE_NAME}, but you can specify
 * another property name by using request parameter {@value #PARAM_FILE_NAME}.
 *
 * @author Josef Cacek
 */
@WebServlet(ReadFileServlet.SERVLET_PATH)
public class ReadFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/ReadFileServlet";
    public static final String PARAM_FILE_NAME = "file";
    public static final String DEFAULT_FILE_NAME = "/etc/passwd";

    /**
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String property = req.getParameter(PARAM_FILE_NAME);
        if (property == null) {
            property = DEFAULT_FILE_NAME;
        }
        InputStream is = new FileInputStream(property);
        try {
            copy(is, resp.getOutputStream());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copies an input stream to an output stream.
     *
     * @param input
     * @param output
     * @return count of bytes copied
     * @throws IOException
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        long count = 0;
        int n = 0;
        final byte[] buffer = new byte[4096];
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
