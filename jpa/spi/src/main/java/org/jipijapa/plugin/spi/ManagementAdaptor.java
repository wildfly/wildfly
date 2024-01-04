/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import org.jipijapa.management.spi.Statistics;

/**
 * Defines persistence provider management operations/statistics
 *
 * @author Scott Marlow
 */
public interface ManagementAdaptor {


    /**
     * Get the short identification string that represents the management adaptor (e.g Hibernate)
     *
     * @return id label
     */
    String getIdentificationLabel();

    /**
     * Version that uniquely identifies the management adapter (can be used to tell the difference between
     * Hibernate 4.1 vs 4.3).
     *
     * @return version string
     */
    String getVersion();

    Statistics getStatistics();
}
