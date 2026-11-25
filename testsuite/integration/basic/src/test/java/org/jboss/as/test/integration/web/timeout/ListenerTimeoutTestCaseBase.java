/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.timeout;

import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class ListenerTimeoutTestCaseBase {
    public static final String DEPLOYMENT_NAME = "sockettimeout";
    public static final String DEPLOYMENT_NAME_WAR = DEPLOYMENT_NAME+".war";
    public static final String READ_SERVLET_PATH = "/read";
    public static final String WRITE_SERVLET_PATH = "/write";
    public static final int SMALL_TIMEOUT = 10;

    private AutoCloseable serverSnapshot;
    @ArquillianResource
    protected ManagementClient managementClient;
    @ArquillianResource
    protected URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME_WAR);
        war.addClass(ReadServlet.class);
        war.addClass(WriteServlet.class);
        war.addClass(TimeoutLog.class);
        war.addClass(TimeoutLogBean.class);
        war.addClass(ActionSequence.class);
        return war;
    }

    @Before
    public void before() throws Exception {
        serverSnapshot = ServerSnapshot.takeSnapshot(managementClient);
    }

    @After
    public void after() throws Exception {
        serverSnapshot.close();
    }

    @Test
    public void testReadTimeout() throws IOException, InterruptedException {
        final PathAddress listenerAddress = getListenerAddress();
        final ModelNode setTimeoutOperation = Operations.createWriteAttributeOperation(listenerAddress.toModelNode(), "read-timeout", SMALL_TIMEOUT);
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        final ModelNode response = controllerClient.execute(setTimeoutOperation);
        Assert.assertTrue(Operations.isSuccessfulOutcome(response));
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        final String message = "POST /"+DEPLOYMENT_NAME+READ_SERVLET_PATH+" HTTP/1.1\r\n" +
                "JSESSIONID: MY_ID\r\n" +
                "Expect: 100-continue\r\n" +
                "Content-Length: 16\r\n" +
                "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                "Host: "+getServerAddress()+":"+getServerPort()+"\r\n" +
                "Connection: Keep-Alive\r\n\r\nMy HTTP Request!";
        try(Socket socket = new Socket()){
            socket.connect(new InetSocketAddress(getServerAddress(), getServerPort()), 5000);
            sendMessageToServer(socket, message, true);
            //TOs are opportunistic, it need another OP to trigger TO and than another to see if pipe is broken.
            Thread.sleep(SMALL_TIMEOUT+SMALL_TIMEOUT/2);
            sendMessageToServer(socket, message, false);
            Thread.sleep(SMALL_TIMEOUT/3);

            assertThrows(SocketException.class, () -> {sendMessageToServer(socket, message, false);});

        }
    }

    @Test
    public void testWriteTimeout() throws IOException, InterruptedException, NamingException {
        final TimeoutLog log = lookupEJB();
        Assert.assertNotNull(log);
        final PathAddress listenerAddress = getListenerAddress();
        final ModelNode setTimeoutOperation = Operations.createWriteAttributeOperation(listenerAddress.toModelNode(), "write-timeout", SMALL_TIMEOUT);
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        final ModelNode response = controllerClient.execute(setTimeoutOperation);
        Assert.assertTrue(Operations.isSuccessfulOutcome(response));
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        //trigger other side and read what we get and wait for bean to get info
        final String message = "POST /"+DEPLOYMENT_NAME+WRITE_SERVLET_PATH+" HTTP/1.1\r\n" +
                "JSESSIONID: MY_ID2\r\n" +
                "Expect: 100-continue\r\n" +
                "Content-Length: 16\r\n" +
                "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                "Host: "+getServerAddress()+":"+getServerPort()+"\r\n" +
                "Connection: Keep-Alive\r\n\r\nMy HTTP Request!";
        try(Socket socket = new Socket()){
            socket.connect(new InetSocketAddress(getServerAddress(), getServerPort()), 5000);
            sendMessageToServer(socket, message, false);
            Thread.sleep(SMALL_TIMEOUT*2);
            sendMessageToServer(socket, message, false);
            final List<ActionSequence> expectedResult = new ArrayList<ActionSequence>();
            expectedResult.add(ActionSequence.MSG_RECEIVED);
            expectedResult.add(ActionSequence.MSG_SENT);
            expectedResult.add(ActionSequence.MSG_RECEIVED);
            expectedResult.add(ActionSequence.IO_EXCEPTION);
            final List<ActionSequence> result = log.getTestResult();
            Assert.assertEquals(expectedResult.size(), result.size());
            for(int index = 0; index < expectedResult.size() ; index++) {
                Assert.assertEquals(expectedResult+":"+result,expectedResult.get(index), result.get(index));
            }
        }
    }

    protected TimeoutLog lookupEJB() throws NamingException {
        final Context context = getInitialContext();
        return (TimeoutLog) context.lookup("ejb:/sockettimeout/TimeoutLogBean!org.jboss.as.test.integration.web.timeout.TimeoutLog");
    }

    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }
    protected void sendMessageToServer(final Socket socket, final String message, final boolean failOnNOReturn) throws IOException, InterruptedException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        doRead(socket,failOnNOReturn);
    }

    protected String readStream(final InputStream stream) throws IOException {

        byte[] buf = new byte[100];
        StringBuilder sb = new StringBuilder();
        while(stream.available() > 0) {
            int r = stream.read(buf);
            sb.append(new String(buf, 0, r));
        }
        return sb.toString();
    }

    protected void doRead(final Socket socket, final boolean failOnNoMessage) throws InterruptedException, IOException {
        final CountDownLatch latch = new CountDownLatch(10);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket.getInputStream().available() <= 0) {
                        latch.countDown();
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    Assert.assertNull(e.getMessage());
                }
            }
        };
        new Thread(r).start();
        latch.await(TimeoutUtil.adjust(1000), TimeUnit.MILLISECONDS);
        if(socket.getInputStream().available() > 0) {
            final String httpResponse = readStream(socket.getInputStream());
            Assert.assertTrue(httpResponse, httpResponse.contains("100"));
        } else {
            if(failOnNoMessage) {
                Assert.assertNull("Failed to receive response");
            }
        }
    }

    protected String getServerAddress() {
        return url.getHost();
    }

    protected int getServerPort() {
        return url.getPort();
    }

    protected abstract PathAddress getListenerAddress();
}
