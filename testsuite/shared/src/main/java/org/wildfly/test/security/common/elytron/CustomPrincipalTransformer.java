/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import static org.jboss.as.controller.PathElement.pathElement;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.ModelNodeUtil;

/**
 * A {@link ConfigurableElement} to define a custom principal transformer. Requires a {@link Module} to be
 * added to the server.
 *
 * @author <a href="mailto:jrodri@redhat.com">Jessica Rodriguez</a>
 */
public class CustomPrincipalTransformer implements ConfigurableElement {

    private final PathAddress address;
    private final String name;
    private final String className;
    private final String module;
    private final Map<String, String> configuration;

    protected CustomPrincipalTransformer(String name, String className, String module, Map<String, String> configuration) {
        this.name = name;
        this.className = className;
        this.module = module;
        this.configuration = configuration;

        this.address = PathAddress.pathAddress(
                pathElement("subsystem", "elytron"),
                pathElement("custom-principal-transformer", name));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode addOperation = Util.createAddOperation(address);
        ModelNodeUtil.setIfNotNull(addOperation, "class-name", className);
        ModelNodeUtil.setIfNotNull(addOperation, "module", module);

        // Add optional key-value pairs as configuration object
        if(configuration.size() > 0) {
            StringBuilder configurationPairs = new StringBuilder();
            configuration.forEach((key, value) -> configurationPairs.append(',').append(key).append('=').append(value));
            configurationPairs.replace(0, 1, "{").append('}');
            addOperation.get("configuration").set(configurationPairs.toString());
        }
        Utils.applyUpdate(addOperation, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(address), client);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder to register a custom principal transformer with Elytron. */
    public static class Builder {

        private String name;
        private String className;
        private String module;
        private Map<String,String> configuration = new HashMap<>();

        private Builder() {}

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withClassName(final String className) {
            this.className = className;
            return this;
        }

        public Builder withModule(String module) {
            this.module = module;
            return this;
        }

        public Builder addConfiguration(String key, String value) {
            configuration.put(key, value);
            return this;
        }

        public Builder addConfigurations(Map<String, String> configuration) {
            this.configuration.putAll(configuration);
            return this;
        }

        public CustomPrincipalTransformer build() {
            return new CustomPrincipalTransformer(name, className, module, configuration);
        }
    }
}
