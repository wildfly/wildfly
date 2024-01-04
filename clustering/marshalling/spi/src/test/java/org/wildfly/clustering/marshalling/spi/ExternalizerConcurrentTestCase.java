/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractConcurrentTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.util.concurrent.ConcurrentExternalizerProvider;

/**
 * Externalizer tests for java.util.concurrent.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerConcurrentTestCase extends AbstractConcurrentTestCase {

    public ExternalizerConcurrentTestCase() {
        super(new ExternalizerTesterFactory(ConcurrentExternalizerProvider.class));
    }
}
