/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.embedded;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.infinispan.AbstractCacheTestCase;
import org.jboss.as.test.clustering.cluster.infinispan.bean.embedded.CreatedCacheBean;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author Paul Ferraro
 */
public class CreatedInfinispanCacheTestCase extends AbstractCacheTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return createDeployment(CreatedCacheBean.class, "org.infinispan");
    }
}
