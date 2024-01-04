/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import org.wildfly.clustering.marshalling.AbstractConcurrentTestCase;

/**
 * @author Paul Ferraro
 */
public class JBossMarshallingConcurrentTestCase extends AbstractConcurrentTestCase {

    public JBossMarshallingConcurrentTestCase() {
        super(JBossMarshallingTesterFactory.INSTANCE);
    }
}
