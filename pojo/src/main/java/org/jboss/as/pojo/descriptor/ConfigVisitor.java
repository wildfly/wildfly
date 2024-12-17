/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;

import java.util.Deque;

/**
 * Config visitor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ConfigVisitor {
    /**
     * Visit node's children.
     *
     * @param node the config node
     */
    void visit(ConfigVisitorNode node);

    /**
     * Get current nodes.
     *
     * @return the current nodes
     */
    Deque<ConfigVisitorNode> getCurrentNodes();

    /**
     * Get current state.
     *
     * @return the state
     */
    BeanState getState();

    /**
     * Get module for this visitor.
     *
     * @return the classloader
     */
    Module getModule();

    /**
     * Load module.
     *
     * @param identifier the module identifier
     * @return loaded module
     */
    Module loadModule(String identifier);

    /**
     * Get reflection index.
     *
     * @return the reflection index
     */
    DeploymentReflectionIndex getReflectionIndex();

    /**
     * Get bean info.
     *
     * @return the bean info
     */
    BeanInfo getBeanInfo();

    /**
     * Add dependency.
     *
     * @param name the dependency name
     */
    void addDependency(ServiceName name);

    /**
     * Add dependency.
     *
     * @param name     the dependency name
     * @param injector the injector
     */
    void addDependency(ServiceName name, Injector injector);

    /**
     * Add bean dependency.
     *
     * @param bean  the dependency name
     * @param state the required bean state
     */
    void addDependency(String bean, BeanState state);

    /**
     * Add bean dependency.
     *
     * @param bean     the dependency name
     * @param state    the required bean state
     * @param injector the injector
     */
    void addDependency(String bean, BeanState state, Injector injector);
}
