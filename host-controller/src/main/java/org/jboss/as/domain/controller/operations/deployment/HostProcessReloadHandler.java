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
package org.jboss.as.domain.controller.operations.deployment;

import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostProcessReloadHandler extends ProcessReloadHandler<HostRunningModeControl>{

    private static final AttributeDefinition RESTART_SERVERS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RESTART_SERVERS, ModelType.BOOLEAN, true)
    .setDefaultValue(new ModelNode(true)).build();

    private static final AttributeDefinition USE_CURRENT_DOMAIN_CONFIG = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USE_CURRENT_DOMAIN_CONFIG, ModelType.BOOLEAN, true)
    .setDefaultValue(new ModelNode(true)).build();

    private static final AttributeDefinition USE_CURRENT_HOST_CONFIG = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USE_CURRENT_HOST_CONFIG, ModelType.BOOLEAN, true)
    .setDefaultValue(new ModelNode(true)).build();


    private static final AttributeDefinition[] MASTER_ATTRIBUTES = new AttributeDefinition[] {ADMIN_ONLY, RESTART_SERVERS, USE_CURRENT_DOMAIN_CONFIG, USE_CURRENT_HOST_CONFIG};

    private static final AttributeDefinition[] SLAVE_ATTRIBUTES = new AttributeDefinition[] {ADMIN_ONLY, RESTART_SERVERS, USE_CURRENT_HOST_CONFIG};

    public static OperationDefinition getDefinition(final LocalHostControllerInfo hostControllerInfo) {
        return new DeferredParametersOperationDefinitionBuilder(hostControllerInfo, OPERATION_NAME, HostModelUtil.getResourceDescriptionResolver())
            .setParameters(hostControllerInfo.isMasterDomainController() ? MASTER_ATTRIBUTES : SLAVE_ATTRIBUTES)
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();
    }

    public HostProcessReloadHandler(final ServiceName rootService, final HostRunningModeControl runningModeControl, final ControlledProcessState processState) {
        super(rootService, runningModeControl, processState);
    }

    @Override
    protected ProcessReloadHandler.ReloadContext<HostRunningModeControl> initializeReloadContext(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final boolean adminOnly = ADMIN_ONLY.resolveModelAttribute(context, operation).asBoolean(false);
        final boolean restartServers = RESTART_SERVERS.resolveModelAttribute(context, operation).asBoolean(true);
        final boolean useCurrentHostConfig = USE_CURRENT_HOST_CONFIG.resolveModelAttribute(context, operation).asBoolean(true);
        final boolean useCurrentDomainConfig = USE_CURRENT_DOMAIN_CONFIG.resolveModelAttribute(context, operation).asBoolean(true);

        return new ReloadContext<HostRunningModeControl>() {

            @Override
            public void reloadInitiated(HostRunningModeControl runningModeControl) {
                runningModeControl.setRestartMode(restartServers ? RestartMode.SERVERS : RestartMode.HC_ONLY);
            }

            @Override
            public void doReload(HostRunningModeControl runningModeControl) {
                runningModeControl.setRunningMode(adminOnly ? RunningMode.ADMIN_ONLY : RunningMode.NORMAL);
                runningModeControl.setReloaded(true);
                runningModeControl.setUseCurrentConfig(useCurrentHostConfig);
                runningModeControl.setUseCurrentDomainConfig(useCurrentDomainConfig);
            }
        };
    }

    /**
     * The host controller info does not know if it is master or not until later in the bootup process
     */
    private static class DeferredParametersOperationDefinitionBuilder extends SimpleOperationDefinitionBuilder {
        private final LocalHostControllerInfo hostControllerInfo;

        public DeferredParametersOperationDefinitionBuilder(LocalHostControllerInfo hostControllerInfo, String name, ResourceDescriptionResolver resolver) {
            super(name, resolver);
            this.hostControllerInfo = hostControllerInfo;
        }

        @Override
        public SimpleOperationDefinition internalBuild(final ResourceDescriptionResolver resolver, final ResourceDescriptionResolver attributeResolver) {
            return new SimpleOperationDefinition(name, resolver, attributeResolver, entryType, flags, replyType, replyValueType, deprecationData, replyParameters, parameters) {
                @Override
                public DescriptionProvider getDescriptionProvider() {
                    return new DescriptionProvider() {
                        @Override
                        public ModelNode getModelDescription(Locale locale) {
                            AttributeDefinition[] params = hostControllerInfo.isMasterDomainController() ? MASTER_ATTRIBUTES : SLAVE_ATTRIBUTES;
                            return new DefaultOperationDescriptionProvider(getName(), resolver, attributeResolver, replyType, replyValueType, deprecationData, replyParameters, params).getModelDescription(locale);
                        }
                    };
                }
            };
        }
    }
}
