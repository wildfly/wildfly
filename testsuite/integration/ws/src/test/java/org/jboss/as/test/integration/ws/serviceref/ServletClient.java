/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.serviceref;

import java.io.IOException;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceRef;

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
