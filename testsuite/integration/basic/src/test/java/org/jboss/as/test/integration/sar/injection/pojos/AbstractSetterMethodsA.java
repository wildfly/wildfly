/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.pojos;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
public abstract class AbstractSetterMethodsA extends AbstractLifycycleMethodsA {

    private int count;

    public final void setCount(final int count) {
        this.count = count;
    }

    public final int getCount() {
        return this.count;
    }

}
