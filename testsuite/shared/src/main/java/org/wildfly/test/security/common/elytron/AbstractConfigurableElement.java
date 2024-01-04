/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.Objects;

/**
 * Abstract parent for {@link ConfigurableElement} implementations. It just holds common fields and provides parent for
 * builders.
 *
 * @author Josef Cacek
 */
public abstract class AbstractConfigurableElement implements ConfigurableElement {

    protected final String name;

    protected AbstractConfigurableElement(Builder<?> builder) {
        this.name = Objects.requireNonNull(builder.name, "Configuration name must not be null");
    }

    @Override
    public final String getName() {
        return name;
    }

    /**
     * Builder to build {@link AbstractConfigurableElement}.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private String name;

        protected Builder() {
        }

        protected abstract T self();

        public final T withName(String name) {
            this.name = name;
            return self();
        }

    }

}
