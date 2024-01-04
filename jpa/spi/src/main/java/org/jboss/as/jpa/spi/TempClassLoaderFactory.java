/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.spi;

/**
 * Factory for creating temporary classloaders used by persistence providers.
 *
 * @author Antti Laisi
 * @deprecated  replaced by {@link org.jipijapa.plugin.spi.TempClassLoaderFactory}
 */
@Deprecated
public interface TempClassLoaderFactory extends org.jipijapa.plugin.spi.TempClassLoaderFactory {


}
