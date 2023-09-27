/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso.coarse;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionsKey extends RemoteCacheKey<String> {

    public CoarseSessionsKey(String id) {
        super(id);
    }
}
