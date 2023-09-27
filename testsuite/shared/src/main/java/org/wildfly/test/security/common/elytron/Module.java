/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * A {@link ConfigurableElement} to add a custom module.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Module extends AbstractConfigurableElement {

    private final List<String> resources;
    private final List<String> dependencies;

    Module(Builder builder) {
        super(builder);
        this.resources = builder.resources;
        this.dependencies = builder.dependencies;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        StringBuilder moduleAddCommand = new StringBuilder("module add --name=")
                .append(name);

        if (resources.size() > 0) {
            moduleAddCommand.append(" --resources=");
            Iterator<String> resourcesIterator = resources.iterator();
            moduleAddCommand.append(resourcesIterator.next());
            while (resourcesIterator.hasNext()) {
                moduleAddCommand.append(",").append(resourcesIterator.next());
            }
        }

        if (dependencies.size() > 0) {
            moduleAddCommand.append(" --dependencies=");
            Iterator<String> dependenciesIterator = dependencies.iterator();
            moduleAddCommand.append(dependenciesIterator.next());
            while (dependenciesIterator.hasNext()) {
                moduleAddCommand.append(",").append(dependenciesIterator.next());
            }
        }

        cli.sendLine(moduleAddCommand.toString(), true);
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        String moduleRemove = "module remove --name=" + name;

        cli.sendLine(moduleRemove, true);
    }


    /**
     * Create a new Builder instance.
     *
     * @return a new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build a module registration.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private List<String> resources = new ArrayList<>();
        private List<String> dependencies = new ArrayList<>();

        /**
         * Add a resource to be referenced by the module.
         *
         * @param resource a resource to be referenced by the module.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withResource(final String resource) {
            resources.add(resource);

            return this;
        }

        /**
         * Add a dependency to be referenced by the module.
         *
         * @param dependency a dependency to be referenced by the module.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withDependency(final String dependency) {
            dependencies.add(dependency);

            return this;
        }

        /**
         * Build an instance of {@link Module}
         *
         * @return an instance of {@link Module}
         */
        public Module build() {
            return new Module(this);
        }

        @Override
        protected Builder self() {
            return this;
        }

    }
}
