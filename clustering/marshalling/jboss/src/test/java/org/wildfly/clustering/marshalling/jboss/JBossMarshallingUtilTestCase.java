/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import org.wildfly.clustering.marshalling.AbstractUtilTestCase;

/**
 * @author Paul Ferraro
 */
public class JBossMarshallingUtilTestCase extends AbstractUtilTestCase {

    public JBossMarshallingUtilTestCase() {
        super(JBossMarshallingTesterFactory.INSTANCE);
    }
}
