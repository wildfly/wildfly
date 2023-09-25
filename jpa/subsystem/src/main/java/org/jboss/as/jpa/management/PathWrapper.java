/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.management;

import org.jipijapa.management.spi.PathAddress;

/**
 * Path wrapper for a static String value
 *
 * @author Scott Marlow
 */
public class PathWrapper implements PathAddress {
    private final String value;

    public PathWrapper(String value) {
        this.value = value;
    }

    public static PathWrapper path(String value) {
        return new PathWrapper(value);
    }
    @Override
    public int size() {
        return 1;
    }

    @Override
    public String getValue(String name) {
        return value;
    }

    @Override
    public String getValue(int index) {
        return value;
    }
}
