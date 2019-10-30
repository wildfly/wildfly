/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
