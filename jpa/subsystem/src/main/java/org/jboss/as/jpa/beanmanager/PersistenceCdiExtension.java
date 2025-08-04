/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.inject.spi.Extension;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * PersistenceCdiExtension
 *
 * @author Scott Marlow
 */
public interface PersistenceCdiExtension extends Extension {
    IntegrationWithCDIBagImpl register(final PersistenceUnitMetadata persistenceUnitMetadata);
}
