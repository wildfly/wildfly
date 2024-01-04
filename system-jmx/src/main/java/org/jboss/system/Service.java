/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.system;

/**
 * The Service interface.
 *
 * @author <a href="mailto:marc.fleury@jboss.org">Marc Fleury</a>.
 * @version $Revision: 81032 $
 */
public interface Service {
    /**
     * create the service, do expensive operations etc
     *
     * @throws Exception for any error
     */
    void create() throws Exception;

    /**
     * start the service, create is already called
     *
     * @throws Exception for any error
     */
    void start() throws Exception;

    /**
     * stop the service
     */
    void stop();

    /**
     * destroy the service, tear down
     */
    void destroy();
}
