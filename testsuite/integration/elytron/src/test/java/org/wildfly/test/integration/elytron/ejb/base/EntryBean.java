/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.ejb.base;

import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;

import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.test.integration.elytron.ejb.WhoAmI;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class EntryBean {

    @EJB
    private WhoAmI whoAmIBean;

    @Resource
    private SessionContext context;

    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    public String[] doubleWhoAmI() {
        String localWho = context.getCallerPrincipal().getName();
        String remoteWho = whoAmIBean.getCallerPrincipal().getName();
        String secondLocalWho = context.getCallerPrincipal().getName();
        if (secondLocalWho.equals(localWho) == false) {
            throw new IllegalStateException("Local getCallerPrincipal changed from '" + localWho + "' to '" + secondLocalWho);
        }

        return new String[] { localWho, remoteWho };
    }

    public String[] doubleWhoAmI(String username, String password) throws Exception {
        String localWho = context.getCallerPrincipal().getName();

        final Callable<String[]> callable = () -> {
            String remoteWho = whoAmIBean.getCallerPrincipal().getName();
            return new String[] { localWho, remoteWho };
        };
        try {
            return switchIdentity(username, password, callable);
        } finally {
            String secondLocalWho = context.getCallerPrincipal().getName();
            if (secondLocalWho.equals(localWho) == false) {
                throw new IllegalStateException(
                        "Local getCallerPrincipal changed from '" + localWho + "' to '" + secondLocalWho);
            }
        }
    }

    public boolean doIHaveRole(String roleName) {
        return context.isCallerInRole(roleName);
    }

    public boolean[] doubleDoIHaveRole(String roleName) {
        boolean localDoI = context.isCallerInRole(roleName);
        boolean remoteDoI = whoAmIBean.doIHaveRole(roleName);

        return new boolean[] { localDoI, remoteDoI };
    }

    public boolean[] doubleDoIHaveRole(String roleName, String username, String password) throws Exception {
        boolean localDoI = context.isCallerInRole(roleName);
        final Callable<boolean[]> callable = () -> {
            boolean remoteDoI = whoAmIBean.doIHaveRole(roleName);
            return new boolean[] { localDoI, remoteDoI };
        };
        try {
            return switchIdentity(username, password, callable);
        } finally {
            boolean secondLocalDoI = context.isCallerInRole(roleName);
            if (secondLocalDoI != localDoI) {
                throw new IllegalStateException("Local call to isCallerInRole for '" + roleName + "' changed from " + localDoI
                        + " to " + secondLocalDoI);
            }
        }
    }

    private static <T> T switchIdentity(final String username, final String password, final Callable<T> callable)
            throws RealmUnavailableException, Exception {
        return SecurityDomain.getCurrent().authenticate(username, new PasswordGuessEvidence(password.toCharArray()))
                .runAs(callable);
    }

}
