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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORCE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * A builder for creating the username-filter add op.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UsernameFilter extends ParentBuilder<LdapAuthorizationBuilder> {

    private final LdapAuthorizationBuilder parent;
    private final ModelNode address;
    private boolean built = false;

    private boolean force = false;
    private String attribute;
    private String baseDn;
    private boolean recursive;

    private CacheBuilder<UsernameFilter> cacheBuilder = null;
    private final List<ModelNode> additionalSteps = new ArrayList<ModelNode>();

    UsernameFilter(LdapAuthorizationBuilder parent) {
        this.parent = parent;
        address = parent.getLdapAuthorizationAddress().add(USERNAME_TO_DN, USERNAME_FILTER);
    }

    public UsernameFilter setForce(final boolean force) {
        assertNotBuilt();
        this.force = force;

        return this;
    }

    public UsernameFilter setAttribute(final String attribute) {
        assertNotBuilt();
        this.attribute = attribute;

        return this;
    }

    public UsernameFilter setBaseDn(final String baseDn) {
        assertNotBuilt();
        this.baseDn = baseDn;

        return this;
    }

    public UsernameFilter setRecursive(final boolean recursive) {
        assertNotBuilt();
        this.recursive = recursive;

        return this;
    }

    public CacheBuilder<UsernameFilter> cache() {
        assertNotBuilt();
        if (cacheBuilder == null) {
            cacheBuilder = new CacheBuilder<UsernameFilter>(this, address.clone());
        }

        return cacheBuilder;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    @Override
    public LdapAuthorizationBuilder build() {
        assertNotBuilt();
        if (cacheBuilder != null && cacheBuilder.isBuilt() == false) {
            cacheBuilder.build();
        }
        built=true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(address);

        if (force) {
            add.get(FORCE).set(true);
        }
        if (recursive) {
            add.get(RECURSIVE).set(true);
        }
        if (attribute != null) {
            add.get(ATTRIBUTE).set(attribute);
        }
        if (baseDn != null) {
            add.get(BASE_DN).set(baseDn);
        }

        parent.addStep(add);
        for (ModelNode current : additionalSteps) {
            parent.addStep(current);
        }

        return parent;
    }

    void assertNotBuilt() {
        parent.assertNotBuilt();
        if (built) {
            throw new IllegalStateException("Already built.");
        }
    }

    @Override
    void addStep(ModelNode step) {
        assertNotBuilt();
        additionalSteps.add(step);
    }

}
