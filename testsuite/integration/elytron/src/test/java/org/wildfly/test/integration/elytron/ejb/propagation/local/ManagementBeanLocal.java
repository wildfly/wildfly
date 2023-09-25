/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.propagation.local;

import static org.jboss.as.test.shared.integration.ejb.security.Util.switchIdentity;

import java.util.concurrent.Callable;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * A simple EJB that can be called to obtain the current caller principal and to check the role membership for
 * that principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("elytron-tests")
public class ManagementBeanLocal {

    @Resource
    private SessionContext context;

    @PermitAll
    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    @PermitAll
    public boolean invokeEntryDoIHaveRole(String role) {
        EntryLocal entry = lookup(EntryLocal.class, "java:global/ear-ejb-deployment-local/ear-ejb-deployment-local-ejb/EntryBeanLocal!org.wildfly.test.integration.elytron.ejb.propagation.local.EntryLocal");
        return entry.doIHaveRole(role);
    }

    @PermitAll
    public String[] switchThenInvokeEntryDoIHaveRole(String username, String password, String role) {
        EntryLocal entry = lookup(EntryLocal.class, "java:global/ear-ejb-deployment-local/ear-ejb-deployment-local-ejb/EntryBeanLocal!org.wildfly.test.integration.elytron.ejb.propagation.local.EntryLocal");
        final Callable<String[]> callable = () -> {
            String localWho = entry.whoAmI();
            boolean hasRole = entry.doIHaveRole(role);
            return new String[] { localWho, String.valueOf(hasRole) };
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
