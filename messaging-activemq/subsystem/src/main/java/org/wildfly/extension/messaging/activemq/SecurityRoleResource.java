/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.ROLE;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Custom {@link Resource} that represents a messaging security role.
 */
public class SecurityRoleResource implements Resource {

    public static final SecurityRoleResource INSTANCE = new SecurityRoleResource();

    private SecurityRoleResource() {
    }

    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    @Override
    public boolean hasChild(PathElement element) {
        return false;
    }

    @Override
    public Resource getChild(PathElement element) {
        return null;
    }

    @Override
    public Resource requireChild(PathElement element) {
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return false;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        return Collections.emptySet();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public void registerChild(PathElement pathElement, int i, Resource resource) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw MessagingLogger.ROOT_LOGGER.immutableResource();
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Resource clone() {
        return new SecurityRoleResource();
    }

    public static class SecurityRoleResourceEntry extends SecurityRoleResource implements ResourceEntry {

        final PathElement path;

        public SecurityRoleResourceEntry(String name) {
            path = PathElement.pathElement(ROLE, name);
        }

        @Override
        public String getName() {
            return path.getValue();
        }

        @Override
        public PathElement getPathElement() {
            return path;
        }

        @Override
        public SecurityRoleResourceEntry clone() {
            return new SecurityRoleResourceEntry(path.getValue());
        }
    }
}
