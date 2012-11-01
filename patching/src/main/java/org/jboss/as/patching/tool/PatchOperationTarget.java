/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.tool;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchResourceDefinition;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.io.IOException;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class PatchOperationTarget {

    static final PathElement CORE_SERVICES = PathElement.pathElement(CORE_SERVICE, "patching");

    /**
     * Get the patch info.
     *
     * @return the patch info
     */
    public abstract PatchInfo getPatchInfo();

    /**
     * Create a local target.
     *
     * @param jbossHome the jboss home
     * @return the local target
     * @throws IOException
     */
    public static final PatchOperationTarget createLocal(final File jbossHome) throws IOException {
        final PatchTool tool = PatchTool.Factory.create(jbossHome);
        return new LocalPatchOperationTarget(tool);
    }

    /**
     * Create a standalone target.
     *
     * @param controllerClient the connected controller client to a standalone instance.
     * @return the remote target
     */
    public static final PatchOperationTarget createStandalone(final ModelControllerClient controllerClient) {
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(CORE_SERVICES);
        return new RemotePatchOperationTarget(address, controllerClient);
    }

    /**
     * Create a host target.
     *
     * @param hostName the host name
     * @param client the connected controller client to the master host.
     * @return the remote target
     */
    public static final PatchOperationTarget createHost(final String hostName, final ModelControllerClient client) {
        final PathElement host = PathElement.pathElement(HOST, hostName);
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(host, CORE_SERVICES);
        return new RemotePatchOperationTarget(address, client);
    }

    //

    protected abstract void applyPatch(final File file, final ContentPolicyBuilderImpl builder) throws PatchingException;
    protected abstract void rollback(final String patchId, final ContentPolicyBuilderImpl builder, final boolean restoreConfiguration) throws PatchingException;

    protected static class LocalPatchOperationTarget extends PatchOperationTarget {

        private final PatchTool tool;
        public LocalPatchOperationTarget(PatchTool tool) {
            this.tool = tool;
        }

        @Override
        public PatchInfo getPatchInfo() {
            return tool.getPatchInfo();
        }

        @Override
        protected void applyPatch(final File file, final ContentPolicyBuilderImpl builder) throws PatchingException {
            final ContentVerificationPolicy policy = builder.createPolicy();
            tool.applyPatch(file, policy);
        }

        @Override
        protected void rollback(final String patchId, final ContentPolicyBuilderImpl builder, boolean restoreConfiguration) throws PatchingException {
            final ContentVerificationPolicy policy = builder.createPolicy();
            // FIXME expose rollbackTo parameter
            tool.rollback(patchId, policy, PatchResourceDefinition.ROLLBACK_TO.getDefaultValue().asBoolean(), restoreConfiguration);
        }

    }

    protected static class RemotePatchOperationTarget extends PatchOperationTarget {

        private final PathAddress address;
        private final ModelControllerClient client;

        public RemotePatchOperationTarget(PathAddress address, ModelControllerClient client) {
            this.address = address;
            this.client = client;
        }

        @Override
        public PatchInfo getPatchInfo() {

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(OP_ADDR).set(address.toModelNode());

            try {
                final ModelNode result = client.execute(operation);

                // TODO parse result

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        }

        @Override
        protected void applyPatch(final File file, final ContentPolicyBuilderImpl policyBuilder) throws PatchingException {
            final ModelNode operation = createOperation(Constants.PATCH, address.toModelNode(), policyBuilder);
            operation.get(INPUT_STREAM_INDEX).set(0);
            final OperationBuilder operationBuilder = OperationBuilder.create(operation);
            operationBuilder.addFileAsAttachment(file);
            try {
                final ModelNode result = client.execute(operationBuilder.build());
                checkConflicts(result);
            } catch (IOException e) {
                throw new PatchingException(e);
            }
        }

        @Override
        protected void rollback(String patchId, ContentPolicyBuilderImpl builder, boolean restoreConfiguration) throws PatchingException {
            final ModelNode operation = createOperation(Constants.PATCH, address.toModelNode(), builder);
            operation.get(Constants.PATCH_ID).set(patchId);
            try {
                final ModelNode result = client.execute(operation);
                checkConflicts(result);
            } catch (IOException e) {
                throw new PatchingException(e);
            }
        }
    }

    static ModelNode createOperation(final String operationName, final ModelNode addr, final ContentPolicyBuilderImpl builder) {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(operationName);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(addr);

        // Process the policy
        operation.get(Constants.OVERRIDE_MODULES).set(builder.ignoreModulesChanges);
        operation.get(Constants.OVERRIDE_ALL).set(builder.overrideAll);
        if(! builder.override.isEmpty()) {
            for(final String o : builder.override) {
                operation.get(Constants.OVERRIDES).add(o);
            }
        }
        if(! builder.preserve.isEmpty()) {
            for(final String p : builder.preserve) {
                operation.get(Constants.PRESERVE).add(p);
            }
        }
        return operation;
    }

    static void checkConflicts(final ModelNode result) throws PatchingException {
        if(! ModelDescriptionConstants.SUCCESS.equals(result.get(ModelDescriptionConstants.OUTCOME))) {
            // TODO format
            throw new PatchingException(result.toString());
        }
    }

}
