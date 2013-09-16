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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUPS_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATTERN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT_PATTERN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REVERSE_GROUP;

import org.jboss.dmr.ModelNode;

/**
 * A builder for defining group loading from LDAP.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthorizationBuilder implements AuthorizationBuilderChild {

    private final AuthorizationBuilder parent;
    private boolean built = false;

    private String connection;
    private String baseDN;
    private boolean recursive;
    private String usernameAttribute;
    private String groupsDN;
    private String pattern;
    private String resultPattern;
    private int group = -1;
    private boolean reverseGroup;

    LdapAuthorizationBuilder(final AuthorizationBuilder parent) {
        this.parent = parent;
    }

    public LdapAuthorizationBuilder setConnection(final String connection) {
        assertNotBuilt();
        this.connection = connection;

        return this;
    }

    public LdapAuthorizationBuilder setBaseDn(final String baseDN) {
        assertNotBuilt();
        this.baseDN = baseDN;

        return this;
    }

    public LdapAuthorizationBuilder setRecursive(final boolean recursive) {
        assertNotBuilt();
        this.recursive = recursive;

        return this;
    }

    public LdapAuthorizationBuilder setUsernameAttribute(final String usernameAttribute) {
        assertNotBuilt();
        this.usernameAttribute = usernameAttribute;

        return this;
    }

    public LdapAuthorizationBuilder setGroupsDn(final String groupsDN) {
        assertNotBuilt();
        this.groupsDN = groupsDN;

        return this;
    }

    public LdapAuthorizationBuilder setPattern(final String pattern) {
        assertNotBuilt();
        this.pattern = pattern;

        return this;
    }

    public LdapAuthorizationBuilder setResultPattern(final String resultPattern) {
        assertNotBuilt();
        this.resultPattern = resultPattern;

        return this;
    }

    public LdapAuthorizationBuilder setGroup(final int group) {
        assertNotBuilt();
        this.group = group;

        return this;
    }

    public LdapAuthorizationBuilder setReverseGroup(final boolean reverseGroup) {
        assertNotBuilt();
        this.reverseGroup = reverseGroup;

        return this;
    }

    @Override
    public AuthorizationBuilder build() {
        assertNotBuilt();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(parent.getRealmAddress().add(AUTHORIZATION, LDAP));

        if (connection != null) {
            add.get(CONNECTION).set(connection);
        }
        if (baseDN != null) {
            add.get(BASE_DN).set(baseDN);
        }
        if (recursive) {
            add.get(RECURSIVE).set(true);
        }
        if (usernameAttribute != null) {
            add.get(USERNAME_ATTRIBUTE).set(usernameAttribute);
        }
        if (groupsDN != null) {
            add.get(GROUPS_DN).set(groupsDN);
        }
        if (pattern != null) {
            add.get(PATTERN).set(pattern);
        }
        if (resultPattern != null) {
            add.get(RESULT_PATTERN).set(resultPattern);
        }
        if (group > 0) {
            add.get(GROUP).set(group);
        }
        if (reverseGroup) {
            add.get(REVERSE_GROUP).set(true);
        }

        parent.setChildStep(add);

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
