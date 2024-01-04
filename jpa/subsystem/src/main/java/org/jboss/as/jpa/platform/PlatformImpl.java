/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.plugin.spi.Platform;

/**
 * represents the EE container platform that we are running in and its capabilities (e.g. default 2lc which
 * currently can be INFINISPAN or NONE)
 *
 * @author Scott Marlow
 */
public class PlatformImpl implements Platform {


    private final Classification defaultCacheClassification;
    private final Set<Classification> cacheClassfications;

    public PlatformImpl(Classification defaultCacheClassification, Classification... supportedClassifications) {
        this.defaultCacheClassification = defaultCacheClassification;
        ArrayList<Classification> includedClassifications = new ArrayList<>();
        for(Classification eachClassification:supportedClassifications) {
            includedClassifications.add(eachClassification);
        }

        this.cacheClassfications = !includedClassifications.isEmpty() ?
                EnumSet.copyOf(includedClassifications):
                Collections.<Classification>emptySet();
    }

    @Override
    public Classification defaultCacheClassification() {
        return defaultCacheClassification;
    }

    @Override
    public Set<Classification> cacheClassifications() {
        return cacheClassfications;
    }
}
