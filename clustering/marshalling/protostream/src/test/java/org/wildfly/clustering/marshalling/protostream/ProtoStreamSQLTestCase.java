/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractSQLTestCase;

/**
 * ProtoStream tests for java.sql package.
 * @author Paul Ferraro
 */
public class ProtoStreamSQLTestCase extends AbstractSQLTestCase {

    public ProtoStreamSQLTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
