/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.datasource;

import javax.sql.XADataSource;

/**
 * Dummy abstract XADataSource, used to verify that it cannot be specified during xa data source setup.
 *
 * @author <a href="mailto:lgao@redhat.com>Lin Gao</a>
 */
public abstract class DummyXADataSource implements XADataSource {

}
