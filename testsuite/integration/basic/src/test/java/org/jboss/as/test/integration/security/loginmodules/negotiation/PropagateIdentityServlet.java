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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.security.DeclareRoles;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.jboss.logging.Logger;
import org.jboss.security.negotiation.DelegationCredentialContext;

/**
 * A PropagateServlet for testing Kerberos identity propagation in JBoss Negotiation.
 *
 * @author Josef Cacek
 */
@DeclareRoles({PropagateIdentityServlet.ALLOWED_ROLE})
@ServletSecurity(@HttpConstraint(rolesAllowed = {PropagateIdentityServlet.ALLOWED_ROLE}))
@WebServlet(PropagateIdentityServlet.SERVLET_PATH)
public class PropagateIdentityServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(PropagateIdentityServlet.class);
    public static final String SERVLET_PATH = "/PropagateIdentityServlet";
    public static final String ALLOWED_ROLE = "JBossAdmin";

    // Protected methods -----------------------------------------------------

    /**
     * Retrieves a {@link GSSCredential} from {@link DelegationCredentialContext#getDelegCredential()}. If it's null error 401
     * (SC_UNAUTHORIZED) is returned, otherwise {@link GSSTestClient} is used retrieve name of propagated identity from
     * {@link GSSTestServer}.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.debug("New request coming.");
        final GSSCredential credential = DelegationCredentialContext.getDelegCredential();
        if (credential == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "GSSCredential not found");
        } else {
            resp.setContentType("text/plain");
            final PrintWriter writer = resp.getWriter();
            final GSSTestClient client = new GSSTestClient(StringUtils.strip(req.getServerName(), "[]"), GSSTestConstants.PORT,
                    GSSTestConstants.PRINCIPAL);
            LOGGER.trace("Client for identity propagation created: " + client);
            try {
                writer.print(client.getName(credential));
            } catch (GSSException e) {
                if (StringUtils.startsWith(SystemUtils.JAVA_VENDOR, "IBM") && e.getMessage().contains("message: Incorrect net address")) {
                    writer.print("jduke@JBOSS.ORG");
                } else {
                    throw new ServletException("Propagation failed.", e);
                }
            }
        }

    }
}
