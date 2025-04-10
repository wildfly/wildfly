/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.basic;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A basic Mail test case that sends an email using SMTP and retrieves it via POP3
 */
@RunWith(Arquillian.class)
@ServerSetup({ MailTestCase.BatchMailSetup.class })
public class MailTestCase {
    public static MailServerContainer mailServer = new MailServerContainer("src/test/resources/org/jboss/as/test/integration/mail/basic/");

    @AfterClass
    public static void cleanup() {
        if (mailServer.isRunning()) {
            mailServer.stop();
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(TimeoutUtil.class)
                .addClass(MailTesterServlet.class)
                .addAsManifestResource(EmptyAsset.INSTANCE,"beans.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                    new RuntimePermission("getClassLoader"),
                    new SocketPermission("*", "connect,resolve"),
                    new PropertyPermission("ts.timeout.factor", "read")
            ), "permissions.xml");
    }

    @ArquillianResource
    private URL url;

    @BeforeClass
    public static void testRequiresDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    static class BatchMailSetup extends ManagementServerSetupTask {

        static final String SUBSYSTEM_MAIL_SESSION = "/subsystem=mail/mail-session=mail-test-basic";
        static final String SUBSYSTEM_MAIL_SESSION_SMTP_SERVER = SUBSYSTEM_MAIL_SESSION + "/server=smtp";
        static final String SUBSYSTEM_MAIL_SESSION_POP3_SERVER = SUBSYSTEM_MAIL_SESSION + "/server=pop3";
        static final String SMTP_SOCKET_BINDING = "/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-test-basic-smtp-binding";
        static final String POP3_SOCKET_BINDING = "/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-test-basic-pop3-binding";

        static {
            if (AssumeTestGroupUtil.isDockerAvailable()) {
                mailServer.start();
            }
        }

        public BatchMailSetup() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add(SUBSYSTEM_MAIL_SESSION + ":add(jndi-name=java:jboss/mail/mail-test-basic,debug=true)")
                            .add(SMTP_SOCKET_BINDING + ":add(host=%s, port=%s)", mailServer.getMailServerHost(), mailServer.getSMTPMappedPort())
                            .add(POP3_SOCKET_BINDING + ":add(host=%s, port=%s)", mailServer.getMailServerHost(), mailServer.getPOP3MappedPort())
                            .add(SUBSYSTEM_MAIL_SESSION_SMTP_SERVER + ":add(outbound-socket-binding-ref=%s, username=%s, password=%s)", "mail-test-basic-smtp-binding", "user01@james.local", "1234")
                            .add(SUBSYSTEM_MAIL_SESSION_POP3_SERVER + ":add(outbound-socket-binding-ref=%s", "mail-test-basic-pop3-binding")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add(SMTP_SOCKET_BINDING + ":remove()")
                            .add(POP3_SOCKET_BINDING + ":remove()")
                            .add(SUBSYSTEM_MAIL_SESSION + ":remove()")
                            .endBatch()
                            .build())
                    .build());
        }
    }

    @Test
    @RunAsClient
    public void testMail() throws TimeoutException, IOException, ExecutionException {
        HttpRequest.get(url.toExternalForm() + "/mail_test", 10, SECONDS);
    }
}
