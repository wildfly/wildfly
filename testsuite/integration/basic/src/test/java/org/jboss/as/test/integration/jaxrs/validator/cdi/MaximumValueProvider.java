/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator.cdi;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A test Jakarta Contexts and Dependency Injection bean.
 *
 * @author Gunnar Morling
 */
@ApplicationScoped
public class MaximumValueProvider {

    public int getMax() {
        return 10;
    }
}
