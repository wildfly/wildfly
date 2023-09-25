/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

/**
 * The Wildfly's EE context handle.
 *
 * @author Eduardo Martins
 */
public interface ResetContextHandle extends org.glassfish.enterprise.concurrent.spi.ContextHandle {

    /**
     * @see org.glassfish.enterprise.concurrent.spi.ContextSetupProvider#reset(org.glassfish.enterprise.concurrent.spi.ContextHandle)
     */
    void reset();

    /**
     * Retrieves the name of the factory which built the handle.
     * @return
     */
    String getFactoryName();
}
