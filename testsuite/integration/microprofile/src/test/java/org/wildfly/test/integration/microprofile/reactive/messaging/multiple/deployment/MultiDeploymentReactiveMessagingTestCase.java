/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.amqp.AmqpMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.kafka.KafkaMessagingBean;
import org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.memory.InVmMessagingBean;

/**
 * Within an EAR file the channel names need to be unique. e.g. We can't use the same channel names for the
 * contained Kafka and AMQP subdeployments
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunKafkaSetupTask.class, RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@DockerRequired
public class MultiDeploymentReactiveMessagingTestCase extends AbstractCliTestBase {

    private static final String BASE_NAME = "multideployment-rm";
    private static final String INVM_BASE_BANE = BASE_NAME + "-invm";
    private static final String KAFKA_BASE_BANE = BASE_NAME + "-kafka";
    private static final String AMQP_BASE_BANE = BASE_NAME + "-amqp";

    private static final int TIMEOUT = TimeoutUtil.adjust(20);

    @ArquillianResource
    URL url;

    @Before
    public void before() throws Exception {
        initCLI();
    }

    @After
    public void after() throws Exception {
        closeCLI();
    }

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        // Empty deployment to satisfy Arquillian
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(EmptyClass.class);
        return ja;
    }

    private Path createInVmDeployment() throws Exception {
        WebArchive inVm = ShrinkWrap.create(WebArchive.class, INVM_BASE_BANE + ".war")
                .addPackage(InVmMessagingBean.class.getPackage())
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return exportArchive(inVm);
    }

    private Path createAmqpDeployment() throws Exception {
        WebArchive amqp = ShrinkWrap.create(WebArchive.class, AMQP_BASE_BANE + ".war")
            .addPackage(AmqpMessagingBean.class.getPackage())
            .addAsWebInfResource(AmqpMessagingBean.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
            .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return exportArchive(amqp);
    }

    private Path createKafkaDeployment() throws Exception {
        WebArchive kafka = ShrinkWrap.create(WebArchive.class, KAFKA_BASE_BANE + ".war")
            .addPackage(KafkaMessagingBean.class.getPackage())
            .addAsWebInfResource(KafkaMessagingBean.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
            .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return exportArchive(kafka);
    }

    private Path exportArchive(Archive<?> archive) throws Exception{
        Path p = Paths.get("./target", archive.getName()).toAbsolutePath();
        if (Files.exists(p)) {
            Files.delete(p);
        }
        archive.as(ZipExporter.class).exportTo(p.toFile());
        return p;
    }

    @Test
    public void testMultipleReactiveMessagingModules() throws Exception {
        List<Path> archives = new ArrayList<>();
        archives.add(createInVmDeployment());
        archives.add(createKafkaDeployment());
        archives.add(createAmqpDeployment());

        List<String> deployed = new ArrayList<>();

        try {

            for (Path p : archives) {
                String name = p.getFileName().toString();
                cli.sendLine("deploy " + p);
                deployed.add(name);
            }

            String inVmUrl = getBaseURL(url) + INVM_BASE_BANE;
            String kafkaUrl = getBaseURL(url) + KAFKA_BASE_BANE;
            String amqpUrl = getBaseURL(url) + AMQP_BASE_BANE;

            try (CloseableHttpClient client = HttpClientBuilder.create().build()){
                postData(client, inVmUrl, "VM-1");
                postData(client, inVmUrl, "VM-2");
                postData(client, inVmUrl, "VM-3");

                postData(client, kafkaUrl, "KF-1");
                postData(client, kafkaUrl, "KF-2");
                postData(client, kafkaUrl, "KF-3");

                postData(client, amqpUrl, "AM-1");
                postData(client, amqpUrl, "AM-2");
                postData(client, amqpUrl, "AM-3");

                checkData(client, inVmUrl, TIMEOUT, "VM-1", "VM-2", "VM-3");
                checkData(client, kafkaUrl, TIMEOUT, "KF-1", "KF-2", "KF-3");
                checkData(client, amqpUrl, TIMEOUT, "AM-1", "AM-2", "AM-3");
            }
        } finally {
            try {
                for (int i = deployed.size() - 1 ; i >= 0 ; i--) {
                    String name = deployed.get(i);
                    cli.sendLine("undeploy " + name);
                }
            } finally {
                for (Path p : archives) {
                    if (Files.exists(p)) {
                        Files.delete(p);
                    }
                }
            }
        }
    }

    private String deploy(Path p) throws Exception {
        String name = p.getFileName().toString();
        cli.sendLine("deploy --url=" + p.toUri().toURL().toExternalForm() + " --name=" + name + " --headers={rollback-on-runtime-failure=false}");
        return name;
    }

    private void postData(CloseableHttpClient client, String url, String value) throws Exception {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("value", value));
        post.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = client.execute(post);){
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    private void checkData(CloseableHttpClient client, String url, int timeoutSeconds, String... expected) throws Exception {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        List<String> lines = Collections.emptyList();
        boolean ok = false;
        while (System.currentTimeMillis() < end) {
            lines = getData(client, url);
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
            Assert.fail("Timeout reading " + url);
        }
    }

    private List<String> getData(CloseableHttpClient client, String url) throws Exception {
        HttpGet get = new HttpGet(url);
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

    private String getUrl() {
        return url.toString() + "/" + BASE_NAME + "/";
//        int i = s.lastIndexOf('/');
//        s = s.substring(0, i + i);
//        s += BASE_NAME;
//        return s;
    }
}
