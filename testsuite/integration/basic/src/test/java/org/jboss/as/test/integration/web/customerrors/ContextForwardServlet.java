/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.web.customerrors;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;

import org.jboss.logging.Logger;

/**
 * A generic fowarding servlet that obtains the ServletContext for the
 * forwardContext init-param and then obtain a request dispatcher for the
 * request.getPathInfo and forward the request. This allows a global servlet
 * mapping to redirect requests to a common target. An example would be a global
 * web.xml error-pages with a single shared context for the web pages. This
 * requires that the web context crossContext attribute is set to true.
 *
 * @author Scott.Stark@jboss.org
 */
public class ContextForwardServlet extends HttpServlet {

    private static final long serialVersionUID = -853278446594804509L;

    private static Logger log = Logger.getLogger(ContextForwardServlet.class);

    /** The name of the context to which requests are forwarded */
    private String forwardContext = "/error-pages";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String param = config.getInitParameter("forwardContext");
        if (param != null)
            forwardContext = param;
    }

    /**
     * Lookup the ServletContext associated with the forwardContext init-param
     * and then obtain a request dispatcher for the request.getPathInfo and
     * forward the request. This allows a global servlet mapping to redirect
     * requests to a common target.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (log.isTraceEnabled()) {
            log.trace("[" + forwardContext + "], PathInfo: " + request.getPathInfo() + ", QueryString: "
                    + request.getQueryString() + ", ContextPath: " + request.getContextPath() + ", HeaderNames: "
                    + request.getHeaderNames() + ", isCommitted: " + response.isCommitted());
        }
        String path = request.getPathInfo();
        ServletContext sc = getServletContext().getContext(forwardContext);
        if (sc != null) {
            if (log.isTraceEnabled())
                log.trace("Found ServletContext for: " + forwardContext);
            RequestDispatcher rd = sc.getRequestDispatcher(path);
            if (rd != null) {
                if (log.isTraceEnabled())
                    log.trace("Found RequestDispatcher for: " + path);
                rd.forward(request, response);
                return;
            }
        }
        throw new ServletException("No RequestDispatcher for: " + forwardContext + "/" + path);
    }

}