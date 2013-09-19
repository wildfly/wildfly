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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * A builder for defining group loading from LDAP.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthorizationBuilder implements AuthorizationBuilderChild {

    private final AuthorizationBuilder parent;
    private boolean built = false;

    private final List<ModelNode> additionalSteps = new ArrayList<ModelNode>();

    private LdapAuthorizationBuilderChild usernameFilterChild;
    private LdapAuthorizationBuilderChild groupFilterChild;

    private String connection;

    LdapAuthorizationBuilder(final AuthorizationBuilder parent) {
        this.parent = parent;
    }

    public LdapAuthorizationBuilder setConnection(final String connection) {
        assertNotBuilt();
        this.connection = connection;

        return this;
    }

    public UsernameIsDnAddBuilder usernameIsDN() {
        UsernameIsDnAddBuilder ub = null;
        if (usernameFilterChild == null) {
            usernameFilterChild = ub = new UsernameIsDnAddBuilder(this);
        } else if (usernameFilterChild instanceof UsernameIsDnAddBuilder) {
            ub = (UsernameIsDnAddBuilder) usernameFilterChild;
        } else {
            throw new IllegalStateException("An alternative username to dn configuration has already been set.");
        }
        ub.assertNotBuilt();

        return ub;
    }

    public UsernameFilter usernameFilter() {
        UsernameFilter uf = null;
        if (usernameFilterChild == null) {
            usernameFilterChild = uf = new UsernameFilter(this);
        } else if (usernameFilterChild instanceof UsernameFilter) {
            uf = (UsernameFilter) usernameFilterChild;
        } else {
            throw new IllegalStateException("An alternative username to dn configuration has already been set.");
        }
        uf.assertNotBuilt();

        return uf;
    }

    public AdvancedFilter advancedFilter() {
        AdvancedFilter af = null;
        if (usernameFilterChild == null) {
            usernameFilterChild = af = new AdvancedFilter(this);
        } else if (usernameFilterChild instanceof AdvancedFilter) {
            af = (AdvancedFilter) usernameFilterChild;
        } else {
            throw new IllegalStateException("An alternative username to dn configuration has already been set.");
        }
        af.assertNotBuilt();

        return af;
    }

    public GroupToPrincipalAddBuilder groupToPrincipal() {
        GroupToPrincipalAddBuilder gtp = null;
        if (groupFilterChild == null) {
            groupFilterChild = gtp = new GroupToPrincipalAddBuilder(this);
        } else if (groupFilterChild instanceof GroupToPrincipalAddBuilder) {
            gtp = (GroupToPrincipalAddBuilder) groupFilterChild;
        } else {
            throw new IllegalStateException("An alternative group search configuration has already been set.");
        }
        gtp.assertNotBuilt();

        return gtp;
    }

    public PrincipalToGroupBuilder principalToGroup() {
        PrincipalToGroupBuilder ptg = null;
        if (groupFilterChild == null) {
            groupFilterChild = ptg = new PrincipalToGroupBuilder(this);
        } else if (groupFilterChild instanceof PrincipalToGroupBuilder) {
            ptg = (PrincipalToGroupBuilder) groupFilterChild;
        } else {
            throw new IllegalStateException("An alternative group search configuration has already been set.");
        }
        ptg.assertNotBuilt();

        return ptg;
    }

    @Override
    public AuthorizationBuilder build() {
        assertNotBuilt();
        buildChildren();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(getLdapAuthorizationAddress());

        if (connection != null) {
            add.get(CONNECTION).set(connection);
        }

        parent.addStep(add);
        for (ModelNode step : additionalSteps) {
            parent.addStep(step);
        }

        return parent;
    }

    ModelNode getLdapAuthorizationAddress() {
        return parent.getRealmAddress().add(AUTHORIZATION, LDAP);
    }

    private void buildChildren() {
        if (usernameFilterChild != null && usernameFilterChild.isBuilt() == false) {
            usernameFilterChild.build();
        }
        if (groupFilterChild != null && groupFilterChild.isBuilt() == false) {
            groupFilterChild.build();
        }
    }

    public LdapAuthorizationBuilder addStep(final ModelNode step) {
        assertNotBuilt();
        additionalSteps.add(step);
        return this;
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
