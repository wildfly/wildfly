/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.management;

import org.jipijapa.management.spi.StatisticName;

/**
 * StatisticNameLookup
 *
 * @author Scott Marlow
 */
public class StatisticNameLookup implements StatisticName {

    private final String name;

    public StatisticNameLookup(String name) {
        this.name = name;
    }

    public static StatisticNameLookup statisticNameLookup(String name) {
        return new StatisticNameLookup(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
