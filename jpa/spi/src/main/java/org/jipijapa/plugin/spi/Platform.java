/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.util.Set;

import org.jipijapa.cache.spi.Classification;

/**
 * describes the platform that is in use
 *
 * @author Scott Marlow
 */
public interface Platform {

    /**
     * obtain the default second level cache classification
     * @return default 2lc type
     */
    Classification defaultCacheClassification();

    /**
     * get the second level cache classifications
     * @return Set<Classification>
     */
    Set<Classification> cacheClassifications();
}
