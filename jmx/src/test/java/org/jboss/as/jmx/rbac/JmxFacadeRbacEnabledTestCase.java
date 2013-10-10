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

import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.management.JMRuntimeException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.security.auth.Subject;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.VaultExpressionSensitivityConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.domain.management.access.PrincipalResourceDefinition;
import org.jboss.as.domain.management.access.RoleMappingResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.domain.management.security.RealmUser;
import org.jboss.as.jmx.ExposeModelResourceResolved;
import org.jboss.as.jmx.JMXExtension;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.test.util.AbstractControllerTestBase;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JmxFacadeRbacEnabledTestCase extends AbstractControllerTestBase {
    volatile DelegatingConfigurableAuthorizer authorizer;
    volatile MBeanServer server;

    private static final String TEST_USER = "test";
    private static final String LAUNCH_TYPE = "launch-type";
    private static final String TYPE_STANDALONE = "STANDALONE";

    private static final PathElement ONE = PathElement.pathElement("one");
    private static final PathElement ONE_A = PathElement.pathElement("one", "a");
    private static final PathElement ONE_B = PathElement.pathElement("one", "b");

    private static final ObjectName ROOT_NAME;
    private static final ObjectName ONE_A_NAME;
    private static final ObjectName ONE_B_NAME;
    static {
        try {
            ROOT_NAME = new ObjectName("jboss.as:management-root=server");
            ONE_A_NAME = new ObjectName("jboss.as:one=a");
            ONE_B_NAME = new ObjectName("jboss.as:one=b");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Resource rootResource;

    public JmxFacadeRbacEnabledTestCase(){
    }

    @Before
    public void installMBeans() throws Exception {
        server = getMBeanServer();
    }


    @After
    public void clearDependencies() throws Exception {
        authorizer = null;
    }

    //These three are the same for different roles
    @Test
    public void testNotSensitiveResourceAsMonitor() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.MONITOR, true, true, false, true);
    }

    @Test
    public void testNotSensitiveResourceAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.MAINTAINER, true, true, true, true);
    }

    @Test
    public void testNotSensitiveResourceAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.ADMINISTRATOR, true, true, true, true);
    }

    //These three are the same for different roles
    @Test
    public void testSensitiveAddressableResourceAsMonitor() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.MONITOR, false, false, false, false, createSensitivityConstraint("testSensitiveAddressableResourceAsMonitor", true, false, false));
    }

    @Test
    public void testSensitiveAddressableResourceAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.MAINTAINER, false, false, true, true, createSensitivityConstraint("testSensitiveAddressableResourceAsMaintainer", true, false, false));
    }

    @Test
    public void testSensitiveAddressableResourceAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveResource(StandardRole.ADMINISTRATOR, true, true, true, true, createSensitivityConstraint("testSensitiveAddressableResourceAsAdministrator", true, false, false));
    }

    private void checkMBeanAccessSensitiveResource(final StandardRole standardRole, final boolean addressable, final boolean readable,
            final boolean writable, final boolean executable, final SensitiveTargetAccessConstraintDefinition...sensitivityConstraints) throws Exception {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE, sensitivityConstraints);
        oneChild.addAttribute("attr1");
        oneChild.addOperation("test", true, false, null);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_A, resourceA);
        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_B, resourceB);

        checkMBeanAccess(standardRole, addressable, readable, writable, executable);
    }

    //These three are the same for different roles
    /*
     Having read denied and not write does not make sense. However read-resource-description returns these in access-control as read=false,write=true, but then actually trying to execute the operation
     fails
    @Test
    public void testSensitiveReadAttributeAsMonitor() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MONITOR, true, false, false, true, createSensitivityConstraint("testSensitiveReadAttributeAsMonitor", false, true, false));
    }

    @Test
    public void testSensitiveReadAttributeAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MAINTAINER, true, false, true, true, createSensitivityConstraint("testSensitiveReadAttributeAsMaintainer", false, true, false));
    }

    @Test
    public void testSensitiveReadAttributeAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.ADMINISTRATOR, true, true, true, true, createSensitivityConstraint("testSensitiveReadAttributeAsAdministrator", false, true, false));
    }
    */

    //These three are the same for different roles
    @Test
    public void testSensitiveWriteAttributeAsMonitor() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MONITOR, true, true, false, true, createSensitivityConstraint("testSensitiveWriteAttributeAsAdministrator", false, false, true));
    }


    @Test
    public void testSensitiveWriteAttributeAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MAINTAINER, true, true, false, true, createSensitivityConstraint("testSensitiveWriteAttributeAsAdministrator", false, false, true));
    }

    @Test
    public void testSensitiveWriteAttributeAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.ADMINISTRATOR, true, true, true, true, createSensitivityConstraint("testSensitiveWriteAttributeAsAdministrator", false, false, true));
    }

    //These three are the same for different roles
    @Test
    public void testSensitiveReadWriteAttributeAsMonitor() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MONITOR, true, false, false, true, createSensitivityConstraint("testSensitiveReadWriteAttributeAsMonitor", false, true, true));
    }

    @Test
    public void testSensitiveReadWriteAttributeAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.MAINTAINER, true, false, false, true, createSensitivityConstraint("testSensitiveReadWriteAttributeAsMaintainer", false, true, true));
    }

    @Test
    public void testSensitiveReadWriteAttributeAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveAttribute(StandardRole.ADMINISTRATOR, true, true, true, true, createSensitivityConstraint("testSensitiveReadWriteAttributeAsAdministrator", false, true, true));
    }

    private void checkMBeanAccessSensitiveAttribute(final StandardRole standardRole, final boolean addressable, final boolean readable,
            final boolean writable, final boolean executable, final SensitiveTargetAccessConstraintDefinition...sensitivityConstraints) throws Exception {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addAttribute("attr1", sensitivityConstraints);
        oneChild.addOperation("test", true, false, null);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_A, resourceA);
        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_B, resourceB);

        checkMBeanAccess(standardRole, addressable, readable, writable, executable);
    }

    //These three are the same for different roles
    @Test
    public void testSensitiveOperationAsMonitor() throws Exception {
        checkMBeanAccessSensitiveOperation(StandardRole.MONITOR, true, true, false, false, createSensitivityConstraint("testSensitiveOperationAsMonitor", false, true, true));
    }

    @Test
    public void testSensitiveOperationAsMaintainer() throws Exception {
        checkMBeanAccessSensitiveOperation(StandardRole.MAINTAINER, true, true, true, false, createSensitivityConstraint("testSensitiveOperationAsMaintainer", false, true, true));
    }

    @Test
    public void testSensitiveOperationAsAdministrator() throws Exception {
        checkMBeanAccessSensitiveOperation(StandardRole.ADMINISTRATOR, true, true, true, true, createSensitivityConstraint("testSensitiveOperationAsAdministrator", false, true, true));
    }

    private void checkMBeanAccessSensitiveOperation(final StandardRole standardRole, final boolean addressable, final boolean readable,
            final boolean writable, final boolean executable, final SensitiveTargetAccessConstraintDefinition...sensitivityConstraints) throws Exception {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addAttribute("attr1");
        oneChild.addOperation("test", true, false, null, sensitivityConstraints);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_A, resourceA);
        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_B, resourceB);

        checkMBeanAccess(standardRole, addressable, readable, writable, executable);
    }

    private void checkMBeanAccess(final StandardRole standardRole, final boolean addressable, final boolean readable, final boolean writable, final boolean executable) throws Exception {
        Subject subject = standardRole == null ? null :
            new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                Set<ObjectName> names = server.queryNames(null, null);
                Assert.assertEquals(addressable, names.contains(ONE_A_NAME));
                Assert.assertEquals(addressable, names.contains(ONE_B_NAME));

                Set<ObjectInstance> instances = server.queryMBeans(null, null);
                Assert.assertEquals(names.size(), instances.size());
                for (ObjectInstance instance : instances) {
                    Assert.assertTrue(names.contains(instance.getObjectName()));
                }
                int number = server.getMBeanCount();
                Assert.assertEquals(names.size(), number);

                try {
                    server.getMBeanInfo(ONE_A_NAME);
                    Assert.assertTrue(addressable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                }

                try {
                    server.getObjectInstance(ONE_A_NAME);
                    Assert.assertTrue(addressable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                }

                try {
                    server.getAttribute(ONE_A_NAME, "attr1");
                    Assert.assertTrue(addressable);
                    Assert.assertTrue(readable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                } catch (JMRuntimeException e) {
                    Assert.assertTrue(addressable);
                    Assert.assertFalse(readable);
                }

                try {
                    server.getAttributes(ONE_B_NAME, new String[] {"attr1"});
                    Assert.assertTrue(addressable);
                    Assert.assertTrue(readable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                } catch (JMRuntimeException e) {
                    Assert.assertTrue(addressable);
                    Assert.assertFalse(readable);
                }

                try {
                    server.setAttribute(ONE_A_NAME, new Attribute("attr1", "Test"));
                    Assert.assertTrue(addressable);
                    Assert.assertTrue(writable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                } catch (JMRuntimeException e) {
                    Assert.assertTrue(addressable);
                    Assert.assertFalse(writable);
                }

                try {
                    server.setAttributes(ONE_A_NAME, new AttributeList(Collections.singletonList(new Attribute("attr1", "Test"))));
                    Assert.assertTrue(addressable);
                    Assert.assertTrue(writable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                } catch (JMRuntimeException e) {
                    Assert.assertTrue(addressable);
                    Assert.assertFalse(writable);
                }

                try {
                    server.invoke(ONE_A_NAME, "test", new Object[0], new String[0]);
                    Assert.assertTrue(addressable);
                    Assert.assertTrue(executable);
                } catch (InstanceNotFoundException e) {
                    Assert.assertFalse(addressable);
                } catch (JMRuntimeException e) {
                    Assert.assertTrue(addressable);
                    Assert.assertFalse(executable);
                }
                return null;
            }
        });
    }

    //These three are the same for different roles
    @Test
    public void testAddAccessSensitiveWildcardResourceAsMonitor() throws Exception {
        checkAddSensitive(StandardRole.MONITOR, true, false, createSensitivityConstraint("testAddAccessSensitiveWildcardResourceAsMonitor", true, true, true));
    }

    @Test
    public void testAddAccessSensitiveWildcardResourceAsMaintainer() throws Exception {
        checkAddSensitive(StandardRole.MAINTAINER, true, false, createSensitivityConstraint("testAddAccessSensitiveWildcardResourceAsMaintainer", true, true, true));
    }

    @Test
    public void testAddAccessSensitiveWildcardResourceAsAdministrator() throws Exception {
        checkAddSensitive(StandardRole.ADMINISTRATOR, true, true, createSensitivityConstraint("testAddAccessSensitiveWildcardResourceAsAdministrator", true, true, true));
    }

    //These three are the same for different roles
    @Test
    public void testAddWriteSensitiveWildcardResourceAsMonitor() throws Exception {
        checkAddSensitive(StandardRole.MONITOR, true, false, createSensitivityConstraint("testAddWriteSensitiveWildcardResourceAsMonitor", false, true, true));
    }

    @Test
    public void testAddWrieSensitiveWildcardResourceAsMaintainer() throws Exception {
        checkAddSensitive(StandardRole.MAINTAINER, true, false, createSensitivityConstraint("testAddWrieSensitiveWildcardResourceAsMaintainer", false, true, true));
    }

    @Test
    public void testAddWriteSensitiveWildcardResourceAsAdministrator() throws Exception {
        checkAddSensitive(StandardRole.ADMINISTRATOR, true, true, createSensitivityConstraint("testAddWriteSensitiveWildcardResourceAsAdministrator", false, true, true));
    }

    //These three are the same for different roles
    @Test
    public void testAddAccessSensitiveFixedResourceAsMonitor() throws Exception {
        checkAddSensitive(StandardRole.MONITOR, false, false, createSensitivityConstraint("testAddAccessSensitiveFixedResourceAsMonitor", true, true, true));
    }

    @Test
    public void testAddAccessSensitiveFixedResourceAsMaintainer() throws Exception {
        checkAddSensitive(StandardRole.MAINTAINER, false, false, createSensitivityConstraint("testAddAccessSensitiveFixedResourceAsMaintainer", true, true, true));
    }

    @Test
    public void testAddAccessSensitiveFixedResourceAsAdministrator() throws Exception {
        checkAddSensitive(StandardRole.ADMINISTRATOR, false, true, createSensitivityConstraint("testAddAccessSensitiveFixedResourceAsAdministrator", true, true, true));
    }

    //These three are the same for different roles
    @Test
    public void testAddWriteSensitiveFixedResourceAsMonitor() throws Exception {
        checkAddSensitive(StandardRole.MONITOR, false, false, createSensitivityConstraint("testAddWriteSensitiveFixedResourceAsMonitor", false, true, true));
    }

    @Test
    public void testAddWriteSensitiveFixedResourceAsMaintainer() throws Exception {
        checkAddSensitive(StandardRole.MAINTAINER, false, false, createSensitivityConstraint("testAddWriteSensitiveFixedResourceAsMaintainer", false, true, true));
    }

    @Test
    public void testAddWriteSensitiveFixedResourceAsAdministrator() throws Exception {
        checkAddSensitive(StandardRole.ADMINISTRATOR, false, true, createSensitivityConstraint("testAddWriteSensitiveFixedResourceAsAdministrator", false, true, true));
    }

    private void checkAddSensitive(final StandardRole standardRole, final boolean wildcard, final boolean  executable, final SensitiveTargetAccessConstraintDefinition...sensitivityConstraints) throws Exception {
        PathElement pathElement = wildcard ? ONE : ONE_A;
        ChildResourceDefinition oneChild = new ChildResourceDefinition(pathElement, sensitivityConstraints);
        oneChild.addAttribute("attr1");
        oneChild.addOperation("test", true, false, null);
        rootRegistration.registerSubModel(oneChild);

        Subject subject = standardRole == null ? null :
            new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                Assert.assertFalse(server.queryNames(null, null).contains(ONE_A_NAME));
                try {
                    String add = wildcard ? "addOne" : "addOneA";
                    Object[] params = wildcard ? new String[]{"a", "test"} : new String[]{"test"};
                    String[] sig = wildcard ? new String[] {String.class.getName(), String.class.getName()} : new String[] {String.class.getName()};
                    server.invoke(ROOT_NAME, add, params, sig);
                    Assert.assertTrue(executable);
                    Assert.assertTrue(server.queryNames(null, null).contains(ONE_A_NAME));
                } catch (JMRuntimeException e) {
                    Assert.assertFalse(executable);
                }

                return null;
            }
        });
    }

    @Test
    public void testReadVaultExpressionsNoReadAsMonitor() throws Exception {
        checkReadVaultExpressionReadSensitive(StandardRole.MONITOR, false);
    }

    @Test
    public void testReadVaultExpressionsNoReadAsMaintainer() throws Exception {
        checkReadVaultExpressionReadSensitive(StandardRole.MAINTAINER, false);
    }

    @Test
    public void testReadVaultExpressionsNoReadAsAdministrator() throws Exception {
        checkReadVaultExpressionReadSensitive(StandardRole.ADMINISTRATOR, true);
    }

    private void checkReadVaultExpressionReadSensitive(final StandardRole standardRole, final boolean readable) throws Exception {
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(false);
        try {
            ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
            oneChild.addAttribute("attr1");
            oneChild.addOperation("test", true, false, null);
            rootRegistration.registerSubModel(oneChild);
            Resource resourceA = Resource.Factory.create();
            resourceA.getModel().get("attr1").set("test-a");
            rootResource.registerChild(ONE_A, resourceA);
            Resource resourceB = Resource.Factory.create();
            resourceB.getModel().get("attr1").set("${VAULT::AA::bb::cc}");
            rootResource.registerChild(ONE_B, resourceB);

            Subject subject = standardRole == null ? null :
                new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    Assert.assertEquals("test-a", server.getAttribute(ONE_A_NAME, "attr1"));
                    try {
                        Assert.assertEquals("${VAULT::AA::bb::cc}", server.getAttribute(ONE_B_NAME, "attr1"));
                        Assert.assertTrue(readable);
                    } catch (JMRuntimeException e) {
                        Assert.assertFalse(readable);
                    }

                    return null;
                }
            });
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testWriteVaultExpressionsWriteSensitiveAsMonitor() throws Exception {
        checkWriteVaultExpressionWriteSensitive(StandardRole.MONITOR, false);
    }

    @Test
    public void testWriteVaultExpressionsWriteSensitiveAsMaintainer() throws Exception {
        checkWriteVaultExpressionWriteSensitive(StandardRole.MAINTAINER, false);
    }

    @Test
    public void testWriteVaultExpressionsWriteSensitiveAsAdministrator() throws Exception {
        checkWriteVaultExpressionWriteSensitive(StandardRole.ADMINISTRATOR, true);
    }

    private void checkWriteVaultExpressionWriteSensitive(final StandardRole standardRole, final boolean writable) throws Exception {
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
            oneChild.addAttribute("attr1");
            oneChild.addOperation("test", true, false, null);
            rootRegistration.registerSubModel(oneChild);
            Resource resourceA = Resource.Factory.create();
            resourceA.getModel().get("attr1").set("test-a");
            rootResource.registerChild(ONE_A, resourceA);

            Subject subject = standardRole == null ? null :
                new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    try {
                        server.setAttribute(ONE_A_NAME, new Attribute("attr1", "${VAULT::AA::bb::cc}"));
                        Assert.assertTrue(writable);
                    } catch (JMRuntimeException e) {
                        Assert.assertFalse(writable);
                    }

                    return null;
                }
            });
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testOperationVaultExpressionWriteSensitiveAsMonitor() throws Exception {
        checkOperationVaultExpressionWriteSensitive(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testOperationVaultExpressionWriteSensitiveAsMaintainer() throws Exception {
        checkOperationVaultExpressionWriteSensitive(StandardRole.MAINTAINER, true, false);
    }

    @Test
    public void testOperationVaultExpressionWriteSensitiveAsAdministrator() throws Exception {
        checkOperationVaultExpressionWriteSensitive(StandardRole.ADMINISTRATOR, true, true);
    }

    private void checkOperationVaultExpressionWriteSensitive(final StandardRole standardRole, final boolean executable, final boolean executableVault) throws Exception {
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
            oneChild.addAttribute("attr1");
            oneChild.addOperation("test", false, false, new SimpleAttributeDefinition[] {
                    new SimpleAttributeDefinitionBuilder("param1", ModelType.STRING)
                    .setAllowExpression(true)
                    .build()
            });
            rootRegistration.registerSubModel(oneChild);
            Resource resourceA = Resource.Factory.create();
            resourceA.getModel().get("attr1").set("test-a");
            rootResource.registerChild(ONE_A, resourceA);

            Subject subject = standardRole == null ? null :
                new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    try {
                        server.invoke(ONE_A_NAME, "test", new Object[] {"stuff"}, new String[] {String.class.getName()});
                        Assert.assertTrue(executable);
                    } catch (JMRuntimeException e) {
                        Assert.assertFalse(executable);
                    }
                    try {
                        server.invoke(ONE_A_NAME, "test", new Object[] {"${VAULT::AA::bb::cc}"}, new String[] {String.class.getName()});
                        Assert.assertTrue(executableVault);
                    } catch (JMRuntimeException e) {
                        Assert.assertFalse(executableVault);
                    }
                    return null;
                }
            });
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    //These three are the same for different roles
    @Test
    public void testAddVaultWriteSensitiveFixedResourceAsMonitor() throws Exception {
        checkAddVaultSensitive(StandardRole.MONITOR, false);
    }

    @Test
    public void testAddVaultWriteSensitiveFixedResourceAsMaintainer() throws Exception {
        checkAddVaultSensitive(StandardRole.MAINTAINER, false);
    }

    @Test
    public void testAddVaultWriteSensitiveFixedResourceAsAdministrator() throws Exception {
        checkAddVaultSensitive(StandardRole.ADMINISTRATOR, true);
    }

    private void checkAddVaultSensitive(final StandardRole standardRole, final boolean  executable) throws Exception {
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {

            PathElement pathElement = ONE_A;
            ChildResourceDefinition oneChild = new ChildResourceDefinition(pathElement);
            oneChild.addAttribute("attr1");
            oneChild.addOperation("test", true, false, null);
            rootRegistration.registerSubModel(oneChild);

            Subject subject = standardRole == null ? null :
                new Subject(true, Collections.singleton(new RealmUser(roleToUserName(standardRole))), Collections.emptySet(), Collections.emptySet());
            Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    Assert.assertFalse(server.queryNames(null, null).contains(ONE_A_NAME));
                    try {
                        String add = "addOneA";
                        Object[] params = new String[]{"${VAULT::AA::bb::cc}"};
                        String[] sig = new String[] {String.class.getName()};
                        server.invoke(ROOT_NAME, add, params, sig);
                        Assert.assertTrue(executable);
                        Assert.assertTrue(server.queryNames(null, null).contains(ONE_A_NAME));
                    } catch (JMRuntimeException e) {
                        Assert.assertFalse(executable);
                    }

                    return null;
                }
            });
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        }
    }

    private SensitiveTargetAccessConstraintDefinition createSensitivityConstraint(String name, boolean access, boolean read, boolean write) {
        SensitivityClassification classification = new SensitivityClassification("test", name, access, read, write);
        return new SensitiveTargetAccessConstraintDefinition(classification);
    }

    private MBeanServer getMBeanServer() throws Exception {
        ServiceController<?> controller = getContainer().getRequiredService(MBeanServerService.SERVICE_NAME);
        return (PluggableMBeanServer)controller.getValue();
    }

    @Override
    protected DelegatingConfigurableAuthorizer getAuthorizer() {
        if (authorizer == null) {
            authorizer = new DelegatingConfigurableAuthorizer();
        }
        return authorizer;
    }

    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) {
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


        PathAddress subystemAddress = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME);
        bootOperations.add(Util.createAddOperation(subystemAddress));
        bootOperations.add(Util.createAddOperation(subystemAddress.append(ExposeModelResourceResolved.PATH_ELEMENT)));
    }

    private String roleToUserName(StandardRole role) {
        return TEST_USER + "_" + role.toString();
    }


    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        this.rootResource = rootResource;
        this.rootRegistration = registration;
        PathManagerService pathManagerService = new PathManagerService() {
        };
        GlobalOperationHandlers.registerGlobalOperations(registration, processType);
        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);
        registration.registerOperationHandler(RootResourceHack.DEFINITION, RootResourceHack.INSTANCE);

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

    private static class TestResourceDefinition extends SimpleResourceDefinition {
        TestResourceDefinition(PathElement pathElement) {
            super(pathElement,
                    new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler() {},
                    new AbstractRemoveStepHandler() {});
        }
    }

    private static class ChildResourceDefinition extends TestResourceDefinition implements ConstrainedResourceDefinition {
        private final List<AccessConstraintDefinition> constraints;
        private final List<AttributeDefinition> attributes = Collections.synchronizedList(new ArrayList<AttributeDefinition>());
        private final List<AttributeDefinition> readOnlyAttributes = Collections.synchronizedList(new ArrayList<AttributeDefinition>());
        private final List<OperationDefinition> operations = Collections.synchronizedList(new ArrayList<OperationDefinition>());

        ChildResourceDefinition(PathElement element, AccessConstraintDefinition...constraints){
            super(element);
            this.constraints = Collections.unmodifiableList(Arrays.asList(constraints));
        }

        void addAttribute(String name, AccessConstraintDefinition...constraints) {
            SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING);
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            builder.setAllowExpression(true);
            attributes.add(builder.build());
        }

        void addOperation(String name, boolean readOnly, boolean runtimeOnly, SimpleAttributeDefinition[] parameters, AccessConstraintDefinition...constraints) {
            SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, new NonResolvingResourceDescriptionResolver());
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            if (readOnly) {
                builder.setReadOnly();
            }
            if (runtimeOnly) {
                builder.setRuntimeOnly();
            }
            if (parameters != null) {
                for (SimpleAttributeDefinition param : parameters) {
                    builder.addParameter(param);
                }
            }
            operations.add(builder.build());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            for (AttributeDefinition attribute : attributes) {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new ModelOnlyWriteAttributeHandler(attribute));
            }
            for (AttributeDefinition attribute : readOnlyAttributes) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            }
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            for (OperationDefinition op : operations) {
                if (op.getFlags().contains(Flag.READ_ONLY)) {
                    resourceRegistration.registerOperationHandler(op, TestOperationStepHandler.READ_ONLY_INSTANCE);
                } else {
                    resourceRegistration.registerOperationHandler(op, TestOperationStepHandler.READ_WRITE_INSTANCE);
                }

            }
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraints;
        }
    }

    private static class TestOperationStepHandler implements OperationStepHandler {
        static final TestOperationStepHandler READ_ONLY_INSTANCE = new TestOperationStepHandler(true);
        static final TestOperationStepHandler READ_WRITE_INSTANCE = new TestOperationStepHandler(false);

        private final boolean readOnly;

        public TestOperationStepHandler(boolean readOnly) {
            this.readOnly = readOnly;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (readOnly) {
                context.readResource(PathAddress.EMPTY_ADDRESS);
            } else {
                context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            }
            context.stepCompleted();
        }
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
}
