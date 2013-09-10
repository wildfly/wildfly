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
package org.jboss.as.jmx.rbac;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.security.auth.Subject;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.domain.management.access.PrincipalResourceDefinition;
import org.jboss.as.domain.management.access.RoleMappingResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.jmx.JMXExtension;
import org.jboss.as.jmx.JMXSubsystemRootResource;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.test.util.AbstractControllerTestBase;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class JmxRbacTestCase extends AbstractControllerTestBase {
    volatile DelegatingConfigurableAuthorizer authorizer;
    volatile MBeanServer server;

    private static final String TEST_USER = "test";
    private static final String LAUNCH_TYPE = "launch-type";
    private static final String TYPE_STANDALONE = "STANDALONE";

    private final static ObjectName OBJECT_NAME;
    private final static ObjectName OBJECT_NAME_MODEL;
    private final static ObjectName OBJECT_NAME2;
    static {
        try {
            OBJECT_NAME = new ObjectName("test:name=bean");
            OBJECT_NAME_MODEL = new ObjectName("test:name=bean,type=model");
            OBJECT_NAME2 = new ObjectName("test:name=bean2");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean enableRbac;

    JmxRbacTestCase(final boolean enableRbac){
        this.enableRbac = enableRbac;
    }

    @Before
    public void installMBeans() throws Exception {
        server = getMBeanServer();
        server.registerMBean(new Bean(), OBJECT_NAME);
        server.registerMBean(new TestModelMBean(), OBJECT_NAME_MODEL);
    }


    @After
    public void clearDependencies() throws Exception {
        authorizer = null;

        if (server.isRegistered(OBJECT_NAME)) {
            server.unregisterMBean(OBJECT_NAME);
        }
        if (server.isRegistered(OBJECT_NAME_MODEL)) {
            server.unregisterMBean(OBJECT_NAME_MODEL);
        }
    }

    @Test
    public void testUnauthorizedSensitiveMBeans() throws Exception {
        checkMBeanAccess(null, true);
    }

    @Test
    public void testUnauthorizedNotSensitiveMBeans() throws Exception {
        checkMBeanAccess(null, false);
    }

    @Test
    public void testSuperUserSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.SUPERUSER, true);
    }

    @Test
    public void testSuperUserNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.SUPERUSER, false);
    }

    @Test
    public void testAdminstratorSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.ADMINISTRATOR, true);
    }

    @Test
    public void testAdministratorNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.ADMINISTRATOR, false);
    }

    @Test
    public void testAuditorSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.AUDITOR, true);
    }

    @Test
    public void testAuditorNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.AUDITOR, false);
    }

    @Test
    public void testDeployerSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.DEPLOYER, true);
    }

    @Test
    public void testDeployerNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.DEPLOYER, false);
    }


    @Test
    public void testMaintainerSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.MAINTAINER, true);
    }

    @Test
    public void testMaintainerNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.MAINTAINER, false);
    }

    @Test
    public void testMonitorSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.MONITOR, true);
    }

    @Test
    public void testMonitorNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.MONITOR, false);
    }

    @Test
    public void testOperatorSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.OPERATOR, true);
    }

    @Test
    public void testOperatorNonSensitiveMBeans() throws Exception {
        checkMBeanAccess(StandardRole.OPERATOR, false);
    }


    private String roleToUserName(StandardRole role) {
        return TEST_USER + "_" + role.toString();
    }

    private void checkMBeanAccess(final StandardRole standardRole, final boolean sensitiveMBeans) throws Exception {
        if (sensitiveMBeans) {
            ModelNode sensitiveMBeansOp = Util.getWriteAttributeOperation(
                    PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME),
                    JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY.getName(),
                    new ModelNode(sensitiveMBeans));
            executeForResult(sensitiveMBeansOp);
        }

        final boolean canRead = standardRole == null ? true : canRead(standardRole, sensitiveMBeans);
        final boolean canWrite = standardRole == null ? true : canWrite(standardRole, sensitiveMBeans);
        final boolean canAccessSpecial = standardRole == null ? true : canAccessSpecial(standardRole);


        Subject subject = standardRole == null ? null :
                new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    Set<ObjectInstance> instances = server.queryMBeans(null, null);
                    Set<ObjectName> names = server.queryNames(null, null);
                    int count = server.getMBeanCount();
                    Assert.assertEquals(count, names.size());
                    Assert.assertEquals(count, instances.size());

                    Assert.assertNotNull(server.getDefaultDomain());
                    Assert.assertTrue(server.getDomains().length > 0);

                    //mbean count, queryMBeans/-Names, getObjectInstance(), isRegistered()
                    if (canRead) {
                        Assert.assertTrue(names.contains(OBJECT_NAME));

                        Assert.assertNotNull(server.getObjectInstance(OBJECT_NAME));
                        Assert.assertTrue(server.isRegistered(OBJECT_NAME));
                        Assert.assertNotNull(server.getMBeanInfo(OBJECT_NAME));
                        Assert.assertTrue(server.isInstanceOf(OBJECT_NAME,BeanMBean.class.getName()));
                    } else {
                        Assert.assertFalse(names.contains(OBJECT_NAME));

                        try {
                            server.getObjectInstance(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.isRegistered(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.getMBeanInfo(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.isInstanceOf(OBJECT_NAME,BeanMBean.class.getName());
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    //Attributes
                    if (canRead) {
                        Assert.assertEquals(5, server.getAttribute(OBJECT_NAME, "Attr"));
                        server.getAttributes(OBJECT_NAME, new String[] {"Attr"});

                    } else {
                        try {
                            server.getAttribute(OBJECT_NAME, "Attr");
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.getAttributes(OBJECT_NAME, new String[] {"Attr"});
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }
                    if (canWrite) {
                        server.setAttribute(OBJECT_NAME, new Attribute("Attr", new Integer(10)));
                        server.setAttributes(OBJECT_NAME, new AttributeList(Collections.singletonList(new Attribute("Attr", new Integer(10)))));
                    } else {
                        try {
                            server.setAttribute(OBJECT_NAME, new Attribute("Attr", new Integer(10)));
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.setAttributes(OBJECT_NAME, new AttributeList(Collections.singletonList(new Attribute("Attr", new Integer(10)))));
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    //createMBean, registerMBean and unregisterMBean
                    if (canWrite) {
                        server.createMBean(Bean.class.getName(), OBJECT_NAME2);
                        server.unregisterMBean(OBJECT_NAME2);
                        server.createMBean(Bean.class.getName(), OBJECT_NAME2, new Object[0], new String[0]);
                        server.unregisterMBean(OBJECT_NAME2);
                        server.registerMBean(new Bean(), OBJECT_NAME2);
                        server.unregisterMBean(OBJECT_NAME2);

                    } else {
                        try {
                            server.createMBean(Bean.class.getName(), OBJECT_NAME2);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.createMBean(Bean.class.getName(), OBJECT_NAME2, new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.registerMBean(new Bean(), OBJECT_NAME2);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.unregisterMBean(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    //Notification listeners
                    final TestNotificationListener listener = new TestNotificationListener();
                    if (canRead) {
                        server.addNotificationListener(OBJECT_NAME, listener, listener, new Object());
                        server.removeNotificationListener(OBJECT_NAME, listener);
                    } else {
                        try {
                            server.addNotificationListener(OBJECT_NAME, listener, listener, new Object());
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.removeNotificationListener(OBJECT_NAME, listener);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    //Special methods, which only superuser or adminstrator can call
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bout);
                    try {
                        out.writeObject(new Bean());
                    } finally {
                        IoUtils.safeClose(out);
                    }
                    byte[] bytes = bout.toByteArray();

                    if (canAccessSpecial) {
                        Assert.assertNotNull(server.deserialize(OBJECT_NAME, bytes));
                        Assert.assertNotNull(server.deserialize(Bean.class.getName(), bytes));
                        try {
                            server.getClassLoader(OBJECT_NAME);
                        } catch (InstanceNotFoundException expected) {
                        }
                        Assert.assertNotNull(server.getClassLoaderRepository());
                        Assert.assertNotNull(server.getClassLoaderFor(OBJECT_NAME));
                        Assert.assertNotNull(server.instantiate(Bean.class.getName()));
                        Assert.assertNotNull(server.instantiate(Bean.class.getName(), new Object[0], new String[0]));

                    } else {
                        try {
                            Assert.assertNotNull(server.deserialize(OBJECT_NAME, bytes));
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            Assert.assertNotNull(server.deserialize(Bean.class.getName(), bytes));
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.getClassLoader(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.getClassLoaderRepository();
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.getClassLoaderFor(OBJECT_NAME);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.instantiate(Bean.class.getName());
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.instantiate(Bean.class.getName(), new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    //invoke
                    if (canRead) {
                        server.invoke(OBJECT_NAME_MODEL, "info", new Object[0], new String[0]);
                    } else {
                        try {
                            server.invoke(OBJECT_NAME_MODEL, "info", new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }
                    if (canWrite) {
                        server.invoke(OBJECT_NAME, "method", new Object[0], new String[0]);
                        server.invoke(OBJECT_NAME_MODEL, "action", new Object[0], new String[0]);
                        server.invoke(OBJECT_NAME_MODEL, "actionInfo", new Object[0], new String[0]);
                        server.invoke(OBJECT_NAME_MODEL, "unknown", new Object[0], new String[0]);
                    } else {
                        try {
                            server.invoke(OBJECT_NAME, "method", new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.invoke(OBJECT_NAME_MODEL, "action", new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.invoke(OBJECT_NAME_MODEL, "actionInfo", new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                        try {
                            server.invoke(OBJECT_NAME_MODEL, "unknown", new Object[0], new String[0]);
                            Assert.fail();
                        } catch (RuntimeMBeanException expected) {
                        }
                    }

                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract boolean canRead(StandardRole standardRole, boolean sensitiveMBeans);
    protected abstract boolean canWrite(StandardRole standardRole, boolean sensitiveMBeans);
    protected abstract boolean canAccessSpecial(StandardRole standardRole);

    @Override
    protected DelegatingConfigurableAuthorizer getAuthorizer() {
        if (authorizer == null) {
            authorizer = new DelegatingConfigurableAuthorizer();
        }
        return authorizer;
    }


    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) {
        if (enableRbac) {
            for (StandardRole standardRole : EnumSet.allOf(StandardRole.class)) {
                ModelNode addRoleMappingOp = Util.createAddOperation(
                        PathAddress.pathAddress(
                                CoreManagementResourceDefinition.PATH_ELEMENT,
                                AccessAuthorizationResourceDefinition.PATH_ELEMENT,
                                PathElement.pathElement(RoleMappingResourceDefinition.PATH_KEY, standardRole.toString())));
                bootOperations.add(addRoleMappingOp);

                ModelNode addIncludeUserOp = Util.createAddOperation(
                        PathAddress.pathAddress(
                            CoreManagementResourceDefinition.PATH_ELEMENT,
                            AccessAuthorizationResourceDefinition.PATH_ELEMENT,
                            PathElement.pathElement(RoleMappingResourceDefinition.PATH_KEY, standardRole.toString()),
                            PathElement.pathElement(ModelDescriptionConstants.INCLUDE, "user-" + roleToUserName(standardRole))));
                addIncludeUserOp.get(PrincipalResourceDefinition.NAME.getName()).set(roleToUserName(standardRole));
                addIncludeUserOp.get(PrincipalResourceDefinition.TYPE.getName()).set(PrincipalResourceDefinition.Type.USER.toString());
                bootOperations.add(addIncludeUserOp);
            }

            ModelNode enableRbacOp = Util.getWriteAttributeOperation(
                    PathAddress.pathAddress(
                            CoreManagementResourceDefinition.PATH_ELEMENT,
                            AccessAuthorizationResourceDefinition.PATH_ELEMENT),
                    AccessAuthorizationResourceDefinition.PROVIDER.getName(),
                    new ModelNode(AccessAuthorizationResourceDefinition.Provider.RBAC.toString()));
            bootOperations.add(enableRbacOp);
        }


        ModelNode addOp = Util.createAddOperation(PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME));
        bootOperations.add(addOp);
    }

    private MBeanServer getMBeanServer() throws Exception {
        ServiceController controller = getContainer().getRequiredService(MBeanServerService.SERVICE_NAME);
        return (PluggableMBeanServer)controller.getValue();
    }

    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        PathManagerService pathManagerService = new PathManagerService() {
        };
        GlobalOperationHandlers.registerGlobalOperations(registration, processType);
        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);

        registration.registerReadOnlyAttribute(LAUNCH_TYPE, new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(TYPE_STANDALONE);
                context.stepCompleted();
            }
        }, AttributeAccess.Storage.RUNTIME);

        TestServiceListener listener = new TestServiceListener();
        listener.reset(1);
        getContainer().addService(PathManagerService.SERVICE_NAME, pathManagerService)
                .addListener(listener)
                .install();

        try {
            listener.latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        registration.registerSubModel(PathResourceDefinition.createSpecified(pathManagerService));
        registration.registerSubModel(CoreManagementResourceDefinition.forStandaloneServer(getAuthorizer(), getAuditLogger(), pathManagerService, new EnvironmentNameReader() {
            public boolean isServer() {
                return true;
            }

            public String getServerName() {
                return "Test";
            }

            public String getHostName() {
                return null;
            }

            public String getProductName() {
                return null;
            }
        }));


        pathManagerService.addPathManagerResources(rootResource);


        ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL), AuditLogger.NO_OP_LOGGER, getAuthorizer());
        extensionRegistry.setPathManager(pathManagerService);
        extensionRegistry.setWriterRegistry(new NullConfigurationPersister());
        extensionRegistry.setSubsystemParentResourceRegistrations(registration, null);
        JMXExtension extension = new JMXExtension();
        extension.initialize(extensionRegistry.getExtensionContext("xxxx", false));

        Resource coreManagementResource = Resource.Factory.create();
        rootResource.registerChild(CoreManagementResourceDefinition.PATH_ELEMENT, coreManagementResource);

        Resource accessAuthorizationResource = Resource.Factory.create();
        accessAuthorizationResource.getModel().get(AccessAuthorizationResourceDefinition.PROVIDER.getName()).set(AccessAuthorizationResourceDefinition.Provider.SIMPLE.toString());
        coreManagementResource.registerChild(AccessAuthorizationResourceDefinition.PATH_ELEMENT, accessAuthorizationResource);
    }


    private class TestServiceListener extends AbstractServiceListener<Object> {

        volatile CountDownLatch latch;
        Map<ServiceController.Transition, ServiceName> services = Collections.synchronizedMap(new LinkedHashMap<ServiceController.Transition, ServiceName>());


        void reset(int count) {
            latch = new CountDownLatch(count);
            services.clear();
        }

        public void transition(ServiceController<? extends Object> controller, ServiceController.Transition transition) {
            if (transition == ServiceController.Transition.STARTING_to_UP || transition == ServiceController.Transition.REMOVING_to_REMOVED) {
                services.put(transition, controller.getName());
                latch.countDown();
            }
        }
    }

    private static class TestNotificationListener implements NotificationListener, NotificationFilter {

        private static final long serialVersionUID = 1L;

        @Override
        public void handleNotification(Notification notification, Object handback) {
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return false;
        }
    }


}
