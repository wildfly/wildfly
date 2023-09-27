/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractMathTestCase;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamMathTestCase extends AbstractMathTestCase {

    public ProtoStreamMathTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
