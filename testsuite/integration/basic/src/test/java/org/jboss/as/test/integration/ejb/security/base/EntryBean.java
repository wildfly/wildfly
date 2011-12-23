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
package org.jboss.as.test.integration.ejb.security.base;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import static org.jboss.as.test.shared.integration.ejb.security.Util.getCLMLoginContext;

import org.jboss.as.test.integration.ejb.security.WhoAmI;

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
        System.out.println("RemoteWho = " + remoteWho);
        String secondLocalWho = context.getCallerPrincipal().getName();
        if (secondLocalWho.equals(localWho) == false) {
            throw new IllegalStateException("Local getCallerPrincipal changed from '" + localWho + "' to '" + secondLocalWho);
        }

        return new String[]{localWho, remoteWho};
    }

    public String[] doubleWhoAmI(String username, String password) throws LoginException {
        String localWho = context.getCallerPrincipal().getName();

        LoginContext lc = getCLMLoginContext(username, password);
        lc.login();
        try {
            String remoteWho = whoAmIBean.getCallerPrincipal().getName();
            return new String[]{localWho, remoteWho};
        } finally {
            lc.logout();
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
        LoginContext lc = getCLMLoginContext(username, password);
        lc.login();
        try {
            boolean remoteDoI = whoAmIBean.doIHaveRole(roleName);
            return new boolean[]{localDoI, remoteDoI};
        } finally {
            lc.logout();
            boolean secondLocalDoI = context.isCallerInRole(roleName);
            if (secondLocalDoI != localDoI) {
                throw new IllegalStateException("Local call to isCallerInRole for '" + roleName + "' changed from " + localDoI + " to " + secondLocalDoI);
            }
        }
    }

}
