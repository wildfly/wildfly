/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.viewengine;

import static org.wildfly.test.integration.mvc.MVCTestUtil.callAndTest;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.mvc.MVCTestUtil;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Preview.class, MVCTestUtil.ServerSetup.class})
public class ViewEngineTestCase {

    public static final String SIMPLE_WAR = "ViewEngineTestCase-simple.war";
    public static final String SIMPLE_EAR = "ViewEngineTestCase-simple.ear";
    public static final String WAR_WITH_LIB = "ViewEngineTestCase-with-lib.war";
    public static final String EAR_WITH_LIB = "ViewEngineTestCase-with-lib.ear";
    private static final JavaArchive viewLib = createViewLib();

    private static JavaArchive createViewLib() {
        return ShrinkWrap.create(JavaArchive.class, "viewEngine.jar")
                .addClasses(MockViewEngine.class, MockViewEngineConfigProducer.class)
                .addAsManifestResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    private static WebArchive simpleWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name)
                .addClasses(TestApplication.class, TestMVCController.class);
    }

    @Deployment(name = WAR_WITH_LIB, managed = false, testable = false)
    public static WebArchive warWithLib() {
        return simpleWar(WAR_WITH_LIB)
                .addAsLibrary(viewLib);
    }

    @Deployment(name = SIMPLE_EAR, managed = false, testable = false)
    public static EnterpriseArchive simpleEar() {
        return ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR)
                .addAsModule(warWithLib());
    }

    @Deployment(name = EAR_WITH_LIB, managed = false, testable = false)
    public static EnterpriseArchive earWithLib() {
        return ShrinkWrap.create(EnterpriseArchive.class, EAR_WITH_LIB)
                .addAsModule(simpleWar(SIMPLE_WAR))
                .addAsLibrary(viewLib);
    }

    @ArquillianResource
    private Deployer deployer;

    @Test
    public void testWarLib() throws IOException {
        test(WAR_WITH_LIB, WAR_WITH_LIB);
    }

    @Test
    public void testEarLib() throws IOException {
        test(EAR_WITH_LIB, SIMPLE_WAR);
    }

    @Test
    public void testWarLibInEar() throws IOException {
        test(SIMPLE_EAR, WAR_WITH_LIB);
    }

    private void test(String deploymentName, String warName) throws IOException {
        deployer.deploy(deploymentName);
        try {
            callAndTest(warName, "test", "Mock View");
        } finally {
            deployer.undeploy(deploymentName);
        }
    }

}
