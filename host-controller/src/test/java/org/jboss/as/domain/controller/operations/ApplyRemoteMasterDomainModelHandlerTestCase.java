/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.access.rbac.StandardRBACAuthorizer;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationEntry;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResource;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ApplyRemoteMasterDomainModelHandlerTestCase extends AbstractOperationTestCase {

    private final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL));
    private final ApplyExtensionsHandler extensionHandler = new ApplyExtensionsHandler(extensionRegistry, HOST_INFO, new IgnoredDomainResourceRegistry(HOST_INFO)) {
        @Override
        protected void initializeExtension(String module) {
            // nothing here
        }
    };
    WritableAuthorizerConfiguration authorizerConfiguration = new WritableAuthorizerConfiguration(StandardRBACAuthorizer.AUTHORIZER_DESCRIPTION);
    private final ApplyRemoteMasterDomainModelHandler handler = new ApplyRemoteMasterDomainModelHandler(new MockDomainController(),
            createHostControllerEnvironment(), null, null, HOST_INFO, new IgnoredDomainResourceRegistry(HOST_INFO), authorizerConfiguration);

    @Test
    public void testNoChanges() throws Exception {
        Resource root = createRootResource();

        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).set(getCurrentModelUpdates(root, UpdateListModifier.createForAdditions()));
        final MockOperationContext operationContext = getOperationContext(root, false);
        handler.execute(operationContext, operation);
        final List<OperationAndHandler> operations = operationContext.verify().get(OperationContext.Stage.MODEL);
        Assert.assertEquals(4, operations.size());
    }

    @Test
    public void testBooting() throws Exception {
        Resource root = createRootResource();

        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext(root, true);
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testRootResource() throws Exception {
        Resource root = createRootResource();

        final ModelNode rootValues = new ModelNode();
        rootValues.get("my-version").set(42);
        rootValues.get("product-name").set("my-product");

        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.EMPTY_ADDRESS.toModelNode());
        change.get("domain-resource-model").set(rootValues);


        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext(root, true);
        handler.execute(operationContext, operation);
        operationContext.verify();

        ModelNode appliedRootValues = root.getModel();
        Assert.assertEquals(rootValues, appliedRootValues);
    }

    @Test
    public void testExtensionAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS).set(PathAddress.pathAddress(PathElement.pathElement(EXTENSION, "org.jboss.extension")).toModelNode());
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_MODEL).set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext(false);
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        extensionHandler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testExtensionRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext(false);
        operationContext.root.registerChild(PathElement.pathElement(EXTENSION, "org.jboss.extension"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        extensionHandler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testChangesToNonExtensions() throws Exception {
        Resource root = createRootResource();

        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS).set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        final ModelNode group = root.clone().getChild(PathElement.pathElement(SERVER_GROUP, "group-one")).getModel();
        group.get(PROFILE).set("profile-two");
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_MODEL).set(group);
        operation.get(DOMAIN_MODEL).set(getCurrentModelUpdates(root, UpdateListModifier.createForChanges(change)));

        MockOperationContext operationContext = getOperationContext(root, false);
        operationContext.expectStep(PathAddress.EMPTY_ADDRESS);
        handler.execute(operationContext, operation);
        Map<OperationContext.Stage, List<OperationAndHandler>> addedSteps = operationContext.verify();

        assertTrue(addedSteps.containsKey(OperationContext.Stage.MODEL));
        List<OperationAndHandler> modelSteps = addedSteps.get(OperationContext.Stage.MODEL);
        assertEquals(4, modelSteps.size());
        OperationAndHandler oah = modelSteps.get(3);

        operationContext = getOperationContext(root, false);
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        oah.handler.execute(operationContext, oah.operation);
        operationContext.verify();
    }

    @Test
    public void testDifferentOrderNoChange() throws Exception {
        Resource root = createRootResource();
        Resource socketBindingGroup = root.getChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "binding-one"));
        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().setEmptyObject();
        socketBindingGroup.registerChild(PathElement.pathElement(SOCKET_BINDING, "bindingA"), resourceA);
        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().setEmptyObject();
        socketBindingGroup.registerChild(PathElement.pathElement(SOCKET_BINDING, "bindingB"), resourceB);

        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).set(getCurrentModelUpdates(root.clone(), UpdateListModifier.createForAdditions()));

        socketBindingGroup.removeChild(PathElement.pathElement(SOCKET_BINDING, "bindingA"));
        socketBindingGroup.removeChild(PathElement.pathElement(SOCKET_BINDING, "bindingB"));
        socketBindingGroup.registerChild(PathElement.pathElement(SOCKET_BINDING, "bindingB"), resourceB);
        socketBindingGroup.registerChild(PathElement.pathElement(SOCKET_BINDING, "bindingA"), resourceA);

        final MockOperationContext operationContext = getOperationContext(root, false);
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testRolloutPlans() throws Exception {
        Resource root = createRootResource();

        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS));
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS).set(pa.toModelNode());
        change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_MODEL).set(new ModelNode());
        operation.get(DOMAIN_MODEL).set(getCurrentModelUpdates(root, UpdateListModifier.createForAdditions(change)));
        final MockOperationContext operationContext = getOperationContext(root, false);
        handler.execute(operationContext, operation);
        operationContext.verify();
        Resource r = operationContext.root.navigate(pa);
        assertTrue(r instanceof ManagedDMRContentTypeResource);
    }

    private ModelNode getCurrentModelUpdates(Resource root, UpdateListModifier modifier) throws Exception {
        MockOperationContext context = getOperationContext(root, true);
        DomainControllerRuntimeIgnoreTransformationRegistry registry = new DomainControllerRuntimeIgnoreTransformationRegistry();
        registry.initializeHost("localhost");
        new ReadMasterDomainModelHandler("localhost", new NoopTransformers(), registry).execute(context, new ModelNode());
        return modifier.modifyList(context.getResult());
    }

    private MockOperationContext getOperationContext(Resource root, boolean booting) {
        return new MockOperationContext(root, booting, PathAddress.EMPTY_ADDRESS, false);
    }


    private static class UpdateListModifier {
        private ModelNode[] additions;
        private Map<PathAddress, ModelNode> changes;
        private Set<PathAddress> removals;

        public UpdateListModifier(ModelNode[] additions, Map<PathAddress, ModelNode> changes, Set<PathAddress> removals) {
            this.additions = additions;
            this.changes = changes;
            this.removals = removals;
        }

        static UpdateListModifier createForAdditions(ModelNode...additions) {
            return new UpdateListModifier(additions, Collections.<PathAddress, ModelNode>emptyMap(), Collections.<PathAddress>emptySet());
        }

        static UpdateListModifier createForChanges(ModelNode...changes) {
            Map<PathAddress, ModelNode> changeMap = new HashMap<PathAddress, ModelNode>();
            for (ModelNode change : changes) {
                changeMap.put(PathAddress.pathAddress(change.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS)), change);
            }
            return new UpdateListModifier(new ModelNode[0], changeMap, Collections.<PathAddress>emptySet());
        }

        static UpdateListModifier createForRemovals(PathAddress...removals) {
            Set<PathAddress> removedSet = new HashSet<>(Arrays.asList(removals));
            return new UpdateListModifier(new ModelNode[0], Collections.<PathAddress, ModelNode>emptyMap(), removedSet);
        }

        ModelNode modifyList(ModelNode existing) {
            ModelNode result = new ModelNode();
            for (ModelNode current : existing.asList()) {
                PathAddress addr = PathAddress.pathAddress(current.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS));
                if (removals.contains(addr)) {
                    continue;
                }
                if (changes.containsKey(addr)) {
                    result.add(changes.get(addr));
                } else {
                    result.add(current);
                }
            }
            for (ModelNode addition : additions) {
                result.add(addition);
            }
            return result;
        }
    }

    private static class MockDomainController implements DomainController {

        @Override
        public RunningMode getCurrentRunningMode() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public LocalHostControllerInfo getLocalHostInfo() {
            return new LocalHostControllerInfoImpl(null, "localhost");
        }

        @Override
        public void registerRemoteHost(String hostName, ManagementChannelHandler handler, Transformers transformers, Long remoteConnectionId, DomainControllerRuntimeIgnoreTransformationEntry runtimeIgnoreTransformation) throws SlaveRegistrationException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isHostRegistered(String id) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void unregisterRemoteHost(String id, Long remoteConnectionId) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void pingRemoteHost(String hostName) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void registerRunningServer(ProxyController serverControllerClient) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void unregisterRunningServer(String serverName) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ModelNode getProfileOperations(String profileName) {
            return new ModelNode().setEmptyList();  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public HostFileRepository getLocalFileRepository() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public HostFileRepository getRemoteFileRepository() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void stopLocalHost() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void stopLocalHost(int exitCode) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ExtensionRegistry getExtensionRegistry() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ExpressionResolver getExpressionResolver() {
            return new ExpressionResolver() {
                @Override
                public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
                    return node.resolve();
                }
            };
        }

        @Override
        public void initializeMasterDomainRegistry(ManagementResourceRegistration root, ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository, HostFileRepository fileRepository, ExtensionRegistry extensionRegistry, PathManagerService pathManager) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void initializeSlaveDomainRegistry(ManagementResourceRegistration root, ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository, HostFileRepository fileRepository, LocalHostControllerInfo hostControllerInfo, ExtensionRegistry extensionRegistry, IgnoredDomainResourceRegistry ignoredDomainResourceRegistry, PathManagerService pathManager) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }


    private static class NoopTransformers implements Transformers {

        @Override
        public TransformationTarget getTarget() {
            return null;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, ModelNode operation)
                throws OperationFailedException {
            return new TransformedOperation(operation, TransformedOperation.ORIGINAL_RESULT);
        }

        @Override
        public TransformedOperation transformOperation(OperationContext operationContext, ModelNode operation)
                throws OperationFailedException {
            return new TransformedOperation(operation, TransformedOperation.ORIGINAL_RESULT);
        }

        @Override
        public Resource transformResource(ResourceTransformationContext context, Resource resource)
                throws OperationFailedException {
            return resource;
        }

        @Override
        public Resource transformRootResource(OperationContext operationContext, Resource resource)
                throws OperationFailedException {
            return resource;
        }

        @Override
        public Resource transformResource(OperationContext operationContext, PathAddress original, Resource resource, boolean skipRuntimeIgnoreCheck)
                throws OperationFailedException {
            return resource;
        }
    }


    private static HostControllerEnvironment createHostControllerEnvironment() {
        //Copied from core-model-test
        try {
            Map<String, String> props = new HashMap<String, String>();
            File home = new File("target/jbossas");
            delete(home);
            home.mkdir();
            props.put(HostControllerEnvironment.HOME_DIR, home.getAbsolutePath());

            File domain = new File(home, "domain");
            domain.mkdir();
            props.put(HostControllerEnvironment.DOMAIN_BASE_DIR, domain.getAbsolutePath());

            File configuration = new File(domain, "configuration");
            configuration.mkdir();
            props.put(HostControllerEnvironment.DOMAIN_CONFIG_DIR, configuration.getAbsolutePath());


            boolean isRestart = false;
            String modulePath = "";
            InetAddress processControllerAddress = InetAddress.getLocalHost();
            Integer processControllerPort = 9999;
            InetAddress hostControllerAddress = InetAddress.getLocalHost();
            Integer hostControllerPort = 1234;
            String defaultJVM = null;
            String domainConfig = null;
            String initialDomainConfig = null;
            String hostConfig = null;
            String initialHostConfig = null;
            RunningMode initialRunningMode = RunningMode.NORMAL;
            boolean backupDomainFiles = false;
            boolean useCachedDc = false;
            ProductConfig productConfig = new ProductConfig(null, "",  props);
            return new HostControllerEnvironment(props, isRestart, modulePath, processControllerAddress, processControllerPort,
                    hostControllerAddress, hostControllerPort, defaultJVM, domainConfig, initialDomainConfig, hostConfig, initialHostConfig,
                    initialRunningMode, backupDomainFiles, useCachedDc, productConfig);
        } catch (UnknownHostException e) {
            // AutoGenerated
            throw new RuntimeException(e);
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }

}
