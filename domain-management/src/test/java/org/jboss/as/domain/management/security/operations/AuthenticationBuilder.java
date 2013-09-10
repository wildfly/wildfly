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

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * A builder for adding authentication configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthenticationBuilder {

    private final SecurityRealmAddBuilder parent;
    private boolean built = false;

    private final List<ModelNode> additionalSteps = new ArrayList<ModelNode>();

    private AuthenticationBuilderChild usernamePasswordChild;

    AuthenticationBuilder(final SecurityRealmAddBuilder parent) {
        this.parent = parent;
    }

    public PropertiesAuthenticationBuilder property() {
        PropertiesAuthenticationBuilder pab = null;
        if (usernamePasswordChild == null) {
            usernamePasswordChild = pab = new PropertiesAuthenticationBuilder(this);
        } else if (usernamePasswordChild instanceof PropertiesAuthenticationBuilder) {
            pab = (PropertiesAuthenticationBuilder) usernamePasswordChild;
        } else {
            throw new IllegalStateException(
                    "An alternative username/password authentication configuration has already been set.");
        }
        pab.assertNotBuilt();

        return pab;
    }

    public LdapAuthenticationBuilder ldap() {
        LdapAuthenticationBuilder lab = null;
        if (usernamePasswordChild == null) {
            usernamePasswordChild = lab = new LdapAuthenticationBuilder(this);
        } else if (usernamePasswordChild instanceof LdapAuthenticationBuilder) {
            lab = (LdapAuthenticationBuilder) usernamePasswordChild;
        } else {
            throw new IllegalStateException(
                    "An alternative username/password authentication configuration has already been set.");
        }
        lab.assertNotBuilt();

        return lab;
    }

    public SecurityRealmAddBuilder build() {
        assertNotBuilt();
        buildChildren();
        built = true;

        for (ModelNode step : additionalSteps) {
            parent.addStep(step);
        }

        return parent;
    }

    private void buildChildren() {
        if (usernamePasswordChild != null && usernamePasswordChild.isBuilt() == false) {
            usernamePasswordChild.build();
        }
    }

    ModelNode getRealmAddress() {
        return parent.getRealmAddress();
    }

    AuthenticationBuilder addStep(final ModelNode step) {
        assertNotBuilt();
        additionalSteps.add(step);
        return this;
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
