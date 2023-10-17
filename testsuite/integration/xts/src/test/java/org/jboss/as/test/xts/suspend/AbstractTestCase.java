/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend;

import java.net.URLPermission;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.util.List;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.jboss.as.test.xts.suspend.Helpers.getExecutorService;
import static org.jboss.as.test.xts.suspend.Helpers.getRemoteService;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public abstract class AbstractTestCase {

    protected static final String EXECUTOR_SERVICE_CONTAINER = "default-server";

    protected static final String REMOTE_SERVICE_CONTAINER = "alternative-server";

    protected static final String EXECUTOR_SERVICE_ARCHIVE_NAME = "executorService";

    protected static final String REMOTE_SERVICE_ARCHIVE_NAME = "remoteService";

    @ArquillianResource
    @OperateOnDeployment(EXECUTOR_SERVICE_ARCHIVE_NAME)
    private URL executorServiceUrl;

    @ArquillianResource
    @OperateOnDeployment(REMOTE_SERVICE_ARCHIVE_NAME)
    private URL remoteServiceUrl;

    @ArquillianResource
    @OperateOnDeployment(REMOTE_SERVICE_ARCHIVE_NAME)
    private ManagementClient remoteServiceContainerManager;

    private ExecutorService executorService;

    private RemoteService remoteService;

    protected static WebArchive getExecutorServiceArchiveBase() {
        return ShrinkWrap.create(WebArchive.class, EXECUTOR_SERVICE_ARCHIVE_NAME + ".war")
                .addClasses(ExecutorService.class, RemoteService.class, Helpers.class)
                .addAsResource("context-handlers.xml", "context-handlers.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts, org.jboss.jts"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new SocketPermission("127.0.0.1:8180", "connect,resolve"),
                        new RuntimePermission("org.apache.cxf.permission", "resolveUri"),
                        //Required by the ProxySelector in the new HttpClientHTTPConduit
                        //This can be removed after the https://issues.apache.org/jira/browse/CXF-8933 is included
                        new NetPermission("getProxySelector"),
                        //Removed this after https://issues.apache.org/jira/browse/CXF-8935 is fxied
                        new URLPermission("http:*", "POST:*"),
                        // WSDLFactory#L243 from wsdl4j library needs the following
                        new FilePermission(System.getProperty("java.home") + File.separator + "lib" + File.separator + "wsdl.properties", "read")
                        ), "permissions.xml");
    }

    protected static WebArchive getRemoteServiceArchiveBase() {
        return ShrinkWrap.create(WebArchive.class, REMOTE_SERVICE_ARCHIVE_NAME + ".war")
                .addClasses(RemoteService.class)
                .addAsResource("context-handlers.xml", "context-handlers.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts, org.jboss.jts"), "MANIFEST.MF");
    }

    protected abstract void assertParticipantInvocations(List<String> invocations);

    @Before
    public void before() throws Exception {
        resumeServer();
        executorService = getExecutorService(executorServiceUrl);
        executorService.init(remoteServiceContainerManager.getWebUri().toString() + "/ws-c11/ActivationService",
                remoteServiceUrl.toString());
        executorService.reset();
        remoteService = getRemoteService(remoteServiceUrl);
        remoteService.reset();
    }

    @After
    public void after() throws IOException {
        resumeServer();
        try {
            executorService.rollback();
        } catch (Throwable ignored) {
        }
    }

    @Test(expected = Exception.class)
    public void testBeginTransactionAfterSuspend() throws Exception {
        suspendServer();
        executorService.begin();
    }

    @Test
    public void testCommitAfterSuspend() throws Exception {
        executorService.begin();
        suspendServer();
        executorService.commit();
    }

    @Test
    public void testRollbackAfterSuspend() throws Exception {
        executorService.begin();
        suspendServer();
        executorService.reset();
    }

    @Test
    public void testRemoteServiceAfterSuspend() throws Exception {
        executorService.begin();
        suspendServer();
        executorService.enlistParticipant();
        executorService.execute();
        executorService.commit();
        resumeServer();
        assertParticipantInvocations(executorService.getParticipantInvocations());
        assertParticipantInvocations(remoteService.getParticipantInvocations());
    }

    private void suspendServer() throws IOException {
        ModelNode suspendOperation = new ModelNode();
        suspendOperation.get(ModelDescriptionConstants.OP).set("suspend");
        remoteServiceContainerManager.getControllerClient().execute(suspendOperation);
    }

    private void resumeServer() throws IOException {
        ModelNode suspendOperation = new ModelNode();
        suspendOperation.get(ModelDescriptionConstants.OP).set("resume");
        remoteServiceContainerManager.getControllerClient().execute(suspendOperation);
    }

}
