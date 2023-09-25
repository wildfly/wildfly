/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * This servlet allows to list credential stores, aliases in them and also the secret values for the aliases. <br/>
 * It has optional request parameters:
 * <ul>
 * <li>{@value #PARAM_CREDENTIAL_STORE} - for configuring name of credential store</li>
 * <li>{@value #PARAM_ALIAS} - for configuring alias in credential store</li>
 * <li>{@value #PARAM_SEPARATOR} - for configuring value separator when list of values is returned (default separator is the
 * line break)</li>
 * </ul>
 *
 * <p>
 * If request parameter "{@value #PARAM_CREDENTIAL_STORE}" is not provided, list of credential store names is returned.
 * </p>
 * <p>
 * If request parameter "{@value #PARAM_CREDENTIAL_STORE}" is provided and "{@value #PARAM_CREDENTIAL_STORE}" is not provided ,
 * list of aliases in the given credential store names is returned.
 * </p>
 * <p>
 * If both request parameters ("{@value #PARAM_CREDENTIAL_STORE}", "{@value #PARAM_CREDENTIAL_STORE}") are provided , secret
 * value for given alias is returned.
 * </p>
 *
 * <p>
 * If name parameter is provided but given name (store or alias) is not found, then {@value HttpServletResponse#SC_NOT_FOUND} is
 * returned as reponse status code.
 * </p>
 *
 *
 * @author Josef Cacek
 */
@WebServlet(urlPatterns = { ReadCredentialServlet.SERVLET_PATH })
public class ReadCredentialServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/readCredential";

    public static final String PARAM_CREDENTIAL_STORE = "credentialStore";
    public static final String PARAM_ALIAS = "alias";
    public static final String PARAM_SEPARATOR = "separator";

    public static final String PARAM_SEPARATOR_DEFAULT = "\n";

    private static final ServiceName SERVICE_NAME_CRED_STORE = ServiceName.of("org", "wildfly", "security", "credential-store");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        final PrintWriter writer = resp.getWriter();
        final String credentialStore = req.getParameter(PARAM_CREDENTIAL_STORE);
        final String alias = req.getParameter(PARAM_ALIAS);
        String separator = req.getParameter(PARAM_SEPARATOR);
        if (separator == null) {
            separator = PARAM_SEPARATOR_DEFAULT;
        }

        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        if (credentialStore == null || credentialStore.length() == 0) {
            for (ServiceName name : registry.getServiceNames()) {
                if (SERVICE_NAME_CRED_STORE.equals(name.getParent())) {
                    writer.print(name.getSimpleName());
                    writer.print(separator);
                }
            }
            return;
        }

        ServiceController<?> credStoreService = registry.getService(ServiceName.of(SERVICE_NAME_CRED_STORE, credentialStore));
        if (credStoreService == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.print(credentialStore + " not found");
            return;
        }
        CredentialStore cs = (CredentialStore) credStoreService.getValue();

        if (alias == null || alias.length() == 0) {
            try {
                for (String csAlias : cs.getAliases()) {
                    writer.print(csAlias);
                    writer.print(separator);
                }
            } catch (UnsupportedOperationException | CredentialStoreException e) {
                throw new ServletException("Unable to list aliases", e);
            }
            return;
        }

        String clearPassword = null;

        try {
            if (cs.exists(alias, PasswordCredential.class)) {
                Password password = cs.retrieve(alias, PasswordCredential.class).getPassword();
                if (password instanceof ClearPassword) {
                    clearPassword = new String(((ClearPassword) password).getPassword());
                }
            }
        } catch (CredentialStoreException | IllegalStateException e) {
            throw new ServletException("Unable to retrieve password  from credential store", e);
        }
        if (clearPassword == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.print(alias + " password not found in " + credentialStore);
        } else {
            writer.print(clearPassword);
        }
    }

}
