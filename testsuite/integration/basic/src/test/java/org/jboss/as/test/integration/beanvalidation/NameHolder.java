/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

/**
 * A trivial single-value container type used only so that {@link NameHolderValueExtractor} has a
 * container to extract from. It exists to let {@code validation.xml} declare a WAR-local
 * {@code <value-extractor>}, mirroring the WFLY-21093 reproducer, so the test also covers loading a
 * {@code validation.xml}-referenced class with the correct class loader.
 *
 * @param <T> the held value type
 */
public class NameHolder<T> {

    private final T value;

    /**
     * Creates a holder around the given value.
     *
     * @param value the value to hold (may be {@code null})
     */
    public NameHolder(T value) {
        this.value = value;
    }

    /**
     * Returns the held value.
     *
     * @return the held value, possibly {@code null}
     */
    public T getValue() {
        return value;
    }
}
