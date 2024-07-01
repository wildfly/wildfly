/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.management.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Date;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
            bindPort = Integer.parseInt(request.getParameter(BIND_PORT_PARAM));
            bind();
        } else if (OP_UNBIND.equals(op)) { unbind(); }

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.print(initDate.getTime());
        out.close();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        initDate = new Date();
        super.init(config);
        log.trace("RolloutServlet initialized: " + initDate.getTime());
    }

    @Override
    public void destroy() {
        if (socket != null) {
            try {
                unbind();
            } catch (ServletException se) {
                // ignore
            }
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
