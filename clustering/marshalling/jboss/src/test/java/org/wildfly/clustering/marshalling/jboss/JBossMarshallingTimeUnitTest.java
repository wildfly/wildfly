/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import org.wildfly.clustering.marshalling.AbstractTimeTestCase;

/**
 * @author Paul Ferraro
 */
public class JBossMarshallingTimeUnitTest extends AbstractTimeTestCase {

    public JBossMarshallingTimeUnitTest() {
        super(JBossMarshallingTesterFactory.INSTANCE);
    }
}
