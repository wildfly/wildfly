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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORCE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_IS_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;

import org.jboss.dmr.ModelNode;

/**
 * A builder for creating the username-is-dn add op.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UsernameIsDnAddBuilder implements LdapAuthorizationBuilderChild {

    private final LdapAuthorizationBuilder parent;
    private boolean built = false;

    private boolean force = false;

    UsernameIsDnAddBuilder(LdapAuthorizationBuilder parent) {
        this.parent = parent;
    }

    public UsernameIsDnAddBuilder setForce(final boolean force) {
        assertNotBuilt();
        this.force = force;

        return this;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    @Override
    public LdapAuthorizationBuilder build() {
        assertNotBuilt();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(parent.getLdapAuthorizationAddress().add(USERNAME_TO_DN, USERNAME_IS_DN));

        if (force) {
            add.get(FORCE).set(true);
        }

        return parent.addStep(add);
    }

    void assertNotBuilt() {
        parent.assertNotBuilt();
        if (built) {
            throw new IllegalStateException("Already built.");
        }
    }

}
