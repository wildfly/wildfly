/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.ejb;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * Business interface descriptor
 *
 * @author Stuart Douglas
 */
public class BusinessInterfaceDescriptorImpl<T> implements BusinessInterfaceDescriptor<T>{

    private final BeanDeploymentArchive beanDeploymentArchive;
    private final String className;

    public BusinessInterfaceDescriptorImpl(BeanDeploymentArchive beanDeploymentArchive, String className) {
        this.beanDeploymentArchive = beanDeploymentArchive;
        this.className = className;
    }

    @Override
    public Class<T> getInterface() {
        return (Class<T>) beanDeploymentArchive.getServices().get(ResourceLoader.class).classForName(className);
    }
}
