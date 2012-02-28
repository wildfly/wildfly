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

package org.jboss.as.test.clustering.cluster.ejb3.xpc.bean;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 *
 */
@WebServlet(urlPatterns = { "/count" })
public class StatefulServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        Stateful bean = (Stateful)session.getAttribute("bean");
        if (bean == null) {
            try {
                bean =  (Stateful)new InitialContext().lookup("java:app/stateful/StatefulBean!" +
                    Stateful.class.getName());
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        }

        String command = req.getParameter("command");

        if("createEmployee".equals(command)) {
            bean.createEmployee("Tom Brady","New England Patriots", 1);
            resp.setHeader("employee", bean.getEmployee(1).getName());
        }
        else if("getEmployee".equals(command)) {
            Employee employee = bean.getEmployee(1);
            resp.setHeader("employee", employee.getName());
        }
        else if("getSecondBeanEmployee".equals(command)) {
            Employee employee = bean.getSecondBeanEmployee(1);
            resp.setHeader("employee", employee.getName());
        }
        else if("destroy".equals(command)) {
            bean.destroy();
            resp.setHeader("employee", command);
        }


        resp.getWriter().write("Success");
        session.setAttribute("bean", bean);
    }
}
