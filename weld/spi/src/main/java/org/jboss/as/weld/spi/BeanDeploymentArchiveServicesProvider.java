/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.Collection;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * Provides services which should be added to a bean deployment archve.
 *
 * @author Martin Kouba
 * @see Service
 * @see BeanDeploymentArchive
 * @see ModuleServicesProvider
 */
public interface BeanDeploymentArchiveServicesProvider {

    /**
     *
     * @param archive
     * @return the services for the given bean deployment archive
     */
    Collection<Service> getServices(BeanDeploymentArchive archive);

}
