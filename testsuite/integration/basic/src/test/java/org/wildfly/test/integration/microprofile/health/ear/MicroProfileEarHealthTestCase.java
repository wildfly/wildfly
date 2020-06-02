/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.health.ear;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.function.Functions;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.health.TestApplication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileEarHealthTestCase {

    public static final String LIB_JAR_ARCHIVE_NAME = "MicroProfileEarHealthTest_Jar.jar";
    public static final String EJB_ARCHIVE_NAME = "MicroProfileEarHealthTest_Ejb.jar";
    public static final String WAR1_ARCHIVE_NAME = "MicroProfileEarHealthTest_War1.war";
    public static final String WAR2_ARCHIVE_NAME = "MicroProfileEarHealthTest_War2.war";
    public static final String EAR_ARCHIVE_NAME = "MicroProfileEarHealthTestCase.ear";

    @Deployment(name = EAR_ARCHIVE_NAME, managed = false)
    public static Archive<?> deployEar() {
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, LIB_JAR_ARCHIVE_NAME)
                .addClasses(LibJarConfigSource.class, LibJarHealthCheck.class)
                .addAsServiceProvider(ConfigSource.class, LibJarConfigSource.class)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml")
                .addManifest();

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, EJB_ARCHIVE_NAME)
                .addClasses(EjbJarConfigSource.class, EjbJarHealthCheck.class)
                .addAsServiceProvider(ConfigSource.class, EjbJarConfigSource.class)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(emptyEjbJar(), "META-INF/ejb-jar.xml")
                .addAsResource(new StringAsset("Class-Path: lib/" + LIB_JAR_ARCHIVE_NAME + "\n"), "META-INF/MANIFEST.MF");

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, WAR1_ARCHIVE_NAME)
                .addClasses(War1ConfigSource.class, War1HealthCheck.class,
                        DuplicateWar1HealthCheck.class, War2HealthCheck.class,
                        TestApplication.class, TestApplication.Resource.class)
                .addAsServiceProvider(ConfigSource.class, War1ConfigSource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset("Class-Path: lib/" + LIB_JAR_ARCHIVE_NAME + " " + EJB_ARCHIVE_NAME + "\n"), "META-INF/MANIFEST.MF");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, WAR2_ARCHIVE_NAME)
                .addClasses(War2ConfigSource.class, War2HealthCheck.class)
                .addAsServiceProvider(ConfigSource.class, War2ConfigSource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset("Class-Path: " + EJB_ARCHIVE_NAME + "\n"), "META-INF/MANIFEST.MF");


        WebArchive testableWar1 = Testable.archiveToTest(war1);

        EnterpriseArchive ear = ShrinkWrap
                .create(EnterpriseArchive.class, EAR_ARCHIVE_NAME)
                .addAsModule(testableWar1)
                .addAsModule(war2)
                .addAsModule(ejbJar)
                .addAsLibraries(libJar)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(MicroProfileEarHealthTestCase.class.getPackage(), "application.xml", "application.xml")
                .addAsResource(MicroProfileEarHealthTestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
                .addManifest();

        System.out.println(ear.toString(true));
        return ear;
    }


    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                        "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                        "         version=\"3.0\">\n" +
                        "   \n" +
                        "</ejb-jar>");
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(1)
    public void testHealthCheckBeforeDeployment() throws Exception {
        checkOutcome(true, false);

        // deploy the archive
        deployer.deploy(EAR_ARCHIVE_NAME);
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment(EAR_ARCHIVE_NAME)
    public void testHealthCheckAfterDeployment(@ArquillianResource URL url) throws Exception {
        checkOutcome(true, true);
    }

    @Test
    @InSequence(3)
    public void testHealthCheckBetweenDeployments() throws Exception {
        deployer.undeploy(EAR_ARCHIVE_NAME);
        checkOutcome(true, false);

    }

    private void checkOutcome(boolean mustBeUP, boolean checkDeployment) throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "microprofile-health-smallrye");
        ModelNode checkOp = getEmptyOperation("check", address);

        ModelNode response = managementClient.getControllerClient().execute(checkOp);

        final String opOutcome = response.get("outcome").asString();
        assertEquals("success", opOutcome);

        ModelNode result = response.get("result");

        if (checkDeployment) {
            checkSubDeploymentCheks(result);
        }

        assertEquals(mustBeUP ? "UP" : "DOWN", result.get("status").asString());

    }

    private void checkSubDeploymentCheks(ModelNode response) {
        System.out.println(response.toJSONString(false));
        Map<String, ModelNode> checksByName = response.get("checks").asList().stream()
                .collect(Collectors.toMap(
                        n -> n.get("name").asString(),
                        Functions.identity()
                ));

        // See issue WFLY-12835
        // All subdeployment checks should be discovered.
        // Config properties should be resolved from each module class loader Config instance.

        checkPropertyDefined(checksByName, LibJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_LIB_JAR, true);
        checkPropertyDefined(checksByName, LibJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_EJB_JAR, false);
        checkPropertyDefined(checksByName, LibJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR1, false);
        checkPropertyDefined(checksByName, LibJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR2, false);

        checkPropertyDefined(checksByName, EjbJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_LIB_JAR, true);
        checkPropertyDefined(checksByName, EjbJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_EJB_JAR, true);
        checkPropertyDefined(checksByName, EjbJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR1, false);
        checkPropertyDefined(checksByName, EjbJarHealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR2, false);

        checkPropertyDefined(checksByName, War1HealthCheck.class, HealthTestConstants.PROPERTY_NAME_LIB_JAR, true);
        checkPropertyDefined(checksByName, War1HealthCheck.class, HealthTestConstants.PROPERTY_NAME_EJB_JAR, true);
        checkPropertyDefined(checksByName, War1HealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR1, true);
        checkPropertyDefined(checksByName, War1HealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR2, false);

        checkPropertyDefined(checksByName, War2HealthCheck.class, HealthTestConstants.PROPERTY_NAME_LIB_JAR, true);
        checkPropertyDefined(checksByName, War2HealthCheck.class, HealthTestConstants.PROPERTY_NAME_EJB_JAR, true);
        checkPropertyDefined(checksByName, War2HealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR1, false);
        checkPropertyDefined(checksByName, War2HealthCheck.class, HealthTestConstants.PROPERTY_NAME_WAR2, true);
    }

    private void checkPropertyDefined(Map<String, ModelNode> checksByName, Class<?> healthCheckClass,
                                      String propertyName, boolean expectedValue) {
        String checkName = healthCheckClass.getName();
        ModelNode checkOutcome = checksByName.get(checkName);
        assertNotNull(checkOutcome);

        String status = checkOutcome.get("status").asString();
        assertEquals("UP", status);

        ModelNode data = checkOutcome.get("data");
        assertNotNull(data);

        try {
            boolean propertyValue = data.get(propertyName).asBoolean();
            assertEquals(propertyValue, expectedValue);
        } catch (IllegalArgumentException e) {
            ModelNode propertyValue = data.get(propertyName);
            fail("Could not parse health data '" + propertyValue.asString() + "' to boolean for property " + propertyName);
        }
    }

}
