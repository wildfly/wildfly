/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.security.DeclareRoles;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.jboss.as.test.multinode.security.api.EJBAction;
import org.jboss.as.test.multinode.security.api.EJBRequest;
import org.jboss.as.test.multinode.security.api.InvocationPath;
import org.jboss.as.test.multinode.security.api.Results;
import org.jboss.as.test.multinode.security.api.TestConfig;
import org.jboss.logging.Logger;

/**
 * @author bmaxwell
 */
@WebServlet(name = "SecurityWebClientServlet", urlPatterns = { "/" }, loadOnStartup = 1)
@DeclareRoles(TestConfig.SECURITY_WEB_ROLE)
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { TestConfig.SECURITY_WEB_ROLE }), httpMethodConstraints = {
        @HttpMethodConstraint(value = "GET", rolesAllowed = { TestConfig.SECURITY_WEB_ROLE }), @HttpMethodConstraint(value = "POST") })
public class SecurityWebClientServlet extends HttpServlet {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private String nodeName = System.getProperty("jboss.node.name");

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("processRequest invoked , remoteUser: " + request.getRemoteUser());
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            log.debug("ejbRequest: " + request.getParameter("ejbRequest"));
            EJBRequest ejbRequest = EJBRequest.unmarshall(request.getParameter("ejbRequest"));
            Results results = invokeRemoteEJB(request, ejbRequest);
            out.write(results.marshall());
            log.debug(results.marshall());
            out.flush();
        } catch (Throwable t) {
            try {
                out.write(new Results(t).marshall());
                t.printStackTrace(System.err);
            } catch (JAXBException je) {
                throw new ServletException(je);
            }
        } finally {
            out.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private Results invokeRemoteEJB(HttpServletRequest request, EJBRequest ejbRequest) {

        EJBRequest response = ejbRequest;
        InvocationPath path = new InvocationPath(TestConfig.SERVLET_1, nodeName, request.getRemoteUser());
        response.getInvocationPath().add(path);
        EJBAction action = null;
        try {
            boolean hasAction = (response.getActions().isEmpty() == false);
            while(hasAction) {
                action = response.getActions().remove(0);
                log.debug(action.toString());
                response = action.invoke(response);
                hasAction = (response.getActions().isEmpty() == false);
            }
        } catch(Exception e) {
            path.setException(new Exception(action.toString(), e));
        }
        return new Results(request.getRemoteUser(), response);
    }
}