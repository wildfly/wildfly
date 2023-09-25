/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.util.Set;
import org.jboss.modules.Module;

/**
 * The marshalling context for a bean deployment.
 * @author Paul Ferraro
 */
public interface BeanDeploymentMarshallingContext {

    /**
     * Returns the module of the bean deployment,
     * @return the module of the bean deployment
     */
    Module getModule();

    /**
     * The set of bean classes contained in the bean deployment.
     * @return a set of bean classes
     */
    Set<Class<?>> getBeanClasses();
}
