/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

/**
 * Factory for creating temporary classloaders used by persistence providers.
 *
 * @author Antti Laisi
 */
public interface TempClassLoaderFactory {

    /**
     * Creates a temporary classloader with the same scope and classpath as the persistence unit classloader.
     *
     * @see jakarta.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
     */
    ClassLoader createNewTempClassLoader();

}
