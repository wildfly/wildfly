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

import org.jboss.dmr.ModelNode;

/**
 * A builder for adding authorization configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthorizationBuilder {

    private final SecurityRealmAddBuilder parent;
    private boolean built = false;

    private AuthorizationBuilderChild childBuilder;
    private ModelNode childStep;

    AuthorizationBuilder(final SecurityRealmAddBuilder parent) {
        this.parent = parent;
    }

    public LdapAuthorizationBuilder ldap() {
        LdapAuthorizationBuilder lab = null;
        if (childBuilder == null) {
            childBuilder = lab = new LdapAuthorizationBuilder(this);
        } else if (childBuilder instanceof LdapAuthorizationBuilder) {
            lab = (LdapAuthorizationBuilder) childBuilder;
        } else {
            throw new IllegalStateException("An alternative authorization child configuration has already been set.");
        }
        lab.assertNotBuilt();

        return lab;
    }

    ModelNode getRealmAddress() {
        return parent.getRealmAddress();
    }

    AuthorizationBuilder setChildStep(final ModelNode step) {
        assertNotBuilt();

        childStep = step;
        return this;
    }


    public SecurityRealmAddBuilder build() {
        assertNotBuilt();
        buildChildren();
        built = true;

        if (childStep != null) {
            parent.addStep(childStep);
        }

        return parent;
    }

    private void buildChildren() {
        if (childBuilder != null && childBuilder.isBuilt() == false) {
            childBuilder.build();
        }
    }

    boolean isBuilt() {
        return built;
    }

    void assertNotBuilt() {
        parent.assertNotBuilt();
        if (built) {
            throw new IllegalStateException("Already built.");
        }
    }



}
