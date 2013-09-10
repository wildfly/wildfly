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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_CREDENTIAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SEARCH_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import org.jboss.dmr.ModelNode;

/**
 * A builder to simplify the creation of operations to define new outbound LDAP connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class OutboundConnectionAddBuilder {

    private boolean built = false;
    private final ModelNode connectionAddress;
    private String url;
    private String searchDn;
    private String searchCredential;
    private String securityRealm;
    private String initialContextFactory;

    private OutboundConnectionAddBuilder(final String name) {
        connectionAddress = new ModelNode().add(CORE_SERVICE, MANAGEMENT).add(LDAP_CONNECTION, name);
    }

    public static OutboundConnectionAddBuilder builder(final String name) {
        return new OutboundConnectionAddBuilder(name);
    }

    public OutboundConnectionAddBuilder setUrl(final String url) {
        assertNotBuilt();
        this.url = url;

        return this;
    }

    public OutboundConnectionAddBuilder setSearchDn(final String searchDn) {
        assertNotBuilt();
        this.searchDn = searchDn;

        return this;
    }

    public OutboundConnectionAddBuilder setSearchCredential(final String searchCredential) {
        assertNotBuilt();
        this.searchCredential = searchCredential;

        return this;
    }

    public OutboundConnectionAddBuilder setSecurityRealm(final String securityRealm) {
        assertNotBuilt();
        this.securityRealm = securityRealm;

        return this;
    }

    public OutboundConnectionAddBuilder setInitialContextFactory(final String initialContextFactory) {
        assertNotBuilt();
        this.initialContextFactory = initialContextFactory;

        return this;
    }

    public ModelNode build() {
        assertNotBuilt();
        built = true;

        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(connectionAddress);
        add.get(OP).set(ADD);

        setNotNullParameter(add, URL, url);
        setNotNullParameter(add, SEARCH_DN, searchDn);
        setNotNullParameter(add, SEARCH_CREDENTIAL, searchCredential);
        setNotNullParameter(add, SECURITY_REALM, securityRealm);
        setNotNullParameter(add, INITIAL_CONTEXT_FACTORY, initialContextFactory);

        return add;
    }

    private void setNotNullParameter(final ModelNode addOp, final String parameterName, final String parameterValue) {
        if (parameterValue != null) {
            addOp.get(parameterName).set(parameterValue);
        }
    }

    void assertNotBuilt() {
        if (built) {
            throw new IllegalStateException("Alreadt built.");
        }
    }

}
