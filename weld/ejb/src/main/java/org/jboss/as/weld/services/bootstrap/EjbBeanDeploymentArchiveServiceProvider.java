/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.weld.spi.BeanDeploymentArchiveServicesProvider;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 *
 * @author Martin Kouba
 */
public class EjbBeanDeploymentArchiveServiceProvider implements BeanDeploymentArchiveServicesProvider {

    @Override
    public Collection<Service> getServices(BeanDeploymentArchive archive) {
        return Collections.singleton(new WeldEjbServices());
    }

}
