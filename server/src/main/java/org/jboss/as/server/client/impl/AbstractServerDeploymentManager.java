/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.client.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.server.client.api.deployment.DeploymentPlan;
import org.jboss.as.server.client.api.deployment.DuplicateDeploymentNameException;
import org.jboss.as.server.client.api.deployment.InitialDeploymentPlanBuilder;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.as.server.client.impl.deployment.DeploymentActionImpl;
import org.jboss.as.server.client.impl.deployment.DeploymentContentDistributor;
import org.jboss.as.server.client.impl.deployment.DeploymentPlanImpl;
import org.jboss.as.server.client.impl.deployment.InitialDeploymentPlanBuilderFactory;
import org.jboss.as.server.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 */
abstract class AbstractServerDeploymentManager implements ServerDeploymentManager {

    private final DeploymentContentDistributor contentDistributor;

    AbstractServerDeploymentManager() {

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                boolean unique = AbstractServerDeploymentManager.this.isDeploymentNameUnique(name);
                if (!unique) {
                    throw new DuplicateDeploymentNameException(name, false);
                }
                return AbstractServerDeploymentManager.this.uploadDeploymentContent(name, runtimeName, stream);
            }

            @Override
            public byte[] distributeReplacementDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException {
                return AbstractServerDeploymentManager.this.uploadDeploymentContent(name, runtimeName, stream);
            }
        };
    }

    @Override
    public String addDeploymentContent(File file) throws IOException, DuplicateDeploymentNameException {
        String name = file.getName();
        uploadDeploymentContent(name, name, new FileInputStream(file));
        return name;
    }

    @Override
    public String addDeploymentContent(URL url) throws IOException, DuplicateDeploymentNameException {
        String name = getName(url);
        addDeploymentContent(name, name, url);
        return name;
    }

    @Override
    public void addDeploymentContent(String name, File file) throws IOException, DuplicateDeploymentNameException {
        String commonName = file.getName();
        uploadDeploymentContent(name, commonName, new FileInputStream(file));
    }

    @Override
    public void addDeploymentContent(String name, URL url) throws IOException, DuplicateDeploymentNameException {
        String commonName = getName(url);
        addDeploymentContent(name, commonName, url);
    }

    private void addDeploymentContent(String name, String commonName, URL url) throws IOException,
            DuplicateDeploymentNameException {
        URLConnection conn = url.openConnection();
        conn.connect();
        uploadDeploymentContent(name, commonName, conn.getInputStream());
    }

    @Override
    public void addDeploymentContent(String name, InputStream stream) throws IOException,
            DuplicateDeploymentNameException {
        addDeploymentContent(name, name, stream);
    }

    @Override
    public void addDeploymentContent(String name, String commonName, InputStream stream) throws IOException,
            DuplicateDeploymentNameException {
        uploadDeploymentContent(name, commonName, stream);
    }

    /** {@inheritDoc} */
    @Override
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder(contentDistributor);
    }

    /** {@inheritDoc} */
    @Override
    public Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan) {
        if (!(plan instanceof DeploymentPlanImpl)) {
            throw new IllegalArgumentException("Plan was not created by this manager");
        }
        DeploymentPlanImpl planImpl = (DeploymentPlanImpl) plan;
        ModelNode operation = getCompositeOperation(planImpl);
        Future<ModelNode> nodeFuture = executeOperation(operation);
        return new ServerDeploymentPlanResultFuture(planImpl, nodeFuture);
    }

    private Future<ModelNode> executeOperation(ModelNode operation){
        return executeOperation(ExecutionContextBuilder.Factory.create(operation).build());
    }
    protected abstract Future<ModelNode> executeOperation(ExecutionContext context);

    private static String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(url + " is not a valid URI", e);
            }
        }

        String path = url.getPath();
        int idx = path.lastIndexOf('/');
        while (idx == path.length() - 1) {
            path = path.substring(0, idx);
            idx = path.lastIndexOf('/');
        }
        if (idx == -1) {
            throw new IllegalArgumentException("Cannot derive a deployment name from " + url
                    + " -- use an overloaded method variant that takes a 'name' parameter");
        }

        return path.substring(idx + 1);
    }

    private byte[] uploadDeploymentContent(String name, String runtimeName, InputStream stream) throws IOException {
        ModelNode op = new ModelNode();
        op.get("operation").set(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME);
        op.get("address").setEmptyList();
        op.get("name").set(name);
        op.get("runtime-name").set(runtimeName);
        op.get("attachment").set(0);
        try {
            try {
                ExecutionContext ctx = ExecutionContextBuilder.Factory.create(op).addInputStream(stream).build();
                ModelNode response = executeOperation(ctx).get();
                return response.asBytes();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        } catch (InterruptedException e) {
            IOException ioe = new InterruptedIOException();
            ioe.initCause(e);
            throw ioe;
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isDeploymentNameUnique(String name) throws IOException {

        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("address").setEmptyList();
        op.get("child-type").set("deployment");
        try {
            try {
                ModelNode response = executeOperation(op).get();
                Set<String> deploymentNames = new HashSet<String>();
                if (response.isDefined()) {
                    List<ModelNode> deploymentNodes = response.asList();
                    for (ModelNode node : deploymentNodes) {
                        deploymentNames.add(node.asString());
                    }
                }
                return !deploymentNames.contains(name);
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        } catch (InterruptedException e) {
            IOException ioe = new InterruptedIOException();
            ioe.initCause(e);
            throw ioe;
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode getCompositeOperation(DeploymentPlanImpl plan) {

        ModelNode op = new ModelNode();
        op.get("operation").set("composite");
        op.get("address").setEmptyList();
        ModelNode steps = op.get("steps");
        steps.setEmptyList();
        op.get("rollback-on-runtime-failure").set(plan.isGlobalRollback());
        // FIXME deal with shutdown params

        for (DeploymentActionImpl action : plan.getDeploymentActionImpls()) {
            ModelNode step = new ModelNode();
            String uniqueName = action.getDeploymentUnitUniqueName();
            switch (action.getType()) {
            case ADD: {
                configureDeploymentOperation(step, "add", uniqueName);
                step.get("runtime-name").set(action.getNewContentFileName());
                step.get("hash").set(action.getNewContentHash());
                break;
            }
            case DEPLOY: {
                configureDeploymentOperation(step, "deploy", uniqueName);
                break;
            }
            case FULL_REPLACE: {
                step.get("operation").set("full-replace-deployment");
                step.get("address").setEmptyList();
                step.get("name").set(uniqueName);
                step.get("runtime-name").set(action.getNewContentFileName());
                step.get("hash").set(action.getNewContentHash());
                break;
            }
            case REDEPLOY: {
                configureDeploymentOperation(step, "redeploy", uniqueName);
                break;
            }
            case REMOVE: {
                configureDeploymentOperation(step, "remove", uniqueName);
                break;
            }
            case REPLACE: {
                step.get("operation").set("replace-deployment");
                step.get("address").setEmptyList();
                step.get("name").set(uniqueName);
                step.get("to-replace").set(action.getReplacedDeploymentUnitUniqueName());
                break;
            }
            case UNDEPLOY: {
                configureDeploymentOperation(step, "undeploy", uniqueName);
                break;
            }
            default: {
                throw new IllegalStateException("Unknown action type " + action.getType());
            }
            }
            steps.add(step);
        }
        return op;
    }

    private void configureDeploymentOperation(ModelNode op, String operationName, String uniqueName) {
        op.get("operation").set(operationName);
        op.get("address").add("deployment", uniqueName);
    }
}
