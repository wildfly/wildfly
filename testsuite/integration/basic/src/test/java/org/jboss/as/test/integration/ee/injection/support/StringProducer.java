/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support;

import jakarta.enterprise.inject.Produces;

public class StringProducer {

    @Produces
    @ProducedString
    public String name() {
        return "Joe";
    }
}
