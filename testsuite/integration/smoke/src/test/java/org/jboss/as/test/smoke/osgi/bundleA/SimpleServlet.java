/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.osgi.bundleA;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.as.test.smoke.osgi.bundleB.Echo;

@SuppressWarnings("serial")
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class SimpleServlet extends HttpServlet {

    private volatile Echo echo;

    @PostConstruct
    public void messageSetup() {
        echo = new Echo() {
            @Override
            public String echo(String msg) {
                return "Simple Servlet called with input=" + msg;
            }
        };
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String msg = req.getParameter("input");
        Writer writer = resp.getWriter();
        writer.write(echo.echo(msg));
    }
}
