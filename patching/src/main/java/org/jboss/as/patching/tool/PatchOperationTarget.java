/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class PatchOperationTarget {

    static final PathElement CORE_SERVICES = PathElement.pathElement(CORE_SERVICE, "patching");

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

    protected abstract ModelNode info() throws IOException;
    protected abstract ModelNode applyPatch(final File file, final ContentPolicyBuilderImpl builder) throws IOException;
    protected abstract ModelNode rollback(final String patchId, final ContentPolicyBuilderImpl builder, boolean rollbackTo, final boolean restoreConfiguration) throws IOException;

    protected static class LocalPatchOperationTarget extends PatchOperationTarget {

        private final PatchTool tool;
        public LocalPatchOperationTarget(PatchTool tool) {
            this.tool = tool;
        }

        @Override
        protected ModelNode info() throws IOException {
            final PatchInfo info = tool.getPatchInfo();
            final ModelNode result = new ModelNode();
            result.get(OUTCOME).set(SUCCESS);
            result.get(RESULT, Constants.CUMULATIVE).set(info.getCumulativeID());
            result.get(RESULT, Constants.PATCHES).setEmptyList();
            for(final String patch : info.getPatchIDs()) {
                result.get(RESULT, Constants.PATCHES).add(patch);
            }
            return result;
        }

        @Override
        protected ModelNode applyPatch(final File file, final ContentPolicyBuilderImpl builder) {
            final ContentVerificationPolicy policy = builder.createPolicy();
            ModelNode result = new ModelNode();
            try {
                PatchingResult apply = tool.applyPatch(file, policy);
                apply.commit();
                result.get(OUTCOME).set(SUCCESS);
                result.get(RESULT).setEmptyObject();
            } catch (PatchingException e) {
                return formatFailedResponse(e);
            }
            return result;
        }

        @Override
        protected ModelNode rollback(final String patchId, final ContentPolicyBuilderImpl builder, boolean rollbackTo, boolean restoreConfiguration) {
            final ContentVerificationPolicy policy = builder.createPolicy();
            ModelNode result = new ModelNode();
            try {
                PatchingResult rollback = tool.rollback(patchId, policy, rollbackTo, restoreConfiguration);
                rollback.commit();
                result.get(OUTCOME).set(SUCCESS);
                result.get(RESULT).setEmptyObject();
            } catch (PatchingException e) {
                return formatFailedResponse(e);
            }
            return result;
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
        protected ModelNode info() throws IOException {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(OP_ADDR).set(address.toModelNode());
            operation.get(INCLUDE_RUNTIME).set(true);
            return client.execute(operation);
        }

        @Override
        protected ModelNode applyPatch(final File file, final ContentPolicyBuilderImpl policyBuilder) throws IOException {
            final ModelNode operation = createOperation(Constants.PATCH, address.toModelNode(), policyBuilder);
            operation.get(INPUT_STREAM_INDEX).set(0);
            final OperationBuilder operationBuilder = OperationBuilder.create(operation);
            operationBuilder.addFileAsAttachment(file);
            return client.execute(operationBuilder.build());
        }

        @Override
        protected ModelNode rollback(String patchId, ContentPolicyBuilderImpl builder, boolean rollbackTo, boolean restoreConfiguration) throws IOException {
            final ModelNode operation = createOperation(Constants.ROLLBACK, address.toModelNode(), builder);
            operation.get(Constants.PATCH_ID).set(patchId);
            return client.execute(operation);
        }
    }

    static ModelNode formatFailedResponse(final PatchingException e) {
        final ModelNode result = new ModelNode();
        result.get(OUTCOME).set(FAILED);
        if(e.hasConflicts()) {
            final ModelNode failureDescription = result.get(FAILURE_DESCRIPTION);
            for(final ContentItem item : e.getConflicts()) {
                final ContentType type = item.getContentType();
                switch (type) {
                    case BUNDLE:
                        failureDescription.get(Constants.BUNDLES).add(item.getRelativePath());
                        break;
                    case MODULE:
                        failureDescription.get(Constants.MODULES).add(item.getRelativePath());
                        break;
                    case MISC:
                        failureDescription.get(Constants.MISC).add(item.getRelativePath());
                        break;
                }
            }
        } else {
            result.get(FAILURE_DESCRIPTION).set(e.getLocalizedMessage());
        }
        return result;
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
                operation.get(Constants.OVERRIDE).add(o);
            }
        }
        if(! builder.preserve.isEmpty()) {
            for(final String p : builder.preserve) {
                operation.get(Constants.PRESERVE).add(p);
            }
        }
        return operation;
    }
}
