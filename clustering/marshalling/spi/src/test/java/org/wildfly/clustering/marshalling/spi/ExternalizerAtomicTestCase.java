/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractAtomicTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.util.concurrent.atomic.AtomicExternalizerProvider;

/**
 * Externalizer tests for java.util.concurrent.atomic.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerAtomicTestCase extends AbstractAtomicTestCase {

    public ExternalizerAtomicTestCase() {
        super(new ExternalizerTesterFactory(AtomicExternalizerProvider.class));
    }
}
