/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

        this.cacheClassfications = includedClassifications.size() > 0 ?
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
