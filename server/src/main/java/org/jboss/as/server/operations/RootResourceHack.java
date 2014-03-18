/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.UnauthorizedException;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.NotificationEntry;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;

/**
 * Ugly hack to be able to get the root resource and registration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RootResourceHack implements OperationStepHandler {

    public static final RootResourceHack INSTANCE = new RootResourceHack();
    public static final String NAME = "root-resource-hack";

    //Private method does not need resources for description
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(NAME, null)
        .setPrivateEntry()
        .setRuntimeOnly()
        .build();
    private static final ModelNode OPERATION;
    static {
        OPERATION = new ModelNode();
        OPERATION.get(OP).set(NAME);
    }

    private ThreadLocal<ResourceAndRegistration> resource = new ThreadLocal<ResourceAndRegistration>();

    private RootResourceHack() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ResourceAndRegistration threadResource = resource.get();
        if (threadResource == null || threadResource != ResourceAndRegistration.NULL) {
            throw ServerMessages.MESSAGES.internalUseOnly();
        }
        try {
        resource.set(new ResourceAndRegistration(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true), context.getResourceRegistration()));
        } catch (UnauthorizedException e) {
            resource.set(new ResourceAndRegistration(Resource.Factory.create(), new EmptyResourceRegistration()));
        }
        context.stepCompleted();
    }

    public ResourceAndRegistration getRootResource(ModelController controller) {
        resource.set(ResourceAndRegistration.NULL);
        ResourceAndRegistration reg = null;
        try {
            controller.execute(OPERATION, null, OperationTransactionControl.COMMIT, null);
        } finally {
            reg = resource.get();
            resource.remove();
            if (ResourceAndRegistration.NULL == reg) {
                throw ServerMessages.MESSAGES.cannotGetRootResource();
            }
        }
        return reg;
    }

    public static class ResourceAndRegistration {
        private static final ResourceAndRegistration NULL = new ResourceAndRegistration(null, null);
        private final Resource resource;
        private final ImmutableManagementResourceRegistration registry;

        private ResourceAndRegistration(final Resource resource, ImmutableManagementResourceRegistration registry) {
            this.resource = resource;
            this.registry = registry;
        }

        public Resource getResource() {
            return resource;
        }

        public ImmutableManagementResourceRegistration getRegistration() {
            return registry;
        }
    }

    private static class EmptyResourceRegistration implements ImmutableManagementResourceRegistration {

        @Override
        public boolean isRuntimeOnly() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public boolean isAlias() {
            return false;
        }

        @Override
        public AliasEntry getAliasEntry() {
            return null;
        }

        @Override
        public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public Set<Flag> getOperationFlags(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public OperationEntry getOperationEntry(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public Set<String> getAttributeNames(PathAddress address) {
            return Collections.emptySet();
        }

        @Override
        public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
            return null;
        }

        @Override
        public Set<String> getChildNames(PathAddress address) {
            return Collections.emptySet();
        }

        @Override
        public Set<PathElement> getChildAddresses(PathAddress address) {
            return Collections.emptySet();
        }

        @Override
        public DescriptionProvider getModelDescription(PathAddress address) {
            return null;
        }

        @Override
        public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, NotificationEntry> getNotificationDescriptions(PathAddress address, boolean inherited) {
            return Collections.emptyMap();
        }

        @Override
        public ProxyController getProxyController(PathAddress address) {
            return null;
        }

        @Override
        public Set<ProxyController> getProxyControllers(PathAddress address) {
            return Collections.emptySet();
        }

        @Override
        public ImmutableManagementResourceRegistration getSubModel(PathAddress address) {
            return null;
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return Collections.emptyList();
        }
    }
}
