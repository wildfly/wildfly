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
package org.jboss.as.osgi.httpservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.DefaultServlet;
import org.osgi.service.http.HttpContext;

/**
 * An {@link org.osgi.service.http.HttpService} implementation
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jul-2012
 */
@SuppressWarnings("serial")
final class ResourceServlet extends DefaultServlet {

    private final String name;
    private final HttpContext context;

    ResourceServlet(String name, HttpContext context) {
        this.name = name;
        this.context = context;
    }

    @Override
    protected void serveResource(HttpServletRequest req, HttpServletResponse res, boolean content) throws IOException, ServletException {

        // Get the relative path
        String path = req.getPathInfo();
        URL resurl = context.getResource(name + path);
        if (resurl == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
        }

        // [TODO] MimeType, Content-Length, etc.

        ServletOutputStream out = res.getOutputStream();
        InputStream in = resurl.openStream();
        IOUtils.copyStream(out, in);
    }


}