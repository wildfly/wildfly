/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DESTROY_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DESTROY_METHOD_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DOGET_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DOGET_METHOD_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.INIT_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.INIT_METHOD_PASS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test case for RunAs in Servlet. It tests secured EJB invocation in HttpServlet init(), doGet() and destroy() methods.
 *
 * @author olukas
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServletRunAsTestCase {

    private static final String DEPLOYMENT = "deployment";

    private static String correctRoleResult;
    private static String incorrectRoleResult;

    private static final File WORK_DIR = new File("ServletRunAsTestCase-" + System.currentTimeMillis());
    private static final File CORRECT_ROLE_AND_STOP_SERVER = new File(WORK_DIR, "correctRoleAndStopServer.log");
    private static final File INCORRECT_ROLE_AND_STOP_SERVER = new File(WORK_DIR, "incorrectRoleAndStopServer.log");
    private static final File CORRECT_ROLE_AND_UNDEPLOY = new File(WORK_DIR, "correctRoleAndUndeploy.log");
    private static final File INCORRECT_ROLE_AND_UNDEPLOY = new File(WORK_DIR, "incorrectRoleAndUndeploy.log");

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    Deployer deployer;

    @Test
    @Order(Integer.MIN_VALUE)
    public void initialize() throws Exception {
        deployer.deploy(DEPLOYMENT);
        WORK_DIR.mkdirs();
    }

    /**
     * Access Servlet which uses RunAs with correct role needed for secured EJB invocation.
     * <p>
     * This method will run init() and doGet() method and stores results.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    @Order(1)
    public void runServletWithCorrectRole(@ArquillianResource @OperateOnDeployment(DEPLOYMENT) URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + RunAsAdminServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, CORRECT_ROLE_AND_UNDEPLOY.getAbsolutePath()));
        correctRoleResult = Utils.makeCall(servletUrl.toURI(), HTTP_OK);
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.init() method.
     *
     * @throws Exception
     */
    @Test
    @Order(2)
    public void checkInitMethodWithCorrectRole() throws Exception {
        assertTrue(correctRoleResult.contains(INIT_METHOD_PASS + HelloBean.HELLO),
                "EJB invocation failed in init() method of Servlet which uses RunAs with correct role.");
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.doGet() method.
     *
     * @throws Exception
     */
    @Test
    @Order(3)
    public void checkDoGetMethodWithCorrectRole() throws Exception {
        assertTrue(correctRoleResult.contains(DOGET_METHOD_PASS + HelloBean.HELLO),
                "EJB invocation failed in doGet() method of Servlet which uses RunAs with correct role.");
    }

    /**
     * Access Servlet which uses RunAs with different role than in needed for secured EJB invocation.
     * <p>
     * This method will run init() and doGet() method and stores results.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    @Order(4)
    public void runServletWithIncorrectRole(@ArquillianResource @OperateOnDeployment(DEPLOYMENT) URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + RunAsUserServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, INCORRECT_ROLE_AND_UNDEPLOY.getAbsolutePath()));
        incorrectRoleResult = Utils.makeCall(servletUrl.toURI(), HTTP_OK);
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.init() method.
     *
     * @throws Exception
     */
    @Test
    @Order(5)
    public void checkInitMethodWithIncorrectRole() throws Exception {
        assertTrue(incorrectRoleResult.contains(INIT_METHOD_NOT_PASS),
                "EJB invocation did not failed in init() method of Servlet which uses RunAs with incorrect role.");
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.doGet() method.
     *
     * @throws Exception
     */
    @Test
    @Order(6)
    public void checkDoGetMethodWithIncorrectRole() throws Exception {
        assertTrue(incorrectRoleResult.contains(DOGET_METHOD_NOT_PASS),
                "EJB invocation did not failed in doGet() method of Servlet which uses RunAs with incorrect role.");
    }

    /**
     * Stop server for invoking HttpServlet.destroy() method during undeploying.
     *
     * @throws Exception
     */
    @Test
    @Order(7)
    public void runDestroyMethodInUndeploying() throws Exception {
        deployer.undeploy(DEPLOYMENT);
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.destroy() method during undeploying application.
     *
     * @throws Exception
     */
    @Test
    @Order(8)
    public void checkDestroyInUndeployingMethodWithCorrectRole() throws Exception {
        assertTrue(readFirstLineOfFile(CORRECT_ROLE_AND_UNDEPLOY).contains(DESTROY_METHOD_PASS + HelloBean.HELLO),
                "EJB invocation failed in destroy() method of Servlet which uses RunAs with correct role during undeploying.");
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.destroy() method during undeploying application.
     *
     * @throws Exception
     */
    @Test
    @Order(9)
    public void checkDestroyInUndeployingMethodWithIncorrectRole() throws Exception {
        assertTrue(readFirstLineOfFile(INCORRECT_ROLE_AND_UNDEPLOY).contains(DESTROY_METHOD_NOT_PASS),
                "EJB invocation did not failed in destroy() method of Servlet which uses RunAs with incorrect role during "
                        + "undeploying.");
    }

    /**
     * Re-deploy the application so that {@link @ArquillianResource} URL can be resolved in the next step.
     *
     * @throws Exception
     */
    @Test
    @Order(10)
    public void redeployForStopServer() throws Exception {
        deployer.deploy(DEPLOYMENT);
    }

    /**
     * Hit Servlet for initialization before server is reloaded which invokes HttpServlet.destroy() method.
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(DEPLOYMENT)
    @Test
    @Order(11)
    public void runDestroyMethodInStopServer(@ArquillianResource @OperateOnDeployment(DEPLOYMENT) URL url) throws Exception {
        URL servletUrl = new URL(url.toExternalForm() + RunAsAdminServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, CORRECT_ROLE_AND_STOP_SERVER.getAbsolutePath()));
        Utils.makeCall(servletUrl.toURI(), HTTP_OK);
        servletUrl = new URL(url.toExternalForm() + RunAsUserServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, INCORRECT_ROLE_AND_STOP_SERVER.getAbsolutePath()));
        Utils.makeCall(servletUrl.toURI(), HTTP_OK);

        ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.destroy() method during stopping server.
     *
     * @throws Exception
     */
    @Test
    @Order(12)
    public void checkDestroyMethodInStopServerWithCorrectRole() throws Exception {
        assertTrue(readFirstLineOfFile(CORRECT_ROLE_AND_STOP_SERVER).contains(DESTROY_METHOD_PASS + HelloBean.HELLO),
                "EJB invocation failed in destroy() method of Servlet which uses RunAs with correct role during stopping "
                        + "server.");
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.destroy() method during stopping server.
     *
     * @throws Exception
     */
    @Test
    @Order(13)
    public void checkDestroyMethodInStopServerWithIncorrectRole() throws Exception {
        assertTrue(readFirstLineOfFile(INCORRECT_ROLE_AND_STOP_SERVER).contains(DESTROY_METHOD_NOT_PASS),
                "EJB invocation did not failed in destroy() method of Servlet which uses RunAs with incorrect role "
                        + "during stopping server.");
    }

    @Test
    @Order(Integer.MAX_VALUE)
    public void finish() throws Exception {
        deployer.undeploy(DEPLOYMENT);
        FileUtils.deleteQuietly(WORK_DIR);
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "servlet-runas.war");
        war.addClasses(CallProtectedEjbServlet.class, RunAsAdminServlet.class, RunAsUserServlet.class, Hello.class,
                HelloBean.class);
        war.addAsWebResource(PermissionUtils.createPermissionsXmlAsset(new FilePermission(WORK_DIR.getAbsolutePath() + "/*", "read,write")), "META-INF/permissions.xml");
        return war;
    }

    private String readFirstLineOfFile(File file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return br.readLine();
        }
    }

}
