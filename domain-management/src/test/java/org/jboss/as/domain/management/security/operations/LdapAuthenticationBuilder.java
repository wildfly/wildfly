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
package org.jboss.as.domain.management.security.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_EMPTY_PASSWORDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER_DN;

import org.jboss.dmr.ModelNode;

/**
 * A builder for defining authentication backed by ldap.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthenticationBuilder implements AuthenticationBuilderChild {

    private final AuthenticationBuilder parent;
    private boolean built = false;

    private String connection;
    private String baseDn;
    private boolean recursive = false;
    private String userDn;
    private boolean allowEmptyPasswords = false;
    private String usernameFilter;
    private String advancedFilter;

    LdapAuthenticationBuilder(final AuthenticationBuilder parent) {
        this.parent = parent;
    }

    public LdapAuthenticationBuilder setConnection(final String connection) {
        assertNotBuilt();
        this.connection = connection;

        return this;
    }

    public LdapAuthenticationBuilder setBaseDn(final String baseDn) {
        assertNotBuilt();
        this.baseDn = baseDn;

        return this;
    }

    public LdapAuthenticationBuilder setRecursive(final boolean recursive) {
        assertNotBuilt();
        this.recursive = recursive;

        return this;
    }

    public LdapAuthenticationBuilder setUserDn(final String userDn) {
        assertNotBuilt();
        this.userDn = userDn;

        return this;
    }

    public LdapAuthenticationBuilder setAllowEmptyPasswords(final boolean allowEmptyPasswords) {
        assertNotBuilt();
        this.allowEmptyPasswords = allowEmptyPasswords;

        return this;
    }

    public LdapAuthenticationBuilder setUsernameFilter(final String usernameFilter) {
        assertNotBuilt();
        if (advancedFilter != null) {
            throw new IllegalStateException("Can not set a username filter once an advanced filter is set.");
        }
        this.usernameFilter = usernameFilter;

        return this;
    }

    public LdapAuthenticationBuilder setAdvancedFilter(final String advancedFilter) {
        assertNotBuilt();
        if (usernameFilter != null) {
            throw new IllegalStateException("Can not set an advanced filter once a user filter is set.");
        }
        this.advancedFilter = advancedFilter;

        return this;
    }

    public AuthenticationBuilder build() {
        assertNotBuilt();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(parent.getRealmAddress().add(AUTHENTICATION, LDAP));

        if (connection != null) {
            add.get(CONNECTION).set(connection);
        }
        if (baseDn != null) {
            add.get(BASE_DN).set(baseDn);
        }
        if (userDn != null) {
            add.get(USER_DN).set(userDn);
        }
        if (recursive) {
            add.get(RECURSIVE).set(true);
        }
        if (allowEmptyPasswords) {
            add.get(ALLOW_EMPTY_PASSWORDS).set(true);
        }
        if (usernameFilter != null) {
            add.get(USERNAME_ATTRIBUTE).set(usernameFilter);
        }
        if (advancedFilter != null) {
            add.get(ADVANCED_FILTER).set(advancedFilter);
        }

        parent.addStep(add);

        return parent;
    }

    public boolean isBuilt() {
        return built;
    }

    void assertNotBuilt() {
        parent.assertNotBuilt();
        if (built) {
            throw new IllegalStateException("Already built.");
        }
    }

}
