/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.http.server.security;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * An implementation of {@link SubjectAccount} for use by classes in this package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class RealmIdentityAccount implements SubjectAccount {

    private final Subject subject;
    private final Principal principal;

    RealmIdentityAccount(final Subject subject, final Principal principal) {
        this.subject = subject;
        this.principal = principal;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return Collections.emptySet();
    }

    public Subject getSubject() {
        return subject;
    }

}
