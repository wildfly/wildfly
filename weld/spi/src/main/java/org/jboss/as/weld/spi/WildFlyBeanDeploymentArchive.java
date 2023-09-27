/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;

/**
 * WildFly bean deployment archive contract.
 *
 * @author Martin Kouba
 */
public interface WildFlyBeanDeploymentArchive extends BeanDeploymentArchive {

    /**
     *
     * @param clazz
     */
    void addBeanClass(String clazz);

    /**
     *
     * @param clazz
     */
    void addBeanClass(Class<?> clazz);

    /**
     *
     * @param descriptor
     */
    void addEjbDescriptor(EjbDescriptor<?> descriptor);

}
