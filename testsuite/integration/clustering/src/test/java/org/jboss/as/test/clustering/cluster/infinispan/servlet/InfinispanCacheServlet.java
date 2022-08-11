/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.infinispan.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.clustering.cluster.infinispan.bean.Cache;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { InfinispanCacheServlet.SERVLET_PATH })
public class InfinispanCacheServlet extends HttpServlet {
    private static final long serialVersionUID = 8191787312871139014L;

    private static final String SERVLET_NAME = "cache";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String KEY = "key";
    private static final String VALUE = "value";
    public static final String RESULT = "result";

    public static URI createURI(URL baseURL, String key) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME + '?' + KEY + '=' + key);
    }

    public static URI createURI(URL baseURL, String key, String value) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME + '?' + KEY + '=' + key + '&' + VALUE + '=' + value);
    }

    @EJB
    private Cache bean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = requireParameter(request, KEY);
        String result = this.bean.get(key);
        if (result != null) {
            response.setHeader(RESULT, result);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = requireParameter(request, KEY);
        String value = requireParameter(request, VALUE);
        String result = this.bean.put(key, value);
        if (result != null) {
            response.setHeader(RESULT, result);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = requireParameter(request, KEY);
        String result = this.bean.remove(key);
        if (result != null) {
            response.setHeader(RESULT, result);
        }
    }

    private static String requireParameter(HttpServletRequest request, String name) throws ServletException {
        String result = request.getParameter(name);
        if (result == null) {
            throw new ServletException("Missing parameter " + name);
        }
        return result;
    }
}
