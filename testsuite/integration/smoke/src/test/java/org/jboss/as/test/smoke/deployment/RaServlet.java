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

package org.jboss.as.test.smoke.deployment;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;

/**
 * User: Jaikiran Pai
 */
@WebServlet(name = "RaServlet", urlPatterns = RaServlet.URL_PATTERN)
public class RaServlet extends HttpServlet {

    public static final String SUCCESS = "SUCCESS";
    public static final String URL_PATTERN = "/raservlet";

    @Resource(name = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;


    @Resource(name = "java:jboss/Name3")
    private MultipleAdminObject1 adminObject1;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuffer sb = new StringBuffer();
        if (connectionFactory1 == null) sb.append("CF1 is null.");
        if (adminObject1 == null) sb.append("AO1 is null.");
        resp.getOutputStream().print((sb.length() > 0) ? sb.toString() : SUCCESS);
    }
}
