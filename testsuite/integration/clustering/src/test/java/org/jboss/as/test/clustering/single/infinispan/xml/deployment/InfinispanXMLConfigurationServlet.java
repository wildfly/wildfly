/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.xml.deployment;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.infinispan.manager.DefaultCacheManager;

/**
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = {InfinispanXMLConfigurationServlet.SERVLET_PATH})
public class InfinispanXMLConfigurationServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 4455812466698420161L;
    private static final String SERVLET_NAME = "xml";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        new DefaultCacheManager("infinispan.xml").stop();
    }

}
