/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.beanvalidation;

/**
 * Provides a minimum value for the number of people for a Reservation instance.
 *
 * @author Farah Juma
 */
public class MinimumValueProvider {

    public int getMin() {
        return 5;
    }
}
