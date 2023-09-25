/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.propagation.remote;

import static org.jboss.as.test.shared.integration.ejb.security.Util.switchIdentity;

import java.util.concurrent.Callable;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A simple EJB that can be called to obtain the current caller principal and to check the role membership for
 * that principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
public class ManagementBeanRemote {

    @Resource
    private SessionContext context;

    @PermitAll
    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    @PermitAll
    public boolean invokeEntryDoIHaveRole(String role) {
        EntryRemote entry = lookup(EntryRemote.class, "java:global/ear-ejb-deployment-remote/ear-ejb-deployment-remote-ejb/EntryBeanRemote!org.wildfly.test.integration.elytron.oidc.client.propagation.remote.EntryRemote");
        return entry.doIHaveRole(role);
    }

    @PermitAll
    public String[] switchThenInvokeEntryDoIHaveRole(String username, String password, String role) {
        EntryRemote entry = lookup(EntryRemote.class, "java:global/ear-ejb-deployment-remote/ear-ejb-deployment-remote-ejb/EntryBeanRemote!org.wildfly.test.integration.elytron.oidc.client.propagation.remote.EntryRemote");
        final Callable<String[]> callable = () -> {
            String remoteWho = entry.whoAmI();
            boolean hasRole = entry.doIHaveRole(role);
            return new String[] { remoteWho, String.valueOf(hasRole) };
        };
        try {
            return switchIdentity(username, password, callable);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T lookup(Class<T> clazz, String jndiName) {
        Object bean = lookup(jndiName);
        return clazz.cast(bean);
    }

    private static Object lookup(String jndiName) {
        Context context = null;
        try {
            context = new InitialContext();
            return context.lookup(jndiName);
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        } finally {
            try {
                context.close();
            } catch (NamingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
