/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.remote;

import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.web.AbstractSessionActivationTestCase;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public abstract class AbstractHotRodSessionActivationTestCase extends AbstractSessionActivationTestCase {

    @RegisterExtension
    public static final InfinispanServerExtension SERVER = InfinispanServerUtil.infinispanServerExtension();

    protected AbstractHotRodSessionActivationTestCase(boolean transactional) {
        super(transactional);
    }

}
