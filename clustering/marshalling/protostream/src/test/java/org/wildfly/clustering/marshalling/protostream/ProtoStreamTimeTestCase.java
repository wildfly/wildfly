/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractTimeTestCase;

/**
 * ProtoStream tests for java.time package.
 * @author Paul Ferraro
 */
public class ProtoStreamTimeTestCase extends AbstractTimeTestCase {

    public ProtoStreamTimeTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
