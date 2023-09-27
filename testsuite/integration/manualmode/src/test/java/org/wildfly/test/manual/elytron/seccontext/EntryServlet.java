/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.concurrent.Callable;

import jakarta.annotation.security.DeclareRoles;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Secured servlet which just calls remote {@link WhoAmI} bean and as a response it returns either the bean call result or
 * exception stack trace.
 *
 * @author Josef Cacek
 */
@WebServlet(urlPatterns = EntryServlet.SERVLET_PATH)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "servlet", "admin" }))
@DeclareRoles({ "entry", "whoami", "servlet", "admin" })
public class EntryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/EntryServlet";
    public static final String PARAM_PROVIDER_URL = "providerUrl";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_REAUTHN_TYPE = "reAuthnType";
    public static final String PARAM_STATEFULL = "statefull";
    public static final String PARAM_CREATE_SESSION = "createSession";
    public static final String PARAM_AUTHZ_NAME = "authzName";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PrintWriter writer = resp.getWriter();
        final String username = req.getParameter(PARAM_USERNAME);
        final String password = req.getParameter(PARAM_PASSWORD);
        final String providerUrl = req.getParameter(PARAM_PROVIDER_URL);
        final String type = req.getParameter(PARAM_REAUTHN_TYPE);
        final String statefull = req.getParameter(PARAM_STATEFULL);
        final String createSession = req.getParameter(PARAM_CREATE_SESSION);
        final String authzName = req.getParameter(PARAM_AUTHZ_NAME);

        if (Boolean.parseBoolean(createSession)) {
            req.getSession();
        }

        final Principal beforePrincipal = req.getUserPrincipal();

        final ReAuthnType reAuthnType = type != null ? ReAuthnType.valueOf(type) : ReAuthnType.FORWARDED_AUTHENTICATION;

        final Callable<String> callable = () -> {
            return getWhoAmIBean(providerUrl, Boolean.parseBoolean(statefull)).getCallerPrincipal().getName();
        };
        try {
            writer.write(SeccontextUtil.switchIdentity(username, password, authzName, callable, reAuthnType));
        } catch (Exception e) {
            e.printStackTrace(writer);
        } finally {
            final Principal afterPrincipal = req.getUserPrincipal();
            if (beforePrincipal != null && !beforePrincipal.equals(afterPrincipal)) {
                throw new IllegalStateException(
                        "Local getUserPrincipal() changed from '" + beforePrincipal + "' to '" + afterPrincipal + "'");
            }
        }
    }

    private WhoAmI getWhoAmIBean(String providerUrl, boolean statefullWhoAmI) throws NamingException {
        return SeccontextUtil.lookup(
                SeccontextUtil.getRemoteEjbName(WAR_WHOAMI, "WhoAmIBean", WhoAmI.class.getName(), statefullWhoAmI), providerUrl);
    }
}
