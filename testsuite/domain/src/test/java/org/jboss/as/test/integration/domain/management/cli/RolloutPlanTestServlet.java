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
package org.jboss.as.test.integration.domain.management.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@WebServlet(urlPatterns = {"/RolloutServlet"}, loadOnStartup = 1)
public class RolloutPlanTestServlet extends HttpServlet {

    public static final String BIND_PORT_PARAM = "bindPort";
    public static final String OP_PARAM = "operation";
    public static final String OP_BIND = "bind";
    public static final String OP_UNBIND = "unbind";

    private Date initDate;
    private int bindPort;
    private ServerSocket socket;
    private String host;

    private static final Logger log = Logger.getLogger(RolloutPlanTestServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        URL requestURL = new URL(request.getRequestURL().toString());
        host = requestURL.getHost();

        String op = request.getParameter(OP_PARAM);
        if (OP_BIND.equals(op)) {
            bindPort = Integer.valueOf(request.getParameter(BIND_PORT_PARAM));
            bind();
        } else if (OP_UNBIND.equals(op)) { unbind(); }

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.print(String.valueOf(initDate.getTime()));
        out.close();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        initDate = new Date();
        super.init(config);
        log.trace("RolloutServlet initialized: " + String.valueOf(initDate.getTime()));
    }

    @Override
    public void destroy() {
        if (socket != null) {
            try {
                unbind();
            } catch (ServletException se) {}
        }
        super.destroy();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void bind() throws ServletException {
        if (socket != null) { throw new ServletException("Already bound."); }

        try {
            socket = new ServerSocket();
            socket.bind(new InetSocketAddress(host, bindPort));
            log.trace("Bound to address " + host + " port " + bindPort + ".");
        } catch (IOException ioe) {
            throw new ServletException("Bind failed.", ioe);
        }
    }

    private void unbind() throws ServletException {
        if (socket == null) { throw new ServletException("Not bound."); }

        try {
            socket.close();
            socket = null;
            log.trace("Unbound from address " + host + " port " + bindPort + ".");
        } catch (IOException ioe) {
            throw new ServletException("Unbind failed.", ioe);
        }
    }
}
