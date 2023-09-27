/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractConcurrentTestCase;

/**
 * ProtoStream tests for java.util.concurrent package.
 * @author Paul Ferraro
 */
public class ProtoStreamConcurrentTestCase extends AbstractConcurrentTestCase {

    public ProtoStreamConcurrentTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
