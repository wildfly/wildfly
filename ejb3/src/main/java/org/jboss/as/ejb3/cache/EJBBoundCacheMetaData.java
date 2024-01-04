/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.cache;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata represents the pool name configured for Jakarta Enterprise Beans via the jboss-ejb3.xml deployment descriptor
 *
 * @author Jaikiran Pai
 */
public class EJBBoundCacheMetaData extends AbstractEJBBoundMetaData {
    private static final long serialVersionUID = -3246398329247802494L;

    private String cacheName;

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(final String cacheName) {
        this.cacheName = cacheName;
    }
}
