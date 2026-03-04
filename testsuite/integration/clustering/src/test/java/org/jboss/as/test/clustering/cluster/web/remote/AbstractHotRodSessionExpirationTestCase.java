/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.infinispan.transaction.TransactionMode;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.expiration.SessionExpirationTestCase;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodSessionExpirationTestCase extends SessionExpirationTestCase {

    @RegisterExtension
    public static final InfinispanServerExtension SERVER = InfinispanServerUtil.infinispanServerExtension();

    public AbstractHotRodSessionExpirationTestCase() {
        super(TransactionMode.NON_TRANSACTIONAL);
    }
}
