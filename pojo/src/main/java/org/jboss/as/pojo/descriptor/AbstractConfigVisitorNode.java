/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.pojo.service.DefaultBeanInfo;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Abstract config visitor node.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractConfigVisitorNode implements ConfigVisitorNode, TypeProvider {
    public void visit(ConfigVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Add children as needed.
     *
     * @param visitor the current visitor
     * @param nodes   the nodes list to add to
     */
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
    }

    public Iterable<ConfigVisitorNode> getChildren(ConfigVisitor visitor) {
        List<ConfigVisitorNode> nodes = new ArrayList<ConfigVisitorNode>();
        addChildren(visitor, nodes);
        return nodes;
    }

    /**
     * Get temp bean info.
     *
     * @param visitor   the visitor
     * @param className the class name
     * @return bean info
     */
    protected static BeanInfo getTempBeanInfo(ConfigVisitor visitor, String className) {
        return getTempBeanInfo(visitor, getType(visitor, className));
    }

    /**
     * Get temp bean info.
     *
     * @param visitor the visitor
     * @param clazz   the class
     * @return bean info
     */
    @SuppressWarnings({"unchecked"})
    protected static BeanInfo getTempBeanInfo(ConfigVisitor visitor, Class<?> clazz) {
        return new DefaultBeanInfo(visitor.getReflectionIndex(), clazz);
    }

    /**
     * Get temp bean info.
     *
     * @param clazz the class
     * @return bean info
     */
    @SuppressWarnings({"unchecked"})
    protected static BeanInfo getTempBeanInfo(Class<?> clazz) {
        return new DefaultBeanInfo(DeploymentReflectionIndex.create(), clazz);
    }

    /**
     * Load class.
     *
     * @param visitor   the visitor
     * @param className the class name
     * @return class or null if null class name
     */
    protected static Class<?> getType(ConfigVisitor visitor, String className) {
        if (className != null) {
            try {
                return visitor.getModule().getClassLoader().loadClass(className);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    /**
     * Get component type.
     *
     * @param type the type
     * @param index the component index
     * @return component's class or null if cannot be determined
     */
    static Type getComponentType(ParameterizedType type, int index) {
        Type[] tp = type.getActualTypeArguments();
        if (index + 1 > tp.length)
            return null;

        return tp[index];
    }

    @Override
    public Class<?> getType(ConfigVisitor visitor, ConfigVisitorNode previous) {
        Deque<ConfigVisitorNode> nodes = visitor.getCurrentNodes();
        if (nodes.isEmpty())
            throw PojoLogger.ROOT_LOGGER.cannotDetermineType();

        ConfigVisitorNode current = nodes.pop();
        try {
            if (current instanceof TypeProvider) {
                return TypeProvider.class.cast(current).getType(visitor, current);
            } else {
                return getType(visitor, current);
            }
        } finally {
            nodes.push(current);
        }

    }
}
