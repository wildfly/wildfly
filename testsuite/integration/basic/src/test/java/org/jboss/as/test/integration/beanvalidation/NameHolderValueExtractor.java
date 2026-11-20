/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * A {@link ValueExtractor} for {@link NameHolder}, declared from the WAR's {@code validation.xml}.
 * <p>
 * Its only purpose in this test is to be a WAR-local class referenced by {@code validation.xml}: if
 * Bean Validation bootstraps with the wrong (EAR) class loader, as in WFLY-21093, this class is not
 * visible and {@code validation.xml} is silently ignored. Loading it successfully therefore confirms
 * that {@code validation.xml} was read with the WAR class loader.
 */
public class NameHolderValueExtractor implements ValueExtractor<NameHolder<@ExtractedValue ?>> {

    /**
     * Extracts the single held value from a {@link NameHolder}.
     *
     * @param originalValue the container instance (may be {@code null})
     * @param receiver      the receiver notified with the extracted value
     */
    @Override
    public void extractValues(NameHolder<?> originalValue, ValueReceiver receiver) {
        receiver.value(null, originalValue == null ? null : originalValue.getValue());
    }
}
