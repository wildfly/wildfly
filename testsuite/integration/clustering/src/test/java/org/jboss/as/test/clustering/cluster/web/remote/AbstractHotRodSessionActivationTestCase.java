/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractSessionActivationTestCase;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public abstract class AbstractHotRodSessionActivationTestCase extends AbstractSessionActivationTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

    protected AbstractHotRodSessionActivationTestCase(boolean transactional) {
        super(transactional);
    }

}
