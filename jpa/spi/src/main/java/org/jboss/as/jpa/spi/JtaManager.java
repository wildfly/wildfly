/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.spi;

/**
 * Provides access to TSR + TM
 *
 * @author Scott Marlow
 * @deprecated  replaced by {@link org.jipijapa.plugin.spi.JtaManager}
 */
@Deprecated
public interface JtaManager extends org.jipijapa.plugin.spi.JtaManager {

}
