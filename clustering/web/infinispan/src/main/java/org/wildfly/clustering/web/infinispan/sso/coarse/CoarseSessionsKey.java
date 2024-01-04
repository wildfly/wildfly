/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso.coarse;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionsKey extends GroupedKey<String> {

    public CoarseSessionsKey(String id) {
        super(id);
    }
}
