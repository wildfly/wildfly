/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.ear;

import static org.wildfly.test.integration.mvc.MVCTestUtil.callAndTest;

import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.formatter.Formatters;
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
public class MVCEarTestCase {

    private static final Logger LOGGER = Logger.getLogger(MVCEarTestCase.class.getName());

    private static final String WITH_CONTROLLER = "with-controller.war";
    private static final String NO_CONTROLLER = "no-controller.war";
    private static final String JSP_VIEW = "JSP View";

    @Deployment
    public static EnterpriseArchive earWithLib() {

        String localResourcePath = MVCEarTestCase.class.getPackage().getName().replace('.', '/');

        // Shared jar in ear/lib
        JavaArchive sharedJar = ShrinkWrap.create(JavaArchive.class, "shared.jar")
                .addClasses(SharedMVCController.class);

        // a war that includes a @Controller class, plus can access one in the shared jar
        WebArchive withController = ShrinkWrap.create(WebArchive.class, WITH_CONTROLLER)
                .addClasses(TestApplication.class, UnsharedMVCController.class, NonMVCResource.class)
                .addAsWebInfResource(new ClassLoaderAsset(localResourcePath + "/view.jsp"), "views/view.jsp");

        // a war that doesn't have any @Controller and can only use one in the shared jar
        WebArchive noController = ShrinkWrap.create(WebArchive.class, NO_CONTROLLER)
                .addClasses(TestApplication.class, NonMVCResource.class)
                .addAsWebInfResource(new ClassLoaderAsset(localResourcePath + "/view.jsp"), "views/view.jsp");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, MVCEarTestCase.class + ".ear")
                .addAsLibrary(sharedJar)
                // a war that includes a @Controller class, plus can access one in the shared jar
                .addAsModule(withController)
                // a war that doesn't have any @Controller and can only use one in the shared jar
                .addAsModule(noController);

        LOGGER.info(sharedJar.toString(Formatters.VERBOSE));
        LOGGER.info(withController.toString(Formatters.VERBOSE));
        LOGGER.info(noController.toString(Formatters.VERBOSE));
        LOGGER.info(ear.toString(Formatters.VERBOSE));
        return ear;
    }

    @Test
    public void test() throws IOException {
        callAndTest(WITH_CONTROLLER, "non-mvc", "No View");
        callAndTest(WITH_CONTROLLER, "unshared", JSP_VIEW);
        callAndTest(WITH_CONTROLLER, "shared", JSP_VIEW);
        callAndTest(NO_CONTROLLER, "non-mvc", "No View");
        callAndTest(NO_CONTROLLER, "shared", JSP_VIEW);
    }
}
