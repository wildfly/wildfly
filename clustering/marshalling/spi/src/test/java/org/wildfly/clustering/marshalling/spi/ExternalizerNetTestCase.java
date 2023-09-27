/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractNetTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.net.NetExternalizerProvider;

/**
 * Externalizer tests for java.net.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerNetTestCase extends AbstractNetTestCase {

    public ExternalizerNetTestCase() {
        super(new ExternalizerTesterFactory(NetExternalizerProvider.class));
    }
}
