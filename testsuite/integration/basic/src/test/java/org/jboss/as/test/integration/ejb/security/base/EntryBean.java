/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.base;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;

import java.util.concurrent.Callable;

import org.jboss.as.test.integration.ejb.security.WhoAmI;
import org.jboss.as.test.shared.integration.ejb.security.Util;

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

        return new String[]{localWho, remoteWho};
    }

    public String[] doubleWhoAmI(String username, String password) throws Exception {
        String localWho = context.getCallerPrincipal().getName();

        final Callable<String[]> callable = () -> {
            String remoteWho = whoAmIBean.getCallerPrincipal().getName();
            return new String[]{localWho, remoteWho};
        };
        try {
            return Util.switchIdentity(username, password, callable);
        } finally {
            String secondLocalWho = context.getCallerPrincipal().getName();
            if (secondLocalWho.equals(localWho) == false) {
                throw new IllegalStateException("Local getCallerPrincipal changed from '" + localWho + "' to '" + secondLocalWho);
            }
        }
    }

    public boolean doIHaveRole(String roleName) {
        return context.isCallerInRole(roleName);
    }

    public boolean[] doubleDoIHaveRole(String roleName) {
        boolean localDoI = context.isCallerInRole(roleName);
        boolean remoteDoI = whoAmIBean.doIHaveRole(roleName);

        return new boolean[]{localDoI, remoteDoI};
    }

    public boolean[] doubleDoIHaveRole(String roleName, String username, String password) throws Exception {
        boolean localDoI = context.isCallerInRole(roleName);
        final Callable<boolean[]> callable = () -> {
            boolean remoteDoI = whoAmIBean.doIHaveRole(roleName);
            return new boolean[]{localDoI, remoteDoI};
        };
        try {
            return Util.switchIdentity(username, password, callable);
        } finally {
            boolean secondLocalDoI = context.isCallerInRole(roleName);
            if (secondLocalDoI != localDoI) {
                throw new IllegalStateException("Local call to isCallerInRole for '" + roleName + "' changed from " + localDoI + " to " + secondLocalDoI);
            }
        }
    }

}
