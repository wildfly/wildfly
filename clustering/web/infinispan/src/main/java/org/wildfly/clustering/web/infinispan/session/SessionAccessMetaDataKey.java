/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for the session access meta data entry.
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataKey extends GroupedKey<String> {

    public SessionAccessMetaDataKey(String id) {
        super(id);
    }
}
