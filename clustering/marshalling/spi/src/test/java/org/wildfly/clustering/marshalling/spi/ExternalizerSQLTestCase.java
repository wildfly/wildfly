/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.AbstractSQLTestCase;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.spi.sql.SQLExternalizerProvider;

/**
 * Externalizer tests for java.sql.* classes.
 * @author Paul Ferraro
 */
public class ExternalizerSQLTestCase extends AbstractSQLTestCase {

    public ExternalizerSQLTestCase() {
        super(new ExternalizerTesterFactory(SQLExternalizerProvider.class));
    }
}
