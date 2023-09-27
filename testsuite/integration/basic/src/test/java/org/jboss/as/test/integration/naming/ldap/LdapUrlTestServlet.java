/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.naming.ldap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet which tries to do a search in LDAP server. Default call use InitialDirContext. You can add parameter
 * {@link #PARAM_LDAP} to your request and then InitialLdapContext is used.
 *
 * @author Josef Cacek
 */
@WebServlet(urlPatterns = { LdapUrlTestServlet.SERVLET_PATH })
public class LdapUrlTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/*";
    public static final String PARAM_HOST = "host";
    public static final String PARAM_LDAP = "ldapctx";

    /**
     * Writes simple text response.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        try {
            String host = req.getParameter(PARAM_HOST);
            writer.write(runSearch(host, req.getParameter(PARAM_LDAP) != null));
        } catch (Exception e) {
            throw new ServletException(e);
        }
        writer.close();
    }

    /**
     * Try to search in LDAP with search base containing URL. Also try to retrieve RequestControls from LdapContext.
     *
     * @param hostname
     * @return
     * @throws Exception
     */
    public static String runSearch(final String hostname, boolean testLdapCtx) throws Exception {
        final StringBuilder result = new StringBuilder();
        final String ldapUrl = "ldap://" + (hostname == null ? "localhost" : hostname) + ":10389";

        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        env.put(Context.SECURITY_CREDENTIALS, "secret");

        final SearchControls ctl = new SearchControls();
        ctl.setReturningAttributes(new String[] { "cn" });

        DirContext dirCtx = null;
        if (testLdapCtx) {
            // LdapContext must also work
            LdapContext ldapCtx = new InitialLdapContext(env, null);
            // next line tests if the LdapContext works
            ldapCtx.getRequestControls();
            dirCtx = ldapCtx;
        } else {
            dirCtx = new InitialDirContext(env);
        }
        final NamingEnumeration<SearchResult> nenum = dirCtx.search(ldapUrl + "/dc=jboss,dc=org", "(uid=jduke)", ctl);

        while (nenum.hasMore()) {
            SearchResult sr = nenum.next();
            Attributes attrs = sr.getAttributes();
            result.append("cn=").append(attrs.get("cn").get());
        }
        dirCtx.close();

        return result.toString();
    }
}
