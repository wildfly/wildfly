/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.sso.remote;

import org.jboss.as.test.clustering.InfinispanServerUtil;
import org.jboss.as.test.clustering.cluster.sso.AbstractSingleSignOnTestCase;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Radoslav Husar
 */
public abstract class AbstractRemoteSingleSignOnTestCase extends AbstractSingleSignOnTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = InfinispanServerUtil.infinispanServerTestRule();

}
