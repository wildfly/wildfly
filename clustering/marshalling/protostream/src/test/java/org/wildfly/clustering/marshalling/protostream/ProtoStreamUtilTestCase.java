/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractUtilTestCase;

/**
 * ProtoStream tests for java.util package.
 * @author Paul Ferraro
 */
public class ProtoStreamUtilTestCase extends AbstractUtilTestCase {

    public ProtoStreamUtilTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
