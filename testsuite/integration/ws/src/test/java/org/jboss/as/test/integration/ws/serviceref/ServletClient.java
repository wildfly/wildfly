/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.serviceref;

import java.io.IOException;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public class ServletClient extends HttpServlet {

    private static Logger log = Logger.getLogger(ServletClient.class);

    @WebServiceRef(value = EndpointService.class, mappedName = "jbossws-client/service/TestService", wsdlLocation = "WEB-INF/wsdl/TestService.wsdl")
    EndpointInterface endpoint1;

    EndpointInterface _endpoint2;

    @WebServiceRef(value = EndpointService.class, mappedName = "jbossws-client/service/TestService", wsdlLocation = "WEB-INF/wsdl/TestService.wsdl")
    public void setEndpoint2(final EndpointInterface endpoint2) {
        this._endpoint2 = endpoint2;
    }

    public String echo1(final String string) throws Exception {
        if (null == endpoint1) {
            throw new IllegalArgumentException("Serviceref for property 'endpoint1' not injected");
        }
        return endpoint1.echo(string);
    }

    public String echo2(final String string) throws Exception {
        if (null == _endpoint2) {
            throw new IllegalArgumentException("Serviceref for property 'endpoint2' not injected");
        }
        return _endpoint2.echo(string);
    }

    // service3 and service4 are defined in web.xml
    public String echo3(final String string) throws Exception {
        InitialContext iniCtx = new InitialContext();
        EndpointInterface endpoint3 = (EndpointInterface) ((Service) iniCtx.lookup("java:comp/env/service3")).getPort(EndpointInterface.class);
        if (null == endpoint3) {
            throw new IllegalArgumentException("Serviceref for 'service3' not injected");
        }
        return endpoint3.echo(string);
    }

    public String echo4(final String string) throws Exception {
        InitialContext iniCtx = new InitialContext();
        EndpointInterface endpoint4 = ((EndpointService) iniCtx.lookup("java:comp/env/service4")).getEndpointPort();
        if (null == endpoint4) {
            throw new IllegalArgumentException("Serviceref for 'service4' not injected");
        }
        return endpoint4.echo(string);
    }

    // service5 is defined in jboss-web.xml
    public String echo5(final String string) throws Exception {
        InitialContext iniCtx = new InitialContext();
        EndpointInterface endpoint5 = (EndpointInterface) ((Service) iniCtx.lookup("java:comp/env/service5")).getPort(EndpointInterface.class);
        if (null == endpoint5) {
            throw new IllegalArgumentException("Serviceref for 'service5' not injected");
        }
        return endpoint5.echo(string);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String echoStr = req.getParameter("echo");
        String typeStr = req.getParameter("type");

        try {
            if (typeStr.equals("echo1")) {
                log.trace("Echo1: " + echo1(echoStr));
            } else if (typeStr.equals("echo2")) {
                log.trace("Echo2: " + echo2(echoStr));
            } else if (typeStr.equals("echo3")) {
                log.trace("Echo3: " + echo3(echoStr));
            } else if (typeStr.equals("echo4")) {
                log.trace("Echo4: " + echo4(echoStr));
            } else if (typeStr.equals("echo5")) {
                log.trace("Echo5: " + echo5(echoStr));
            }
        } catch (Exception ex) {
            resp.getWriter().println(ex.toString());
        }
        resp.getWriter().print(echoStr);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }
}
