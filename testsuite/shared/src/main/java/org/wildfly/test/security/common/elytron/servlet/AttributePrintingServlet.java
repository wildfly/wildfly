/*
 * Copyright 2019 Red Hat, Inc.
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

package org.wildfly.test.security.common.elytron.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.Attributes.Entry;

/**
 * An AttributePrintingServlet returns all attributes in a properties format in the response.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns = { AttributePrintingServlet.SERVLET_PATH })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "*" }))
public class AttributePrintingServlet extends HttpServlet {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The default servlet path (used in {@link WebServlet} annotation). */
    public static final String SERVLET_PATH = "/printAttributes";


    /**
     * Writes plain-text response with all of the current identities attributes.
     *
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();

        SecurityDomain securityDomain = SecurityDomain.getCurrent();
        SecurityIdentity securityIdentity = securityDomain.getCurrentSecurityIdentity();
        Attributes attributes = securityIdentity.getAttributes();
        for (Entry currentAttribute : attributes.entries()) {
            writer.print(currentAttribute.getKey());
            writer.print("=");
            for (int i=0; i<currentAttribute.size(); i++) {
                writer.print(currentAttribute.get(i));
                if (i < currentAttribute.size()) {
                    writer.print(",");
                }
            }
            writer.println();
        }

        writer.close();
    }
}
