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
package org.jboss.as.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ModelTestLegacyControllerKernelServicesProxy {
    protected final ClassLoader childFirstClassLoader;
    protected final Object childFirstClassLoaderServices;

    private Method getBootError;
    private Method readWholeModel0;
    private Method readWholeModel1;
    private Method readWholeModel2;
    private Method isSuccessfulBoot;
    private Method getPersistedSubsystemXml;
    private Method shutdown;
    private Method executeOperation1;
    private Method executeOperation2;
    private Method validateOperations;
    private Method validateOperation;

    public ModelTestLegacyControllerKernelServicesProxy(ClassLoader childFirstClassLoader, Object childFirstClassLoaderServices) {
        this.childFirstClassLoader = childFirstClassLoader;
        this.childFirstClassLoaderServices = childFirstClassLoaderServices;
    }


    public boolean isSuccessfulBoot() {
        try {
            if (isSuccessfulBoot == null) {
                isSuccessfulBoot = childFirstClassLoaderServices.getClass().getMethod("isSuccessfulBoot");
            }
            return ((Boolean)isSuccessfulBoot.invoke(childFirstClassLoaderServices)).booleanValue();
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public Throwable getBootError() {
        try {
            if (getBootError == null) {
                getBootError = childFirstClassLoaderServices.getClass().getMethod("getBootError");
            }
            return (Throwable)getBootError.invoke(childFirstClassLoaderServices);
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ModelNode readWholeModel() {
        try {
            if (readWholeModel0 == null) {
                readWholeModel0 = childFirstClassLoaderServices.getClass().getMethod("readWholeModel");
            }
            return convertModelNodeFromChildCl(readWholeModel0.invoke(childFirstClassLoaderServices));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ModelNode readWholeModel(boolean includeAliases) {
        try {
            if (readWholeModel1 == null) {
                readWholeModel1 = childFirstClassLoaderServices.getClass().getMethod("readWholeModel", Boolean.TYPE);
            }
            return convertModelNodeFromChildCl(readWholeModel1.invoke(childFirstClassLoaderServices, includeAliases));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ModelNode readWholeModel(boolean includeAliases, boolean includeRuntime) {
        try {
            if (readWholeModel2 == null) {
                readWholeModel2 = childFirstClassLoaderServices.getClass().getMethod("readWholeModel", Boolean.TYPE, Boolean.TYPE);
            }
            return convertModelNodeFromChildCl(readWholeModel2.invoke(childFirstClassLoaderServices, includeAliases, includeRuntime));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ServiceContainer getContainer() {
        return null;
    }


    public ModelNode executeOperation(ModelNode operation, InputStream... inputStreams) {
        try {
            if (executeOperation1 == null) {
                executeOperation1 = childFirstClassLoaderServices.getClass().getMethod("executeOperation",
                        childFirstClassLoader.loadClass(ModelNode.class.getName()),
                        InputStream[].class);
            }
            return convertModelNodeFromChildCl(
                    executeOperation1.invoke(childFirstClassLoaderServices,
                            convertModelNodeToChildCl(operation),
                            inputStreams));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ModelNode executeOperation(ModelNode operation, OperationTransactionControl txControl) {
        try {
            if (executeOperation2 == null) {
                executeOperation2 = childFirstClassLoaderServices.getClass().getMethod("executeOperation",
                        childFirstClassLoader.loadClass(ModelNode.class.getName()),
                        childFirstClassLoader.loadClass(ModelController.OperationTransactionControl.class.getName()));
            }

            Class<?> opTxControlProxyClass = childFirstClassLoader.loadClass(getOperationTransactionProxyClassName());
            Object opTxControl = opTxControlProxyClass.getConstructors()[0].newInstance(txControl);
            return convertModelNodeFromChildCl(
                    executeOperation2.invoke(childFirstClassLoaderServices,
                            convertModelNodeToChildCl(operation),
                            opTxControl));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ModelNode executeForResult(ModelNode operation, InputStream... inputStreams) throws OperationFailedException {
        ModelNode rsp = executeOperation(operation, inputStreams);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }


    public void executeForFailure(ModelNode operation, InputStream... inputStreams) {
        try {
            executeForResult(operation, inputStreams);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
        }
    }


    public String getPersistedSubsystemXml() {
        try {
            if (getPersistedSubsystemXml == null) {
                getPersistedSubsystemXml = childFirstClassLoaderServices.getClass().getMethod("getPersistedSubsystemXml");
            }
            return (String)getPersistedSubsystemXml.invoke(childFirstClassLoaderServices);
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public void validateOperations(List<ModelNode> operations) {
        try {
            if (validateOperations == null) {
                validateOperations = childFirstClassLoaderServices.getClass().getMethod("validateOperations",
                        List.class);
            }

            List<Object> convertedOps = new ArrayList<Object>();
            for (ModelNode operation : operations) {
                convertedOps.add(convertModelNodeToChildCl(operation));
            }
            validateOperations.invoke(childFirstClassLoaderServices, convertedOps);
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public void validateOperation(ModelNode operation) {
        try {
            if (validateOperation == null) {
                validateOperation = childFirstClassLoaderServices.getClass().getMethod("validateOperation",
                        childFirstClassLoader.loadClass(operation.getClass().getName()));
            }
            validateOperation.invoke(childFirstClassLoaderServices, convertModelNodeToChildCl(operation));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public void shutdown() {
        try {
            if (shutdown == null) {
                shutdown = childFirstClassLoaderServices.getClass().getMethod("shutdown");
            }
            shutdown.invoke(childFirstClassLoaderServices);
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }


    public ImmutableManagementResourceRegistration getRootRegistration() {
        //TODO - this might be a problem
        throw new IllegalStateException("Can only be called for the main controller");
    }


    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation)
            throws OperationFailedException {
        throw new IllegalStateException("Can only be called for the main controller");
    }


    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        throw new IllegalStateException("Can only be called for the main controller");
    }


    public ModelNode executeOperation(ModelVersion modelVersion, TransformedOperation op) {
        throw new IllegalStateException("Can only be called for the main controller");

    }

    protected void unwrapInvocationTargetRuntimeException(Exception e) {
        if (e instanceof InvocationTargetException) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
        }
    }

    protected abstract Object convertModelNodeToChildCl(ModelNode modelNode);

    protected abstract ModelNode convertModelNodeFromChildCl(Object object);

    protected abstract String getOperationTransactionProxyClassName();
}
