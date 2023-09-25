/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import javax.sql.DataSource;

/**
 * Dummy abstract DataSource, used to verify that it cannot be specified during data source setup.
 *
 * @author <a href="mailto:lgao@redhat.com>Lin Gao</a>
 */
public abstract class DummyDataSource implements DataSource {

}
