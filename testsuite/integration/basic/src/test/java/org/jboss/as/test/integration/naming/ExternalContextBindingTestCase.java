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

package org.jboss.as.test.integration.naming;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CACHE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.ENVIRONMENT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.sun.jndi.ldap.LdapCtx;
import com.sun.jndi.ldap.LdapCtxFactory;

/**
 * Test for external context binding. There are tests which use a usual InitialContext and treat it
 * as an external context and tests which connect to an actual external LDAP server.
 * @author Stuart Douglas, Jan Martiska
 */
@RunWith(Arquillian.class)
@ServerSetup({ExternalContextBindingTestCase.ObjectFactoryWithEnvironmentBindingTestCaseServerSetup.class,
        ExternalContextBindingTestCase.PrepareExternalLDAPServerSetup.class})
public class ExternalContextBindingTestCase {

    private static Logger LOGGER = Logger.getLogger(ExternalContextBindingTestCase.class);

    private static final String MODULE_NAME = "org.jboss.as.naming";

    public static final int LDAP_PORT = 10389;

    static class ObjectFactoryWithEnvironmentBindingTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId)
                throws Exception {

            // bind the object factory
            ModelNode address = createAddress("nocache");
            ModelNode bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CLASS).set(InitialContext.class.getName());
            ModelNode addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(),
                    addResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Object factory bound.");

            address = createAddress("cache");
            bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CACHE).set(true);
            bindingAdd.get(CLASS).set(InitialContext.class.getName());
            addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(),
                    addResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Object factory bound.");

            address = createAddress("ldap");
            bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CLASS).set(InitialDirContext.class.getName());
            bindingAdd.get(ENVIRONMENT).add("java.naming.provider.url", "ldap://"+managementClient.getMgmtAddress()+":"+
                    ExternalContextBindingTestCase.LDAP_PORT);
            bindingAdd.get(ENVIRONMENT).add("java.naming.factory.initial", LdapCtxFactory.class.getName());
            bindingAdd.get(ENVIRONMENT).add(Context.SECURITY_AUTHENTICATION, "simple");
            bindingAdd.get(ENVIRONMENT)
                    .add(Context.SECURITY_PRINCIPAL, "uid=jduke,ou=People,dc=jboss,dc=org");
            bindingAdd.get(ENVIRONMENT).add(Context.SECURITY_CREDENTIALS, "theduke");
            addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(),
                    addResult.get(FAILURE_DESCRIPTION).isDefined());

            address = createAddress("ldap-cache");
            bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CLASS).set(InitialDirContext.class.getName());
            bindingAdd.get(CACHE).set(true);
            bindingAdd.get(ENVIRONMENT).add("java.naming.provider.url", "ldap://"+managementClient.getMgmtAddress()+":"+
                    ExternalContextBindingTestCase.LDAP_PORT);
            bindingAdd.get(ENVIRONMENT).add("java.naming.factory.initial", LdapCtxFactory.class.getName());
            bindingAdd.get(ENVIRONMENT).add(Context.SECURITY_AUTHENTICATION, "simple");
            bindingAdd.get(ENVIRONMENT)
                    .add(Context.SECURITY_PRINCIPAL, "uid=jduke,ou=People,dc=jboss,dc=org");
            bindingAdd.get(ENVIRONMENT).add(Context.SECURITY_CREDENTIALS, "theduke");
            addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(),
                    addResult.get(FAILURE_DESCRIPTION).isDefined());

        }

        private ModelNode createAddress(String part) {
            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
            address.add(BINDING, "java:global/" + part);
            return address;
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId)
                throws Exception {
            // unbind the object factorys
            ModelNode bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("nocache"));
            bindingRemove.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ModelNode removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(),
                    removeResult.get(FAILURE_DESCRIPTION)
                            .isDefined());
            LOGGER.info("Object factory with uncached InitialContext unbound.");

            bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("cache"));
            bindingRemove.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(),
                    removeResult.get(FAILURE_DESCRIPTION)
                            .isDefined());
            LOGGER.info("Object factory with cached InitialContext unbound.");

            bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("ldap"));
            bindingRemove.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(),
                    removeResult.get(FAILURE_DESCRIPTION)
                            .isDefined());
            LOGGER.info("Object factory with uncached InitialDirContext unbound.");

            bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("ldap-cache"));
            bindingRemove.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(),
                    removeResult.get(FAILURE_DESCRIPTION)
                            .isDefined());
            LOGGER.info("Object factory with cached InitialDirContext unbound.");
        }
    }



    //@formatter:off
    @CreateDS(
            name = "JBossDS",
            partitions =
                    {
                            @CreatePartition(
                                    name = "jboss",
                                    suffix = "dc=jboss,dc=org",
                                    contextEntry = @ContextEntry(
                                            entryLdif =
                                                    "dn: dc=jboss,dc=org\n" +
                                                            "dc: jboss\n" +
                                                            "objectClass: top\n" +
                                                            "objectClass: domain\n\n"))
                    })
    @CreateLdapServer(
            transports =
                    {
                            @CreateTransport(protocol = "LDAP",
                                    port = ExternalContextBindingTestCase.LDAP_PORT)
                    })
    //@formatter:on
    static class PrepareExternalLDAPServerSetup implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;

        public void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
            final CreateTransport[] createTransports = createLdapServer.transports();
            for (int i = 0; i < createTransports.length; i++) {
                final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(
                        createTransports[i]);
                mgCreateTransport.setAddress(address);
                createTransports[i] = mgCreateTransport;
            }
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();

            try {
                for (LdifEntry ldifEntry : new LdifReader(
                        ExternalContextBindingTestCase.class
                                .getResourceAsStream(ExternalContextBindingTestCase.class.getSimpleName()
                                        + ".ldif"))) {
                    directoryService.getAdminSession()
                            .add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }

            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer)AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, managementClient.getMgmtAddress());
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
        }
    }

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "externalContextBindingTest.jar")
                .addClasses(ExternalContextBindingTestCase.class, LookupEjb.class);
    }

    @Test
    public void testBasicWithoutCache() throws Exception {
        InitialContext context = new InitialContext();
        InitialContext lookupContext = (InitialContext) context.lookup("java:global/nocache");
        Assert.assertNotNull(lookupContext);
        LookupEjb ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        InitialContext newLookupContext = (InitialContext) context.lookup("java:global/nocache");
        Assert.assertNotSame(lookupContext, newLookupContext);
        Assert.assertEquals(InitialContext.class, lookupContext.getClass());
    }

    @Test
    public void testBasicWithCache() throws Exception {
        InitialContext context = new InitialContext();
        InitialContext lookupContext = (InitialContext) context.lookup("java:global/cache");
        Assert.assertNotNull(lookupContext);
        LookupEjb ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        InitialContext newLookupContext = (InitialContext) context.lookup("java:global/cache");
        Assert.assertSame(lookupContext, newLookupContext);

        //close should have no effect
        lookupContext.close();
        ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        Assert.assertNotSame(InitialContext.class, lookupContext.getClass());
    }

    @Test
    public void testWithActualLDAPContextWithoutCache() throws Exception {
        InitialContext ctx = null;
        InitialDirContext ldapContext1 = null;
        InitialDirContext ldapContext2 = null;
        try {
            ctx = new InitialContext();
            LOGGER.info("looking up java:global/ldap ....");
            ldapContext1 = (InitialDirContext)ctx.lookup("java:global/ldap");
            ldapContext2 = (InitialDirContext)ctx.lookup("java:global/ldap");
            Assert.assertNotNull(ldapContext1);
            Assert.assertNotNull(ldapContext2);
            Assert.assertNotSame(ldapContext1, ldapContext2);
            LOGGER.info("acquired external LDAP context: " + ldapContext1.toString());
            LdapCtx c = (LdapCtx)ldapContext1.lookup("dc=jboss,dc=org");
            c = (LdapCtx)c.lookup("ou=People");
            Attributes attributes = c.getAttributes("uid=jduke");
            Assert.assertTrue(attributes.get("description").contains("awesome"));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            if(ldapContext1 != null) {
                ldapContext1.close();
            }
            if(ldapContext2 != null) {
                ldapContext2.close();
            }
        }
    }

    @Test
    public void testWithActualLDAPContextWithCache() throws Exception {
        InitialContext ctx = null;
        InitialDirContext ldapContext1 = null;
        InitialDirContext ldapContext2 = null;
        try {
            ctx = new InitialContext();
            LOGGER.info("looking up java:global/ldap-cache ....");
            ldapContext1 = (InitialDirContext)ctx.lookup("java:global/ldap-cache");
            ldapContext2 = (InitialDirContext)ctx.lookup("java:global/ldap-cache");
            Assert.assertNotNull(ldapContext1);
            Assert.assertNotNull(ldapContext2);
            Assert.assertSame(ldapContext1, ldapContext2);
            LOGGER.info("acquired external LDAP context: " + ldapContext1.toString());
            LdapCtx c = (LdapCtx)ldapContext1.lookup("dc=jboss,dc=org");
            c = (LdapCtx)c.lookup("ou=People");
            Attributes attributes = c.getAttributes("uid=jduke");
            Assert.assertTrue(attributes.get("description").contains("awesome"));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            if(ldapContext1 != null) {
                ldapContext1.close();
            }
            if(ldapContext2 != null) {
                ldapContext2.close();
            }
        }
    }

}
