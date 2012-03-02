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
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import static org.junit.Assert.assertTrue;

import org.jboss.as.test.clustering.LocalEJBDirectory;

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
                bean = new LocalEJBDirectory("stateful").lookupStateful(StatefulBean.class, Stateful.class);
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        }

        String command = req.getParameter("command");
        System.out.println(StatefulServlet.class.getName() + ": command = " + command);
        String answer = null;

        if("createEmployee".equals(command)) {
            bean.createEmployee("Tom Brady","New England Patriots", 1);
            answer = bean.getEmployee(1).getName();
        }
        else if("getEmployee".equals(command)) {
            Employee employee = bean.getEmployee(1);
            if (employee == null) {
                throw new ServletException("couldn't load Employee entity (with id=1) from database");
            }
            answer = employee.getName();
        }
        else if("deleteEmployee".equals(command)) {
            bean.deleteEmployee(1);
            answer = command;
        }
        else if ("getEmployeesInSecondLevelCache".equals(command)) {
            long count = bean.getEmployeesInMemory();
            if(count == -1) {
                throw new ServletException("couldn't get number of employees in second level cache");
            }
            answer = "" + count;
        }
        else if("getSecondBeanEmployee".equals(command)) {
            Employee employee = bean.getSecondBeanEmployee(1);
            answer = employee.getName();
        }
        else if("destroy".equals(command)) {
            bean.destroy();
            answer = command;
        }
        else if("flush".equals(command)) {
            bean.flush();
        }
        else if("echo".equals(command)) {
            bean.echo(req.getParameter("message"));
        }
        else if("clear".equals(command)) {
            bean.clear();
        }
        else if("deleteEmployeeViaJDBC".equals(command)) {
            // delete all employees in db
            int deleted = bean.executeNativeSQL("delete from Employee where id=1");
            if (deleted < 1) {
                throw new ServletException("couldn't delete entity in database, deleted row count =" + deleted);
            }
        }
        else {
            throw new ServletException("unknown command name=" +command );
        }

        if( answer != null) {
            resp.setHeader("answer", answer);
        }
        resp.getWriter().write("Success");
        session.setAttribute("bean", bean);
        System.out.println(StatefulServlet.class.getName() + ": command = " + command + " finished");
    }
}
