/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.customerrors;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;

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
