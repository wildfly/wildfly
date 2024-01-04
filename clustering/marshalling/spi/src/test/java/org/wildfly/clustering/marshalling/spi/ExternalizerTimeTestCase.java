/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractTimeTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.time.TimeExternalizerProvider;

/**
 * Externalizer tests for java.time.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerTimeTestCase extends AbstractTimeTestCase {

    public ExternalizerTimeTestCase() {
        super(new ExternalizerTesterFactory(TimeExternalizerProvider.class));
    }
}
