/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.security.runas;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DESTROY_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DESTROY_METHOD_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DOGET_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.DOGET_METHOD_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.INIT_METHOD_NOT_PASS;
import static org.jboss.as.test.integration.web.security.runas.CallProtectedEjbServlet.INIT_METHOD_PASS;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for RunAs in Servlet. It tests secured EJB invocation in HttpServlet init(), doGet() and destroy() methods.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
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
    @InSequence(Integer.MIN_VALUE)
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
    @Test
    @InSequence(1)
    @OperateOnDeployment(DEPLOYMENT)
    public void runServletWithCorrectRole(@ArquillianResource URL url) throws Exception {
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
    @InSequence(2)
    public void checkInitMethodWithCorrectRole() throws Exception {
        assertTrue("EJB invocation failed in init() method of Servlet which uses RunAs with correct role.",
                correctRoleResult.contains(INIT_METHOD_PASS + HelloBean.HELLO));
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.doGet() method.
     *
     * @throws Exception
     */
    @Test
    @InSequence(3)
    public void checkDoGetMethodWithCorrectRole() throws Exception {
        assertTrue("EJB invocation failed in doGet() method of Servlet which uses RunAs with correct role.",
                correctRoleResult.contains(DOGET_METHOD_PASS + HelloBean.HELLO));
    }

    /**
     * Access Servlet which uses RunAs with different role than in needed for secured EJB invocation.
     * <p>
     * This method will run init() and doGet() method and stores results.
     *
     * @param url
     * @throws Exception
     */
    @Test
    @InSequence(4)
    @OperateOnDeployment(DEPLOYMENT)
    public void runServletWithIncorrectRole(@ArquillianResource URL url) throws Exception {
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
    @InSequence(5)
    public void checkInitMethodWithIncorrectRole() throws Exception {
        assertTrue("EJB invocation did not failed in init() method of Servlet which uses RunAs with incorrect role.",
                incorrectRoleResult.contains(INIT_METHOD_NOT_PASS));
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.doGet() method.
     *
     * @throws Exception
     */
    @Test
    @InSequence(6)
    public void checkDoGetMethodWithIncorrectRole() throws Exception {
        assertTrue("EJB invocation did not failed in doGet() method of Servlet which uses RunAs with incorrect role.",
                incorrectRoleResult.contains(DOGET_METHOD_NOT_PASS));
    }

    /**
     * Stop server for invoking HttpServlet.destroy() method during undeploying.
     *
     * @throws Exception
     */
    @Test
    @InSequence(7)
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
    @InSequence(8)
    public void checkDestroyInUndeployingMethodWithCorrectRole() throws Exception {
        assertTrue("EJB invocation failed in destroy() method of Servlet which uses RunAs with correct role during undeploying.",
                readFirstLineOfFile(CORRECT_ROLE_AND_UNDEPLOY).contains(DESTROY_METHOD_PASS + HelloBean.HELLO));
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.destroy() method during undeploying application.
     *
     * @throws Exception
     */
    @Test
    @InSequence(9)
    public void checkDestroyInUndeployingMethodWithIncorrectRole() throws Exception {
        assertTrue("EJB invocation did not failed in destroy() method of Servlet which uses RunAs with incorrect role during "
                        + "undeploying.",
                readFirstLineOfFile(INCORRECT_ROLE_AND_UNDEPLOY).contains(DESTROY_METHOD_NOT_PASS));
    }

    /**
     * Restart server for invoking HttpServlet.destroy() method during stopping server. It also hit Servlet for initialization
     * of Servlet before server is restarted.
     *
     * @param url
     * @throws Exception
     */
    @Test
    @InSequence(10)
    @OperateOnDeployment(DEPLOYMENT)
    public void runDestroyMethodInStopServer(@ArquillianResource URL url) throws Exception {
        deployer.deploy(DEPLOYMENT);

        URL servletUrl = new URL(url.toExternalForm() + RunAsAdminServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, CORRECT_ROLE_AND_STOP_SERVER.getAbsolutePath()));
        Utils.makeCall(servletUrl.toURI(), HTTP_OK);
        servletUrl = new URL(url.toExternalForm() + RunAsUserServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallProtectedEjbServlet.FILE_PARAM, INCORRECT_ROLE_AND_STOP_SERVER.getAbsolutePath()));
        Utils.makeCall(servletUrl.toURI(), HTTP_OK);

        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 50000);
    }

    /**
     * Check whether Servlet which uses RunAs with correct role needed for secured EJB invocation can correctly invoked that EJB
     * method in HttpServlet.destroy() method during stopping server.
     *
     * @throws Exception
     */
    @Test
    @InSequence(11)
    public void checkDestroyMethodInStopServerWithCorrectRole() throws Exception {
        assertTrue("EJB invocation failed in destroy() method of Servlet which uses RunAs with correct role during stopping "
                        + "server.",
                readFirstLineOfFile(CORRECT_ROLE_AND_STOP_SERVER).contains(DESTROY_METHOD_PASS + HelloBean.HELLO));
    }

    /**
     * Check whether Servlet which uses RunAs with different role than in needed for secured EJB invocation cannot correctly
     * invoked that EJB method in HttpServlet.destroy() method during stopping server.
     *
     * @throws Exception
     */
    @Test
    @InSequence(12)
    public void checkDestroyMethodInStopServerWithIncorrectRole() throws Exception {
        assertTrue("EJB invocation did not failed in destroy() method of Servlet which uses RunAs with incorrect role "
                        + "during stopping server.",
                readFirstLineOfFile(INCORRECT_ROLE_AND_STOP_SERVER).contains(DESTROY_METHOD_NOT_PASS));
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
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
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        }
    }

}
