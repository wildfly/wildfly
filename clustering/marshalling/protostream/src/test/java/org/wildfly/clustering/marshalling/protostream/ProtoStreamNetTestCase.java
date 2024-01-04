/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractNetTestCase;

/**
 * ProtoStream tests for java.net package.
 * @author Paul Ferraro
 */
public class ProtoStreamNetTestCase extends AbstractNetTestCase {

    public ProtoStreamNetTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
