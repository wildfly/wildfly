/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.ConfigureElytronSslContextSetupTask;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;

import java.net.URL;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SslAmqpWithSslConfiguredGloballyTestCase.RunArtemisSslUsernamePasswordSecuredSetupTask.class, EnableReactiveExtensionsSetupTask.class, ConfigureElytronSslContextSetupTask.class})
@TestcontainersRequired
@org.junit.Ignore
public class SslAmqpWithSslConfiguredGloballyTestCase {
    @ArquillianResource
    URL url;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-global-ssl-amqp.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(SslAmqpWithSslConfiguredGloballyTestCase.class.getPackage(), "web.xml")
                .addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class)
                .addClasses(RunArtemisSslUsernamePasswordSecuredSetupTask.class, RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(SslAmqpWithSslConfiguredGloballyTestCase.class.getPackage(), "microprofile-config-ssl-global.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void test() {
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            String value = RestAssured.get(url + "last").asString();
            return value.equalsIgnoreCase(String.valueOf(ProducingBean.HIGH));
        });
    }

    static class RunArtemisSslUsernamePasswordSecuredSetupTask extends RunArtemisAmqpSetupTask {
        public RunArtemisSslUsernamePasswordSecuredSetupTask() {
            super("messaging/amqp/broker-ssl.xml", true);
        }
    }
}
