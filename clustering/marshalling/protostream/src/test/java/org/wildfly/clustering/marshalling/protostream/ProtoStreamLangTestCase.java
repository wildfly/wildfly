/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.AbstractLangTestCase;

/**
 * ProtoStream tests for primitives and arrays.
 * @author Paul Ferraro
 */
public class ProtoStreamLangTestCase extends AbstractLangTestCase {

    public ProtoStreamLangTestCase() {
        super(ProtoStreamTesterFactory.INSTANCE);
    }
}
