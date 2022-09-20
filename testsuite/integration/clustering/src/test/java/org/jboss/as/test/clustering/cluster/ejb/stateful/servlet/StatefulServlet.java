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

package org.jboss.as.test.clustering.cluster.ejb.stateful.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.ejb.LocalEJBDirectory;

/**
 * Test servlet that stores a SFSB reference within the HttpSession.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { StatefulServlet.SERVLET_PATH })
public class StatefulServlet extends AbstractStatefulServlet {
    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "count";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String MODULE = "module";
    public static final String COUNT = "count";
    public static final String BEAN = "bean";

    public static URI createURI(URL baseURL, String module, String bean) throws URISyntaxException {
        StringBuilder builder = new StringBuilder(SERVLET_NAME)
                .append('?').append(MODULE).append('=').append(module)
                .append('&').append(BEAN).append('=').append(bean)
        ;
        return baseURL.toURI().resolve(builder.toString());
    }

    @Override
    public Incrementor apply(HttpServletRequest request) throws ServletException {
        Incrementor incrementor = (Incrementor) request.getSession().getAttribute(BEAN);
        if (incrementor == null) {
            String module = getRequiredParameter(request, MODULE);
            String bean = getRequiredParameter(request, BEAN);
            try (LocalEJBDirectory directory = new LocalEJBDirectory(module)) {
                incrementor = directory.lookupStateful(bean, Incrementor.class);
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        }
        return incrementor;
    }

    @Override
    public void accept(HttpSession session, Incrementor incrementor) {
        session.setAttribute(BEAN, incrementor);
    }

    private static String getRequiredParameter(HttpServletRequest req, String name) throws ServletException {
        String value = req.getParameter(name);
        if (value == null) {
            throw new ServletException("Missing parameter: " + name);
        }
        return value;
    }
}
