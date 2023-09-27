/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.spi;

/**
 * Registry of started persistence unit services.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @deprecated  replaced by {@link org.jipijapa.plugin.spi.PersistenceUnitServiceRegistry}
 */
public interface PersistenceUnitServiceRegistry extends org.jipijapa.plugin.spi.PersistenceUnitServiceRegistry {

}
