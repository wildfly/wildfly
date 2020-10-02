/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.security.common.elytron;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define the failover-realm resource within the Elytron subsystem.
 *
 * @author Ondrej Kotek
 */
public class FailoverRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final String delegateRealm;
    private final String failoverRealm;
    private final Boolean emitEvents;

    FailoverRealm(final String name, final Builder builder) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("failover-realm", name));
        this.delegateRealm = builder.delegateRealm;
        this.failoverRealm = builder.failoverRealm;
        this.emitEvents = builder.emitEvents;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        if (this.delegateRealm != null) {
            addOperation.get("delegate-realm").set(this.delegateRealm);
        }
        if (this.failoverRealm != null) {
            addOperation.get("failover-realm").set(this.failoverRealm);
        }
        if (this.emitEvents != null) {
            addOperation.get("emit-events").set(this.emitEvents);
        }

        return addOperation;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private String delegateRealm;
        private String failoverRealm;
        private Boolean emitEvents;

        Builder(final String name) {
            this.name = name;
        }

        public Builder withDelegateRealm(final String realm) {
            this.delegateRealm = realm;

            return this;
        }

        public Builder withFailoverRealm(final String realm) {
            this.failoverRealm = realm;

            return this;
        }

        public Builder withEmitEvents(final Boolean emitEvents) {
            this.emitEvents = emitEvents;

            return this;
        }

        public SecurityRealm build() {
            return new FailoverRealm(name, this);
        }

    }

}
