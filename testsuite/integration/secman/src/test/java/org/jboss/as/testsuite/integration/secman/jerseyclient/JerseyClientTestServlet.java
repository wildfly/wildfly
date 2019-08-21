/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.secman.jerseyclient;

import org.jboss.as.test.shared.TestSuiteEnvironment;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

/**
 * Test servlet for JerseyClientTestCase with ClientBuilder
 *
 * @author Daniel Cihak
 */
@WebServlet(JerseyClientTestServlet.SERVLET_PATH)
public class JerseyClientTestServlet extends HttpServlet {

    public static final String SERVLET_PATH = "/JerseyClientTestServlet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        ClientBuilder.newBuilder()
                .build()
                .target("http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort())
                .request(MediaType.TEXT_HTML)
                .get()
                .readEntity(String.class);
    }
}
