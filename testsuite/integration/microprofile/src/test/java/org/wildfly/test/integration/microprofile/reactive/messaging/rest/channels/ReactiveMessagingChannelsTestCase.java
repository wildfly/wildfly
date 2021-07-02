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

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.net.SocketPermission;
import java.net.URL;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;

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
                        EmitterToChannelPublisherViaKafkaEndpoint.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        // These are tracked in https://issues.redhat.com/browse/WFLY-15071 and can be reduced
                        // eventually.
                        // This one seems valid
                        new SocketPermission("*", "connect, resolve"),
                        // Really this is only /Users/kabir/.m2/repository/org/jboss/resteasy/resteasy-jaxrs/3.15.1.Final/resteasy-jaxrs-3.15.1.Final.jar
                        // I am doing <<ALL FILES>> simply to not have to figure out the maven repository location and version
                        // The real fix will be in javax.ws.rs.ext.FactoryFinder.find() which should use a privileged
                        // block when loading the services. When it cannot find the RestEasy implementation, it
                        // ends up throwing a ClassNotFoundException since it defaults to the GlassFish implementation
                        // which should not be used. Behind the scenes (which gets swallowed, it throws an
                        // AccessControlException due to the missing file.
                        new FilePermission("<<ALL FILES>>", "read"),
                        // This needs fixing in RestEasy probably
                        new SecurityPermission("insertProvider"),
                        // These can be removed once https://github.com/smallrye/smallrye-reactive-messaging/pull/1334
                        // has been merged and released.
                        new MBeanServerPermission("createMBeanServer"),
                        new MBeanPermission("*", "registerMBean, unregisterMBean"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("modifyThread"),
                        new RuntimePermission("setContextClassLoader")), "permissions.xml");
        return webArchive;
    }

    @Test
    public void testPublisherToChannelPublisher() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            checkData(client, "publisher-to-channel-publisher/poll", "One", "Zwei", "Tres");
        }
    }

    @Test
    public void testEmitterToSubscriber() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"emitter-to-subscriber/publish", "Hello");
            postData(client,"emitter-to-subscriber/publish", "world");
            checkData(client, "emitter-to-subscriber/poll", "Hello", "world");
        }
    }

    @Test
    public void testEmitterToSubscribedPublisherBuilder() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "Hola");
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "mundo");
            postData(client,"emitter-to-subscribed-channel-publisher-builder/publish", "-end-");
            checkData(client, "emitter-to-subscribed-channel-publisher-builder/poll", "Hola", "mundo");
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
            List<String> expected = Arrays.asList("Welcome", "to", "Kafka");
            List<String> list = null;
            while (System.currentTimeMillis() < end) {
                Thread.sleep(200);
                error = null;
                try {
                    list = getData(client, "emitter-to-subscribed-channel-publisher-via-kafka/poll");
                    Assert.assertEquals(expected.size(), list.size());
                    break;
                } catch (AssertionError e) {
                    error = e;
                }
            }

            if (error != null) {
                throw error;
            }

            List<Integer> partitions = getPartitions(client);

            // The data may come on different Kafka partitions and ordering is only per partition so do some extra
            // massaging of the data

            Assert.assertEquals(expected.size(), list.size());
            // Kafka messages only have order per partition, so do some massaging of the data
            Map<Integer, List<String>> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                List<String> values = map.computeIfAbsent(partitions.get(i), ind -> new ArrayList<>());
                values.add(list.get(i));
            }

            for (String s : expected) {
                assertValueNextOnAPartition(map, s);
            }
        }
    }


    private void assertValueNextOnAPartition(Map<Integer, List<String>> map, String value) {
        String found = null;
        int remove = -1;
        for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
            List<String> persons = entry.getValue();
            String s = persons.get(0);
            if (s.equals("data: " + value)) {
                found = s;
                persons.remove(0);
                if (persons.size() == 0) {
                    remove = entry.getKey();
                }
            }
        }
        map.remove(remove);
        Assert.assertNotNull("Could not find " + value, found);
    }

    private List<Integer> getPartitions(CloseableHttpClient client) throws Exception {
        List<String> data = getData(client, "emitter-to-subscribed-channel-publisher-via-kafka/partitions");
        String value = data.get(0);
        // The list will be of the format '[0,0,1]'
        value = value.substring(1, value.length() - 1);
        List<Integer> list = new ArrayList<>();
        String[] values = value.split(",");
        for (int i = 0; i < values.length; i++) {
            list.add(Integer.valueOf( values[i].trim()));
        }
        return list;
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

    private void checkData(CloseableHttpClient client, String path, String... expected) throws Exception {
        List<String> lines = getData(client, path);
        Assert.assertEquals(expected.length, lines.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertTrue(lines.get(i).contains(expected[i]));
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
                    if (line.length() > 0) {
                        lines.add(line);
                    }
                }
            }
        }
        return lines;
    }

}
