/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.beanvalidation.cdi;

/**
 * Provides a minimum value for the number of people for a Team instance.
 *
 * @author Farah Juma
 */
public class MinimumValueProvider {

    public int getMin() {
        return 3;
    }
}
