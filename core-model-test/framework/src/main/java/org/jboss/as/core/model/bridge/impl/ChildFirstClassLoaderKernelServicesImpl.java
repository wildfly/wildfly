/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.bridge.impl;

import java.io.InputStream;
import java.util.List;

import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildFirstClassLoaderKernelServicesImpl implements KernelServices {

    @Override
    public boolean isSuccessfulBoot() {
        return true;
    }

    @Override
    public Throwable getBootError() {
        return null;
    }

    @Override
    public KernelServices getLegacyServices(ModelVersion modelVersion) {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation)
            throws OperationFailedException {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public ModelNode executeOperation(ModelVersion modelVersion, TransformedOperation op) {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public ModelNode readWholeModel() {
        return new ModelNode("test");
    }

    @Override
    public ModelNode readWholeModel(boolean includeAliases) {
        return new ModelNode("test a");
    }

    @Override
    public ModelNode readWholeModel(boolean includeAliases, boolean includeRuntime) {
        return new ModelNode("test a b");
    }

    @Override
    public ServiceContainer getContainer() {
        return null;
    }

    @Override
    public ModelNode executeOperation(ModelNode operation, InputStream... inputStreams) {
        System.out.println("Received " + operation);
        return new ModelNode("Result");
    }

    @Override
    public ModelNode executeOperation(ModelNode operation, OperationTransactionControl txControl) {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public ModelNode executeForResult(ModelNode operation, InputStream... inputStreams) throws OperationFailedException {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public void executeForFailure(ModelNode operation, InputStream... inputStreams) {
        throw new IllegalStateException("Should be handled by proxy");
    }

    @Override
    public String getPersistedSubsystemXml() {
        return null;
    }

    @Override
    public void validateOperations(List<ModelNode> operations) {
        System.out.println("Validating ops" + operations);
    }

    @Override
    public void validateOperation(ModelNode operation) {
        System.out.println("Validating op" + operation);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public ImmutableManagementResourceRegistration getRootRegistration() {
        return null;
    }

}
