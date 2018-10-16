/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.context;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/servlet2")
public class Servlet2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public Servlet2() {
        super();
    }

    /**
     * @see HttpServlet#service(HttpServletRequest request, HttpServletResponse response)
     */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession webApp1Session = (HttpSession) request.getAttribute("WEBAPP1_SESSION");
        HttpSession webApp2Session = request.getSession();

        webApp2Session.setAttribute("WEBAPP_CONTEXT_PATH", getServletContext().getContextPath());

        PrintWriter writer = response.getWriter();

        if (webApp1Session != null) {
            writer.println("webapp1 session is " + (webApp1Session.isNew()? "NEW" : "NOT NEW"));
            writer.println("webapp1 JSESSIONID: " + webApp1Session.getId());
            writer.println("webapp1 context path: " + webApp1Session.getAttribute("WEBAPP_CONTEXT_PATH"));
        }

        if (webApp2Session != null) {
            writer.println("webapp2 session is " + (webApp2Session.isNew()? "NEW" : "NOT NEW"));
            writer.println("webapp2 JSESSIONID: " + webApp2Session.getId());
            writer.println("webapp2 context path: " + webApp2Session.getAttribute("WEBAPP_CONTEXT_PATH"));
        }
    }

}
