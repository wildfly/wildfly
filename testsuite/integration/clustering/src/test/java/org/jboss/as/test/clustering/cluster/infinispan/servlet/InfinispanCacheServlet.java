/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
