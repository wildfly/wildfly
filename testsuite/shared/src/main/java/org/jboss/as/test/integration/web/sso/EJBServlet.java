/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.sso;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that accesses an EJB and tests whether the call argument is
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
