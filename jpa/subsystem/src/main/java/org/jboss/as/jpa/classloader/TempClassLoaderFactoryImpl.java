/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.classloader;

import org.jipijapa.plugin.spi.TempClassLoaderFactory;

/**
 * Factory implementation that creates {@link TempClassLoader} instances.
 *
 * @author Antti Laisi
 */
public class TempClassLoaderFactoryImpl implements TempClassLoaderFactory {

    private final ClassLoader delegateClassLoader;

    public TempClassLoaderFactoryImpl(final ClassLoader delegateClassLoader) {
        this.delegateClassLoader = delegateClassLoader;
    }

    @Override
    public ClassLoader createNewTempClassLoader() {
        return new TempClassLoader(delegateClassLoader);
    }

}
