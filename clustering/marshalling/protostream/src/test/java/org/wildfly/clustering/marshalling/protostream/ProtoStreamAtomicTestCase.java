/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractAtomicTestCase;

/**
 * ProtoStream tests for java.util.concurrent.atomic package.
 * @author Paul Ferraro
 */
public class ProtoStreamAtomicTestCase extends AbstractAtomicTestCase {

    public ProtoStreamAtomicTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
