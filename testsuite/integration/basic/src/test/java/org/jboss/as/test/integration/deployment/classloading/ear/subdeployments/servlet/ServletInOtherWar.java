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

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: jpai
 */
@WebServlet(name = "OtherServlet", urlPatterns = ServletInOtherWar.URL_PATTERN)
public class ServletInOtherWar extends HttpServlet {

    public static final String URL_PATTERN = "/otherservlet";

    public static final String CLASS_IN_OTHER_WAR_PARAMETER = "classInOtherWar";

    public static final String SUCCESS_MESSAGE = "Success";

    public static final String FAILURE_MESSAGE = "Failure";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String classInOtherWar = req.getParameter(CLASS_IN_OTHER_WAR_PARAMETER);
        if (classInOtherWar == null) {
            throw new ServletException(CLASS_IN_OTHER_WAR_PARAMETER + " parameter not set in request");
        }
        try {
            Class<?> klass = this.getClass().getClassLoader().loadClass(classInOtherWar);
            // class from one war wasn't expected to be visible to other war
            resp.getOutputStream().print(FAILURE_MESSAGE);
        } catch (ClassNotFoundException cnfe) {
            // the ClassNotFoundException is expected since class in one war isn't expected to be visible to
            // another war (even if it belongs to the same .ear)
            resp.getOutputStream().print(SUCCESS_MESSAGE);
        }


    }
}
