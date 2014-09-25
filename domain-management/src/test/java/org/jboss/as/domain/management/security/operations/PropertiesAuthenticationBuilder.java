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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;

import org.jboss.dmr.ModelNode;

/**
 * A builder for defining authentication backed by a properties file.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesAuthenticationBuilder extends Builder<AuthenticationBuilder> {

    private final AuthenticationBuilder parent;
    private boolean built = false;

    private boolean plainText = false;
    private String path;
    private String relativeTo;

    PropertiesAuthenticationBuilder(final AuthenticationBuilder parent) {
        this.parent = parent;
    }

    public PropertiesAuthenticationBuilder setPlainText(final boolean plainText) {
        assertNotBuilt();
        this.plainText = plainText;

        return this;
    }

    public PropertiesAuthenticationBuilder setPath(final String path) {
        assertNotBuilt();
        this.path = path;

        return this;
    }

    public PropertiesAuthenticationBuilder setRelativeTo(final String relativeTo) {
        assertNotBuilt();
        this.relativeTo = relativeTo;

        return this;
    }

    public AuthenticationBuilder build() {
        assertNotBuilt();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(parent.getRealmAddress().add(AUTHENTICATION, PROPERTIES));

        if (plainText) {
            add.get(PLAIN_TEXT).set(true);
        }
        if (path != null) {
            add.get(PATH).set(path);
        }
        if (relativeTo != null) {
            add.get(RELATIVE_TO).set(relativeTo);
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
