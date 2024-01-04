/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.util.Map;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * TwoPhaseBootstrapCapable obtains a two phase EntityManagerFactory builder
 *
 * @author Scott Marlow
 * @author Steve Ebersole
 */
public interface TwoPhaseBootstrapCapable {

    /**
     * Returns a two phase EntityManagerFactory builder
     *
     * @param info
     * @param map
     * @return
     */
    EntityManagerFactoryBuilder getBootstrap(final PersistenceUnitInfo info, final Map map);
}
