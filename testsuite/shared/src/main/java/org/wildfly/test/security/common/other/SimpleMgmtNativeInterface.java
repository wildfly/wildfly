/*
 * Copyright 2017 Red Hat, Inc.
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

package org.wildfly.test.security.common.other;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * Configuration for /core-service=management/management-interface=native-interface.
 *
 * @author Josef Cacek
 */
public class SimpleMgmtNativeInterface implements ConfigurableElement {

    private static final PathAddress PATH_NATIVE_INTERFACE = PathAddress.pathAddress().append("core-service", "management").append("management-interface", "native-interface");

    private final String saslAuthenticationFactory;
    private final String socketBinding;
    private final String sslContext;

    private SimpleMgmtNativeInterface(Builder builder) {
        this.saslAuthenticationFactory = builder.saslAuthenticationFactory;
        this.socketBinding = builder.socketBinding;
        this.sslContext = builder.sslContext;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util
                .createAddOperation(PATH_NATIVE_INTERFACE);
        setIfNotNull(op, "sasl-authentication-factory", saslAuthenticationFactory);
        setIfNotNull(op, "socket-binding", socketBinding);
        setIfNotNull(op, "ssl-context", sslContext);

        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(PATH_NATIVE_INTERFACE), client);
    }

    @Override
    public String getName() {
        return "management-interface=native-interface";
    }

    /**
     * Creates builder to build {@link SimpleMgmtNativeInterface}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder to build {@link SimpleMgmtNativeInterface}.
     */
    public static final class Builder {
        private String saslAuthenticationFactory;
        private String socketBinding;
        private String sslContext;

        private Builder() {
        }

        public Builder withSaslAuthenticationFactory(String saslAuthenticationFactory) {
            this.saslAuthenticationFactory = saslAuthenticationFactory;
            return this;
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder withSocketBinding(String socketBinding) {
            this.socketBinding = socketBinding;
            return this;
        }

        public SimpleMgmtNativeInterface build() {
            return new SimpleMgmtNativeInterface(this);
        }
    }


}
