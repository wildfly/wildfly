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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_DN_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_NAME_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_SEARCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ITERATIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PREFER_ORIGINAL_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRINCIPAL_TO_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SKIP_MISSING_GROUPS;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.BaseLdapGroupSearchResource.GroupName;
import org.jboss.dmr.ModelNode;

/**
 * A builder for creating the principal-to-group add op.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PrincipalToGroupBuilder extends ParentBuilder<LdapAuthorizationBuilder> {

    private final LdapAuthorizationBuilder parent;
    private final ModelNode address;
    private boolean built = false;

    private String groupAttribute;
    private String groupDnAttribute;
    private GroupName groupName;
    private String groupNameAttribute;
    private boolean iterative;
    private boolean preferOriginalConnection;
    private boolean skipMissingGroups;

    private CacheBuilder<PrincipalToGroupBuilder> cacheBuilder = null;
    private final List<ModelNode> additionalSteps = new ArrayList<ModelNode>();

    PrincipalToGroupBuilder(LdapAuthorizationBuilder parent) {
        this.parent = parent;
        address = parent.getLdapAuthorizationAddress().add(GROUP_SEARCH, PRINCIPAL_TO_GROUP);
    }

    public PrincipalToGroupBuilder setGroupDnAttribute(final String groupDnAttribute) {
        assertNotBuilt();
        this.groupDnAttribute = groupDnAttribute;

        return this;
    }

    public PrincipalToGroupBuilder setGroupName(final GroupName groupName) {
        assertNotBuilt();
        this.groupName = groupName;

        return this;
    }

    public PrincipalToGroupBuilder setGroupNameAttribute(final String groupNameAttribute) {
        assertNotBuilt();
        this.groupNameAttribute = groupNameAttribute;

        return this;
    }

    public PrincipalToGroupBuilder setIterative(final boolean iterative) {
        assertNotBuilt();
        this.iterative = iterative;

        return this;
    }

    public PrincipalToGroupBuilder setGroupAttribute(final String groupAttribute) {
        assertNotBuilt();
        this.groupAttribute = groupAttribute;

        return this;
    }

    public PrincipalToGroupBuilder setPreferOriginalConnection(final boolean preferOriginalConnection) {
        assertNotBuilt();
        this.preferOriginalConnection = preferOriginalConnection;

        return this;
    }

    public PrincipalToGroupBuilder setSkipMissingGroups(final boolean skipMissingGroups) {
        assertNotBuilt();
        this.skipMissingGroups = skipMissingGroups;

        return this;
    }

    public CacheBuilder<PrincipalToGroupBuilder> cache() {
        assertNotBuilt();
        if (cacheBuilder == null) {
            cacheBuilder = new CacheBuilder<PrincipalToGroupBuilder>(this, address.clone());
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
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(address);

        if (groupDnAttribute != null) {
            add.get(GROUP_DN_ATTRIBUTE).set(groupDnAttribute);
        }
        if (groupName != null) {
            add.get(GROUP_NAME).set(groupName.toString());
        }
        if (groupNameAttribute != null) {
            add.get(GROUP_NAME_ATTRIBUTE).set(groupNameAttribute);
        }
        if (iterative) {
            add.get(ITERATIVE).set(true);
        }
        if (groupAttribute != null) {
            add.get(GROUP_ATTRIBUTE).set(groupAttribute);
        }
        if (preferOriginalConnection == false) {
            add.get(PREFER_ORIGINAL_CONNECTION).set(preferOriginalConnection);
        }
        if (skipMissingGroups == true) {
            add.get(SKIP_MISSING_GROUPS).set(skipMissingGroups);
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
