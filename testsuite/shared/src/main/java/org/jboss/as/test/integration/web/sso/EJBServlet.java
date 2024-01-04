/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet that accesses an Jakarta Enterprise Beans and tests whether the call argument is
 * serialized.
 *
 * @author Scott.Stark@jboss.org
 * @author
 */
public class EJBServlet extends HttpServlet {

    private static final long serialVersionUID = 2070931818661985879L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
/*
        try {
            InitialContext ctx = new InitialContext();
            Context enc = (Context) ctx.lookup("java:comp/env");
            StatelessSessionHome home = (StatelessSessionHome) enc.lookup("ejb/OptimizedEJB");
            StatelessSession bean = home.create();
            bean.noop();

            Object homeRef = enc.lookup("ejb/OptimizedEJB");
            home = (StatelessSessionHome) PortableRemoteObject.narrow(homeRef, StatelessSessionHome.class);
            bean = home.create();
            bean.noop();
            bean.getData();

            StatelessSessionLocalHome localHome = (StatelessSessionLocalHome) enc.lookup("ejb/local/OptimizedEJB");
            StatelessSessionLocal localBean = localHome.create();
            localBean.noop();
        } catch (Exception e) {
            throw new ServletException("Failed to call OptimizedEJB through remote and local interfaces", e);
        }
*/
        response.setContentType("text/html");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head><title>EJBServlet</title></head>");
            out.println("<body>Tests passed<br>Time:" + new Date().toString() + "</body>");
            out.println("</html>");
        }
    }
}
