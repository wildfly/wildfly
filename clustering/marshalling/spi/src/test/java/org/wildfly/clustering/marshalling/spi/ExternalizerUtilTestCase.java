/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractUtilTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.util.UtilExternalizerProvider;

/**
 * Externalizer tests for java.util.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerUtilTestCase extends AbstractUtilTestCase {

    public ExternalizerUtilTestCase() {
        super(new ExternalizerTesterFactory(UtilExternalizerProvider.class));
    }
}
