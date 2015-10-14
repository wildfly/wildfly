/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.datasource.remove;

import org.jboss.as.test.shared.TimeoutUtil;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@WebServlet(name = RemoveDsServlet.SERVLET_NAME, urlPatterns = {"/" + RemoveDsServlet.SERVLET_NAME})
public class RemoveDsServlet extends HttpServlet {
    public static final String SERVLET_NAME = "RemoveDsServlet";

    @Resource(mappedName = "java:jboss/datasources/RemoveDS")
    private DataSource removeDS;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message = "ok";

        if (removeDS == null) {
            message = "bad";
        }

        try (Connection connection = removeDS.getConnection()) {
            if (connection == null || !connection.isValid(TimeoutUtil.adjust(1000))) {
                message = "bad";
            }
        } catch (SQLException e) {
            System.out.println("Catched exception: " + e);
            message = "bad";
        }

        resp.getWriter().write(message);
        resp.getWriter().close();
    }

}
