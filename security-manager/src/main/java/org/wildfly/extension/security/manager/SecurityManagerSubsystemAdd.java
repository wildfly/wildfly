/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import static org.wildfly.extension.security.manager.Constants.DEFAULT_VALUE;
import static org.wildfly.extension.security.manager.Constants.DEPLOYMENT_PERMISSIONS;
import static org.wildfly.extension.security.manager.Constants.MAXIMUM_PERMISSIONS;
import static org.wildfly.extension.security.manager.Constants.MINIMUM_PERMISSIONS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_ACTIONS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_MODULE;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_NAME;

import java.security.AllPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.security.FactoryPermissionCollection;
import org.jboss.modules.security.ImmediatePermissionFactory;
import org.jboss.modules.security.LoadedPermissionFactory;
import org.jboss.modules.security.PermissionFactory;
import org.wildfly.extension.security.manager.deployment.PermissionsParserProcessor;
import org.wildfly.extension.security.manager.deployment.PermissionsValidationProcessor;
import org.wildfly.extension.security.manager.logging.SecurityManagerLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Handler that adds the security manager subsystem. It instantiates the permissions specified in the subsystem configuration
 * and installs the service that will activate the {@link WildFlySecurityManager}.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class SecurityManagerSubsystemAdd extends AbstractAddStepHandler {

    static final SecurityManagerSubsystemAdd INSTANCE = new SecurityManagerSubsystemAdd();

    private SecurityManagerSubsystemAdd() {
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model)
            throws OperationFailedException {

        // This needs to run after all child resources so that they can detect a fresh state
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                ModelNode node = Resource.Tools.readModel(resource);
                launchServices(context, node);
                // Rollback handled by the parent step
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * Retrieves the permissions configured in the security manager subsystem and installs the service that will enable
     * the {@link WildFlySecurityManager}.
     *
     * @param context a reference to the {@link OperationContext}.
     * @param node the {@link ModelNode} that contains all the configured permissions.
     *                   be added.
     * @throws OperationFailedException if an error occurs while launching the security manager services.
     */
    protected void launchServices(final OperationContext context, final ModelNode node)
            throws OperationFailedException {

        // get the minimum set of deployment permissions.
        final List<PermissionFactory> minimumSet = this.retrievePermissionSet(context,
                this.peek(node, DEPLOYMENT_PERMISSIONS, DEFAULT_VALUE, MINIMUM_PERMISSIONS));

        // get the maximum set of deployment permissions.
        final List<PermissionFactory> maximumSet = this.retrievePermissionSet(context,
                this.peek(node, DEPLOYMENT_PERMISSIONS, DEFAULT_VALUE, MAXIMUM_PERMISSIONS));

        if (maximumSet.isEmpty()) {
            maximumSet.add(new ImmediatePermissionFactory(new AllPermission()));
        }

        // validate the configured permissions - the mininum set must be implid by the maximum set.
        final FactoryPermissionCollection maxPermissionCollection = new FactoryPermissionCollection(maximumSet.toArray(new PermissionFactory[maximumSet.size()]));
        final StringBuilder failedPermissions = new StringBuilder();
        for (PermissionFactory factory : minimumSet) {
            Permission permission = factory.construct();
            if (!maxPermissionCollection.implies(permission)) {
                failedPermissions.append("\n\t\t" + permission);
            }
        }
        if (failedPermissions.length() > 0) {
            throw SecurityManagerLogger.ROOT_LOGGER.invalidSubsystemConfiguration(failedPermissions);
        }


        // install the DUPs responsible for parsing and validating security permissions found in META-INF/permissions.xml.
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                 processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_PERMISSIONS,
                         new PermissionsParserProcessor(minimumSet));
                 processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_PERMISSIONS_VALIDATION,
                         new PermissionsValidationProcessor(maximumSet));
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * This method retrieves all security permissions contained within the specified node.
     *
     * @param context the {@link OperationContext} used to resolve the permission attributes.
     * @param node the {@link ModelNode} that might contain security permissions metadata.
     * @return a {@link List} containing the retrieved permissions. They are wrapped as {@link PermissionFactory} instances.
     * @throws OperationFailedException if an error occurs while retrieving the security permissions.
     */
    protected List<PermissionFactory> retrievePermissionSet(final OperationContext context, final ModelNode node) throws OperationFailedException {

        final List<PermissionFactory> permissions = new ArrayList<PermissionFactory>();

        if (node != null && node.isDefined()) {
            for (ModelNode permissionNode : node.asList()) {
                String permissionClass = DeploymentPermissionsResourceDefinition.CLASS.resolveModelAttribute(context, permissionNode).asString();
                String permissionName = null;
                if (permissionNode.hasDefined(PERMISSION_NAME))
                    permissionName = DeploymentPermissionsResourceDefinition.NAME.resolveModelAttribute(context, permissionNode).asString();
                String permissionActions = null;
                if (permissionNode.hasDefined(PERMISSION_ACTIONS))
                    permissionActions = DeploymentPermissionsResourceDefinition.ACTIONS.resolveModelAttribute(context, permissionNode).asString();
                String moduleName = null;
                if(permissionNode.hasDefined(PERMISSION_MODULE)) {
                    moduleName =  DeploymentPermissionsResourceDefinition.MODULE.resolveModelAttribute(context, permissionNode).asString();
                }
                ClassLoader cl = WildFlySecurityManager.getClassLoaderPrivileged(this.getClass());
                if(moduleName != null) {
                    try {
                        cl = Module.getBootModuleLoader().loadModule(ModuleIdentifier.fromString(moduleName)).getClassLoader();
                    } catch (ModuleLoadException e) {
                        throw new OperationFailedException(e);
                    }
                }

                permissions.add(new LoadedPermissionFactory(cl,
                        permissionClass, permissionName, permissionActions));
            }
        }
        return permissions;
    }


    /**
     * Utility method that traverses a {@link ModelNode} according to the provided path. If a valid node is reached, it is
     * returned.
     *
     * @param node the node to be traversed.
     * @param args a {@code String[]} that forms the path to be traversed.
     * @return a reference to the {@link ModelNode} that was reached at the end of the path, or {@code null} if path doesn't
     * lead to a defined node.
     */
    protected ModelNode peek(ModelNode node, String... args) {
        for (String arg : args) {
            if (!node.hasDefined(arg)) {
                return null;
            }
            node = node.get(arg);
        }
        return node;
    }
}
