/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.deployment;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.logging.Logger;
//import org.jboss.security.client.SecurityClient;
//import org.jboss.security.client.SecurityClientFactory;

@AdministeredObjectDefinition(className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
        name = "java:app/rardeployment/AppAdmin",
        resourceAdapter = "eis.rar")
@AdministeredObjectDefinitions({
        @AdministeredObjectDefinition(
                className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
                name = "java:comp/rardeployment/CompAdmin",
                resourceAdapter = "eis"),
        @AdministeredObjectDefinition(
                className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
                name = "java:module/rardeployment/ModuleAdmin",
                resourceAdapter = "eis.rar"),
        @AdministeredObjectDefinition(
                className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
                name = "java:global/rardeployment/GlobalAdmin",
                resourceAdapter = "eis")
})

@ConnectionFactoryDefinition(interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
        name = "java:app/rardeployment/AppCF",
        resourceAdapter = "eis.rar")
@ConnectionFactoryDefinitions({
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:comp/rardeployment/CompCF",
                resourceAdapter = "eis"),
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:module/rardeployment/ModuleCF",
                resourceAdapter = "eis.rar"),
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:global/rardeployment/GlobalCF",
                resourceAdapter = "eis")
})
/**
 * A servlet that accesses a resource adapter deployments.
 */
public class RARServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(RARServlet.class);

    @Resource(lookup = "java:app/rardeployment/xml/ao")
    private MultipleAdminObject1 xmlAO;

    @Resource(name = "xml/cf")
    private MultipleConnectionFactory1 xmlCF;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //SecurityClient client = null;
        try {
            InitialContext ctx = new InitialContext();

            //client = SecurityClientFactory.getSecurityClient();
            //client.setSimple("user1", "password1");
            //client.login();

            Object appAdmin = ctx.lookup("java:app/rardeployment/AppAdmin");
            if (appAdmin == null) { throw new ServletException("Failed to access AppAdmin"); }

            Object compAdmin = ctx.lookup("java:comp/rardeployment/CompAdmin");
            if (compAdmin == null) { throw new ServletException("Failed to access CompAdmin"); }

            Object moduleAdmin = ctx.lookup("java:module/rardeployment/ModuleAdmin");
            if (moduleAdmin == null) { throw new ServletException("Failed to access ModuleAdmin"); }

            Object globalAdmin = ctx.lookup("java:global/rardeployment/GlobalAdmin");
            if (globalAdmin == null) { throw new ServletException("Failed to access GlobalAdmin"); }

            Object appCF = ctx.lookup("java:app/rardeployment/AppCF");
            if (appCF == null) { throw new ServletException("Failed to access AppCF"); }

            Object compCF = ctx.lookup("java:comp/rardeployment/CompCF");
            if (compCF == null) { throw new ServletException("Failed to access CompCF"); }

            Object moduleCF = ctx.lookup("java:module/rardeployment/ModuleCF");
            if (moduleCF == null) { throw new ServletException("Failed to access ModuleCF"); }

            Object globalCF = ctx.lookup("java:global/rardeployment/GlobalCF");
            if (globalCF == null) { throw new ServletException("Failed to access GlobalCF"); }

            if (xmlAO == null) { throw new ServletException("Failed to retrieve AO defined in xml descriptor"); }

            if (xmlCF == null) { throw new ServletException("Failed to retrieve CF defined in xml descriptor"); }

        } catch (ServletException se) {
            log.error(se);
            throw se;
        } catch (Exception e) {
            log.error(e);
            throw new ServletException("Failed to access resource adapter", e);
        } finally {
            //client.logout();
        }
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.print("RARServlet OK");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
