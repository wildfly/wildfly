/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.management.spi;

/**
 * PathAddress is an ordered set of name/value pairs representing the management path.
 *
 * The names are based on the statistic child names (e.g. name=entity value=Employee)
 *
 * @author Scott Marlow
 */
public interface PathAddress {

    int size();

    String getValue(String name);
    String getValue(int index);
}
