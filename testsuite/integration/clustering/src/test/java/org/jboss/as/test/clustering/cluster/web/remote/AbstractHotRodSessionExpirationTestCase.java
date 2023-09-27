/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.infinispan.transaction.TransactionMode;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.expiration.SessionExpirationTestCase;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractHotRodSessionExpirationTestCase extends SessionExpirationTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

    public AbstractHotRodSessionExpirationTestCase() {
        super(TransactionMode.NON_TRANSACTIONAL);
    }
}
