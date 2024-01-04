/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
