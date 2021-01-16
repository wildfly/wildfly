/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.securitydomain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.test.integration.management.util.ServerReload.reloadIfRequired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Hello;
import org.jboss.as.test.integration.ejb.security.securitydomain.ejb.Info;
import org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal;
import org.jboss.as.test.integration.ejb.security.securitydomain.module.NonValidatingLoginModule;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Francesco Marchioni
 * JIRA: https://issues.redhat.com/browse/WFLY-12516
 * EJBContext principal is not popped back after invoking another EJB using a different Security Domain
 */

@RunWith(Arquillian.class)
@ServerSetup({SnapshotRestoreSetupTask.class, EJBContextMultipleSDTestCase.EJBContextMultipleSDTestCaseServerSetup.class})
public class EJBContextMultipleSDTestCase {

    private static final String ARCHIVE_NAME = "EJBContextMultipleSDTestCase";
    protected Logger log = Logger.getLogger(this.getClass().getSimpleName());

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addAsManifestResource(Hello.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addPackage(Hello.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext initialContext;

    private static final String HOST_PORT = "localhost:8080";
    private static final String REMOTE_HTTP = "remote+http://" + HOST_PORT;
    private static final String CTX_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";

    private static final String USER = "ADMIN";
    private static final String PWD = "redhat1!";

    private static Context getInitialContext() throws NamingException {
        Properties ejbClientProperties = new Properties();
        ejbClientProperties.put(Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY);
        ejbClientProperties.put(Context.PROVIDER_URL, REMOTE_HTTP);
        ejbClientProperties.put(Context.SECURITY_PRINCIPAL, USER);
        ejbClientProperties.put(Context.SECURITY_CREDENTIALS, PWD);

        Context context = new InitialContext(ejbClientProperties);
        return context;
    }

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeElytronProfileEnabled(); // PicketBox specific scenario, Elytron testing not required.
    }

    @Test
    public void testSequentialInvocations() throws Exception {
        Context context = getInitialContext();

        Info info = new Info("Test1: client -> ejb1 (MySecurityDomain/SimplePrincipal) -> ejb2 (MyNonValidatingSecurityDomain/MyPrincipal)");
        Hello hello = (Hello) context.lookup(Hello.HelloOne);
        List<String> list = hello.sayHelloSeveralTimes(info);

        Assert.assertEquals(list.get(0), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");
        Assert.assertEquals(list.get(1), "Correct: org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal == org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal");
        Assert.assertEquals(list.get(2), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");

        info = new Info("Test2: client -> ejb2 (MyNonValidatingSecurityDomain/MyPrincipal) -> ejb1 (MySecurityDomain/SimplePrincipal)");
        hello = (Hello) context.lookup(Hello.HelloTwo);
        list = hello.sayHelloSeveralTimes(info);

        Assert.assertEquals(list.get(0), "Correct: org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal == org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal");
        Assert.assertEquals(list.get(1), "Correct: org.jboss.security.SimplePrincipal == org.jboss.security.SimplePrincipal");
        Assert.assertEquals(list.get(2), "Correct: org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal == org.jboss.as.test.integration.ejb.security.securitydomain.module.MyPrincipal");
    }

    static class EJBContextMultipleSDTestCaseServerSetup implements ServerSetupTask {
        protected Logger log = Logger.getLogger(this.getClass().getSimpleName());

        Path loginModuleJar;
        TestModule module;

        Path createJar(String namePrefix, Class<?>... classes) throws IOException {
            Path testJar = Files.createTempFile(namePrefix, ".jar");
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClasses(classes);
            jar.as(ZipExporter.class).exportTo(testJar.toFile(), true);
            return testJar;
        }

        void deployModule() throws Exception {
            module = new TestModule("loginmodule.custom", "org.picketbox", "javax.api");
            JavaArchive jar = module.addResource("loginmodule.custom.jar");
            jar.addClasses(MyPrincipal.class, NonValidatingLoginModule.class);
            module.create(true);
        }

        void undeployModule() throws Exception {
            if (module != null) {
                module.remove();
            }
        }

        ModelNode createConfigurationOperation() {
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode updates = compositeOp.get(STEPS);

            PathAddress securityDomainAddress = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, "MySecurityDomain");
            updates.add(Util.createAddOperation(securityDomainAddress));

            PathAddress authAddress = securityDomainAddress.append(AUTHENTICATION, Constants.CLASSIC);
            updates.add(Util.createAddOperation(authAddress));

            ModelNode addModule = Util.createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "Remoting"));
            addModule.get(CODE).set("Remoting");
            addModule.get(FLAG).set("optional");
            addModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            updates.add(addModule);

            addModule = Util.createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "UsersRoles"));
            addModule.get(CODE).set("UsersRoles");
            addModule.get(FLAG).set("required");
            addModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            addModule.get(Constants.MODULE_OPTIONS).add("usersProperties", getUsersFile());
            addModule.get(Constants.MODULE_OPTIONS).add("rolesProperties", getGroupsFile());
            updates.add(addModule);

            securityDomainAddress = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, "MyNonValidatingDomain");
            updates.add(Util.createAddOperation(securityDomainAddress));

            authAddress = securityDomainAddress.append(AUTHENTICATION, Constants.CLASSIC);
            updates.add(Util.createAddOperation(authAddress));

            addModule = Util.createAddOperation(authAddress.append(Constants.LOGIN_MODULE, "Custom"));
            addModule.get(CODE).set(NonValidatingLoginModule.class.getName());
            addModule.get(MODULE).set("loginmodule.custom");
            addModule.get(FLAG).set("required");
            addModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            updates.add(addModule);

            PathAddress realmAddress = PathAddress.pathAddress(PathElement.pathElement("core-service", "management"),
                    PathElement.pathElement("security-realm", "MyRealm"));
            ModelNode addRealm = Util.createAddOperation(realmAddress);
            addRealm.get("map-group-to-roles").set("true");
            updates.add(addRealm);

            ModelNode addAuthentication = Util.createAddOperation(realmAddress.append("authentication", "jaas"));
            addAuthentication.get("name").set("MySecurityDomain");
            updates.add(addAuthentication);

            PathAddress httpConnectorAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "remoting"),
                    PathElement.pathElement("http-connector", "http-remoting-connector"));
            updates.add(Util.getWriteAttributeOperation(httpConnectorAddress, "security-realm", "MyRealm"));

            return compositeOp;
        }

        void execute(final ModelControllerClient client, final ModelNode operation) throws IOException {
            final ModelNode result = client.execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail(Operations.getFailureDescription(result).toString());
            }
            Operations.readResult(result);
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            log.debug("Installing modules and Security Domains");

            deployModule();

            CommandContext ctx = CLITestUtil.getCommandContext();
            ctx.connectController();
            ModelControllerClient modelControllerClient = ctx.getModelControllerClient();

            execute(modelControllerClient, createConfigurationOperation());
            reloadIfRequired(modelControllerClient);

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            undeployModule();
        }

        String getUsersFile() {
            return new File(EJBContextMultipleSDTestCase.class.getResource("users.properties").getFile()).getAbsolutePath();
        }

        String getGroupsFile() {
            return new File(EJBContextMultipleSDTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath();
        }
    }
}