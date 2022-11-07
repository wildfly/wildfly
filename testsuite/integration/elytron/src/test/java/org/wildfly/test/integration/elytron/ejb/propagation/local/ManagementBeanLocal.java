/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2022, Red Hat, Inc., and individual contributors
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
