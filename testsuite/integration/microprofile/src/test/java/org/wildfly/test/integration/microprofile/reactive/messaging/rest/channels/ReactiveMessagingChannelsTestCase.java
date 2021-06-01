/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.rest.channels;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingChannelsTestCase {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rm-channels-client.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(ReactiveMessagingChannelsTestCase.class.getPackage(), "web.xml")
                .addAsWebInfResource(ReactiveMessagingChannelsTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class, RunKafkaSetupTask.class)
                .addClasses(
                        PublisherToChannelPublisherEndpoint.class,
                        EmitterToSubscriberEndpoint.class,
                        EmitterToSubscribedChannelPublisherBuilderEndpoint.class,
                        EmitterToChannelPublisherViaKafkaEndpoint.class);
        return webArchive;
    }

    @Test
    public void testPublisherToChannelPublisher() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            getData(client, "publisher-to-channel-publisher/poll", "One", "Zwei", "Tres");
        }
    }

    @Test
    public void testEmitterToSubscriber() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"emitter-to-subscriber/publish", "Hello");
            postData(client,"emitter-to-subscriber/publish", "world");
            getData(client, "emitter-to-subscriber/poll", "Hello", "world");
        }
    }

    @Test
    public void testEmitterToSubscribedPublisherBuilder() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "Hola");
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "mundo");
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "-end-");
            getData(client, "emitter-to-subscribed-channel-publisher-builder/poll", "Hola", "mundo");
        }
    }

    @Test
    public void testEmitterToChannelPublisherViaKafka() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"emitter-to-subscribed-channel-publisher-via-kafka/publish", "Welcome");
            postData(client,"emitter-to-subscribed-channel-publisher-via-kafka/publish", "to");
            postData(client,"emitter-to-subscribed-channel-publisher-via-kafka/publish", "Kafka");

            // Kafka is slower than the other in-memory examples, so do some retrying here
            long end = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            AssertionError error = null;
            while (System.currentTimeMillis() < end) {
                Thread.sleep(200);
                error = null;
                try {
                    getData(client, "emitter-to-subscribed-channel-publisher-via-kafka/poll", "Welcome", "to", "Kafka");
                    break;
                } catch (AssertionError e) {
                    error = e;
                }
            }

            if (error != null) {
                throw error;
            }

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

    private void getData(CloseableHttpClient client, String path, String... expected) throws Exception {
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
                    if (line.length() > 0) {
                        lines.add(line);
                    }
                }
            }
        }

        Assert.assertEquals(expected.length, lines.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertTrue(lines.get(i).contains(expected[i]));
        }
    }

}
