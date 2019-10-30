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

package org.jboss.as.test.clustering.cluster.ejb.xpc.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.xpc.bean.Employee;
import org.jboss.as.test.clustering.cluster.ejb.xpc.bean.Stateful;
import org.jboss.as.test.clustering.cluster.ejb.xpc.bean.StatefulBean;
import org.jboss.as.test.clustering.ejb.LocalEJBDirectory;
import org.jboss.logging.Logger;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { StatefulServlet.SERVLET_PATH })
public class StatefulServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "count";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String ANSWER = "answer";
    private static final String COMMAND = "command";
    private static final String BEAN = "bean";
    private static final String MESSAGE = "message";
    private enum Command {
        CREATE_EMPLOYEE,
        GET_EMPLOYEE,
        DELETE_EMPLOYEE,
        GET_EMPLOYEES_IN_2LC,
        GET_2ND_BEAN_EMPLOYEE,
        DESTROY,
        FLUSH,
        ECHO,
        CLEAR,
        DELETE_EMPLOYEE_DIRECT,
        ;
    }

    private static final Logger log = Logger.getLogger(StatefulServlet.class);

    private static URI createURI(URL baseURL, Command command) throws URISyntaxException {
        return baseURL.toURI().resolve(buildCommandQuery(command).toString());
    }

    private static StringBuilder buildCommandQuery(Command command) {
        return new StringBuilder(SERVLET_NAME).append('?').append(COMMAND).append('=').append(command);
    }

    public static URI createEmployeeURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.CREATE_EMPLOYEE);
    }

    public static URI getEmployeeURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.GET_EMPLOYEE);
    }

    public static URI deleteEmployeeURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.DELETE_EMPLOYEE);
    }

    public static URI getEmployeesIn2LCURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.GET_EMPLOYEES_IN_2LC);
    }

    public static URI get2ndBeanEmployeeURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.GET_2ND_BEAN_EMPLOYEE);
    }

    public static URI destroyURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.DESTROY);
    }

    public static URI flushURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.FLUSH);
    }

    public static URI echoURI(URL baseURL, String message) throws URISyntaxException, UnsupportedEncodingException {
        StringBuilder builder = buildCommandQuery(Command.ECHO);
        builder.append('&').append(MESSAGE).append('=').append(URLEncoder.encode(message, "UTF-8"));
        return baseURL.toURI().resolve(builder.toString());
    }

    public static URI clearURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.CLEAR);
    }

    public static URI deleteEmployeeDirectURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Command.DELETE_EMPLOYEE_DIRECT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        Stateful bean = (Stateful) session.getAttribute(BEAN);
        if (bean == null) {
            try (LocalEJBDirectory directory = new LocalEJBDirectory(StatefulBean.MODULE)) {
                bean = directory.lookupStateful(StatefulBean.class, Stateful.class);
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        }

        Command command = Command.valueOf(req.getParameter(COMMAND));
        log.trace(StatefulServlet.class.getName() + ": command = " + command);
        String answer = null;

        switch (command) {
            case CREATE_EMPLOYEE: {
                bean.createEmployee("Tom Brady", "New England Patriots", 1);
                answer = bean.getEmployee(1).getName();
                break;
            }
            case GET_EMPLOYEE: {
                Employee employee = bean.getEmployee(1);
                if (employee == null) {
                    throw new ServletException("couldn't load Employee entity (with id=1) from database");
                }
                answer = employee.getName();
                break;
            }
            case DELETE_EMPLOYEE: {
                bean.deleteEmployee(1);
                answer = command.toString();
                break;
            }
            case GET_EMPLOYEES_IN_2LC: {
                long count = bean.getEmployeesInMemory();
                if (count == -1) {
                    throw new ServletException("couldn't get number of employees in second level cache");
                }
                answer = String.valueOf(count);
                break;
            }
            case GET_2ND_BEAN_EMPLOYEE: {
                Employee employee = bean.getSecondBeanEmployee(1);
                answer = employee.getName();
                break;
            }
            case DESTROY: {
                bean.destroy();
                answer = command.toString();
                break;
            }
            case FLUSH: {
                bean.flush();
                break;
            }
            case ECHO: {
                bean.echo(req.getParameter(MESSAGE));
                break;
            }
            case CLEAR: {
                bean.clear();
                break;
            }
            case DELETE_EMPLOYEE_DIRECT: {
                // delete all employees in db
                int deleted = bean.executeNativeSQL("delete from Employee where id=1");
                if (deleted < 1) {
                    throw new ServletException("couldn't delete entity in database, deleted row count =" + deleted);
                }
                break;
            }
        }

        if (answer != null) {
            resp.setHeader(ANSWER, answer);
        }
        resp.getWriter().write("Success");
        session.setAttribute(BEAN, bean);
        log.trace(StatefulServlet.class.getName() + ": command = " + command + " finished");
    }
}
