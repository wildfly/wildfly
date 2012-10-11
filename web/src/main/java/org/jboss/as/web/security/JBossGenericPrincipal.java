/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security;

import java.security.Principal;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.security.CacheableManager;
import org.jboss.security.SimplePrincipal;

/**
 *
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JBossGenericPrincipal extends GenericPrincipal {

    private CacheableManager<?, Principal> cm;

    private Subject subject;

    private Object credentials;

    /** {@inheritDoc} */
    public JBossGenericPrincipal(Realm realm, String name, String password) {
        this(realm, name, password, null);
    }

    /** {@inheritDoc} */
    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles) {
        this(realm, name, password, roles, null);
    }

    /** {@inheritDoc} */
    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles, Principal userPrincipal) {
        this(realm, name, password, roles, userPrincipal, null);
    }

    /** {@inheritDoc} */
    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles, Principal userPrincipal,
            LoginContext loginContext) {
        this(realm, name, password, roles, userPrincipal, loginContext, null);
    }

    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles, Principal userPrincipal,
            LoginContext loginContext, Object credentials) {
        this(realm, name, password, roles, userPrincipal, loginContext, credentials, (CacheableManager<?, Principal>) null);
    }

    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles, Principal userPrincipal,
            LoginContext loginContext, Object credentials, CacheableManager<?, Principal> cm) {
        this(realm, name, password, roles, userPrincipal, loginContext, credentials, cm, null);
    }

    public JBossGenericPrincipal(Realm realm, String name, String password, List<String> roles, Principal userPrincipal,
            LoginContext loginContext, Object credentials, CacheableManager<?, Principal> cm, Subject subject) {
        super(realm, name, password, roles, userPrincipal, loginContext);
        this.credentials = credentials;
        this.cm = cm;
        this.subject = subject;
    }

    /**
     * Overridden so we can flush the authentication cache when a session is destroyed.
     */
    @Override
    public void logout() throws Exception {
        if (cm != null && super.name != null)
            cm.flushCache(new SimplePrincipal(super.name));
        super.logout();
    }

    public Subject getSubject() {
        return subject;
    }

    public Object getCredentials() {
        return credentials;
    }

}
