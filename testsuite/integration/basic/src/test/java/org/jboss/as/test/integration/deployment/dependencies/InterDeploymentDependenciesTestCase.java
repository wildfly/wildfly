/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;

import java.io.IOException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ArchiveDeployer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.lkop.minilib.annotations.MiniLib;
import org.lkop.minilib.annotations.MiniLibAnnotation;
import org.lkop.minilib.annotations.MiniLibDependenciesFolder;
import org.lkop.minilib.annotations.MiniLibOutputFolder;


/**
 * Tests inter deployment dependencies
 */
@ExtendWith(MiniLibAnnotation.class)
@MiniLibDependenciesFolder("/Users/psotirop/m22/repo")
@MiniLibOutputFolder("/tmp/")
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class InterDeploymentDependenciesTestCase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";

    // Dummy deployment so arq will be able to inject a ManagementClient
    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    private static JavaArchive DEPENDEE = ShrinkWrap.create(JavaArchive.class, "dependee.jar")
                .addClasses(DependeeEjb.class, StringView.class);

    private static JavaArchive DEPENDENT = ShrinkWrap.create(JavaArchive.class, "dependent.jar")
                .addClasses(DependentEjb.class, StringView.class)
                .addPackage(org.lkop.minilib.MiniLibEngine.class.getPackage())
                .addPackage(org.lkop.minilib.treecomponents.BaseVisitor.class.getPackage())
                .addPackage(org.lkop.minilib.utils.StringUtils.class.getPackage())                
                .addAsManifestResource(InterDeploymentDependenciesTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");

    private static Context context;

    @BeforeAll
    public static void beforeClass() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @ArquillianResource
    public ManagementClient managementClient;

    // We don't inject this via @ArquillianResource because ARQ can't fully control
    // DEPENDEE and DEPENDENT and things go haywire if we try. But we use ArchiveDeployer
    // because it's a convenient API for handling deploy/undeploy of Shrinkwrap archives
    private ArchiveDeployer deployer;

    @BeforeEach
    public void setup() {
        deployer = new ArchiveDeployer(managementClient);
    }

    @AfterEach
    public void cleanUp() {
        try {
            deployer.undeploy(DEPENDENT.getName());
        } catch (Exception e) {
            // Ignore
        }

        try {
            deployer.undeploy(DEPENDEE.getName());
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @MiniLib
    public void testDeploymentDependencies() throws NamingException, DeploymentException {

        try {
            deployer.deploy(DEPENDENT);
            Assertions.fail("Deployment did not fail");
        } catch (Exception e) {
            // expected
        }

        deployer.deploy(DEPENDEE);
        deployer.deploy(DEPENDENT);

        StringView ejb = lookupStringView();
        Assertions.assertEquals("hello", ejb.getString());
    }

    @Test
    @MiniLib
    public void testDeploymentDependenciesWithRestart() throws NamingException, IOException, DeploymentException {

        try {
            deployer.deploy(DEPENDENT);
            Assertions.fail("Deployment did not fail");
        } catch (Exception e) {
            // expected
        }

        deployer.deploy(DEPENDEE);
        deployer.deploy(DEPENDENT);

        StringView ejb = lookupStringView();
        Assertions.assertEquals("hello", ejb.getString());

        ModelNode response = managementClient.getControllerClient().execute(Util.createEmptyOperation("redeploy", PathAddress.pathAddress("deployment", DEPENDEE.getName())));
        Assertions.assertEquals("success", response.get("outcome").asString());

        ejb = lookupStringView();
        Assertions.assertEquals("hello", ejb.getString());
    }

    private static StringView lookupStringView() throws NamingException {
        return (StringView) context.lookup("ejb:" + APP_NAME + "/dependent/" + DISTINCT_NAME
                + "/" + DependentEjb.class.getSimpleName() + "!" + StringView.class.getName());

    }

}
