/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractCircularReferenceTestCase;

/**
 * @author Paul Ferraro
 */
public class ProtoStreamCircularReferenceTestCase extends AbstractCircularReferenceTestCase {

    public ProtoStreamCircularReferenceTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
