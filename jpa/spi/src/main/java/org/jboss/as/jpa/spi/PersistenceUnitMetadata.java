/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.spi;

/**
 * Represents the persistence unit definition
 *
 * @author Scott Marlow
 * @deprecated  replaced by {@link org.jipijapa.plugin.spi.PersistenceUnitMetadata}
 */
@Deprecated
public interface PersistenceUnitMetadata extends org.jipijapa.plugin.spi.PersistenceUnitMetadata {
}
