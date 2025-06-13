/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.amqp.AmqpMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.kafka.KafkaMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.memory.InVmMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.earmodule.war.MultiModuleEndpoint;

/**
 * Within an EAR file the channel names need to be unique. e.g. We can't use the same channel names for the
 * contained Kafka and AMQP subdeployments
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunKafkaSetupTask.class, RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@TestcontainersRequired
@org.junit.Ignore
public class MultiEarModuleReactiveMessagingTestCase {

    private static final String BASE_NAME = "multimodule-rm";

    private static final int TIMEOUT = TimeoutUtil.adjust(20);

    @ArquillianResource
    URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive jaxrs = ShrinkWrap.create(WebArchive.class, BASE_NAME + ".war")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(MultiModuleEndpoint.class.getPackage());

        JavaArchive inVm = ShrinkWrap.create(JavaArchive.class, BASE_NAME + "-invm.jar")
                .addPackage(InVmMessagingBean.class.getPackage())
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        JavaArchive kafka = ShrinkWrap.create(JavaArchive.class, BASE_NAME + "-kafka.jar")
                .addPackage(KafkaMessagingBean.class.getPackage())
                .addAsManifestResource(KafkaMessagingBean.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        JavaArchive amqp = ShrinkWrap.create(JavaArchive.class, BASE_NAME + "-amqp.jar")
                .addPackage(AmqpMessagingBean.class.getPackage())
                .addAsManifestResource(AmqpMessagingBean.class.getPackage(), "microprofile-config.properties", "microprofile-config.properties")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, BASE_NAME + ".ear")
                .addAsModule(jaxrs)
                .addAsLibrary(inVm)
                .addAsLibrary(kafka)
                .addAsLibrary(amqp);

        return ear;
    }

    @Test
    public void testMultipleReactiveMessagingModules() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client, "multi/invm", "VM-1");
            postData(client, "multi/invm", "VM-2");
            postData(client, "multi/invm", "VM-3");

            postData(client, "multi/kafka", "KF-1");
            postData(client, "multi/kafka", "KF-2");
            postData(client, "multi/kafka", "KF-3");

            postData(client, "multi/amqp", "AM-1");
            postData(client, "multi/amqp", "AM-2");
            postData(client, "multi/amqp", "AM-3");

            checkData(client, "multi/invm", TIMEOUT, "VM-1", "VM-2", "VM-3");
            checkData(client, "multi/kafka", TIMEOUT, "KF-1", "KF-2", "KF-3");
            checkData(client, "multi/amqp", TIMEOUT, "AM-1", "AM-2", "AM-3");
        }
    }

    private void postData(CloseableHttpClient client, String path, String value) throws Exception {
        HttpPost post = new HttpPost(url + path);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("value", value));
        post.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = client.execute(post);){
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    private void checkData(CloseableHttpClient client, String path, int timeoutSeconds, String... expected) throws Exception {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        List<String> lines = Collections.emptyList();
        boolean ok = false;
        while (System.currentTimeMillis() < end) {
            lines = getData(client, path);
            if (lines.size() < expected.length) {
                Thread.sleep(2000);
                continue;
            }

            for (int i = 0; i < expected.length; i++) {
                Assert.assertTrue(lines.get(i).contains(expected[i]));
            }
            ok = true;
            break;
        }
        if (ok) {
            Assert.assertEquals(expected.length, lines.size());
        } else {
            Assert.fail("Timeout reading " + path);
        }
    }

    private List<String> getData(CloseableHttpClient client, String path) throws Exception {
        HttpGet get = new HttpGet(url + path);
        List<String> lines = new ArrayList<>();

        try (CloseableHttpResponse response = client.execute(get)){
            assertEquals(200, response.getStatusLine().getStatusCode());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    return ModelNode.fromJSONString(line).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }
}
