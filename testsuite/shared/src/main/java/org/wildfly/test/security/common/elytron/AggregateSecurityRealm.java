/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * A {@link ConfigurableElement} to define an Aggregate SecurityRealm resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AggregateSecurityRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final String authenticationRealm;
    private final String authorizationRealm;
    private final String[] authorizationRealms;
    private final String principalTransformer;

    AggregateSecurityRealm(final String name, final String authenticationRealm, final String authorizationRealm, final String[] authorizationRealms, final String principalTransformer) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("aggregate-realm", name));
        this.authenticationRealm = authenticationRealm;
        this.authorizationRealm = authorizationRealm;
        this.authorizationRealms = authorizationRealms;
        this.principalTransformer = principalTransformer;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("authentication-realm").set(authenticationRealm);
        if (authorizationRealm != null) {
            addOperation.get("authorization-realm").set(authorizationRealm);
        }
        if (authorizationRealms != null) {
            ModelNode realms = addOperation.get("authorization-realms");
            for (String realmName : authorizationRealms) {
                realms.add(realmName);
            }
        }
        if (principalTransformer != null) {
            addOperation.get("principal-transformer").set(principalTransformer);
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
        private String authenticationRealm;
        private String authorizationRealm;
        private String[] authorizationRealms;
        private String principalTransformer;

        Builder(final String name) {
            this.name = name;
        }

        public Builder withAuthenticationRealm(final String authenticationRealm) {
            this.authenticationRealm = authenticationRealm;

            return this;
        }

        public Builder withAuthorizationRealm(final String authorizationRealm) {
            this.authorizationRealm = authorizationRealm;

            return this;
        }

        public Builder withAuthorizationRealms(final String... authorizationRealms) {
            this.authorizationRealms = authorizationRealms;

            return this;
        }

        public Builder withPrincipalTransformer(final String principalTransformer) {
            this.principalTransformer = principalTransformer;

            return this;
        }

        public SecurityRealm build() {
            return new AggregateSecurityRealm(name, authenticationRealm, authorizationRealm, authorizationRealms, principalTransformer);
        }

    }

}
