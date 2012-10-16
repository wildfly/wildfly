/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.osgi.stilts;

import static org.jboss.as.test.integration.osgi.stilts.bundle.SimpleStomplet.DESTINATION_QUEUE_ONE;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.integration.osgi.stilts.bundle.SimpleStomplet;
import org.jboss.as.test.integration.osgi.stilts.bundle.SimpleStompletActivator;
import org.jboss.as.test.integration.osgi.stilts.bundle.StompletServerActivator;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.Repository;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.ClientTransaction;
import org.projectodd.stilts.stomp.client.MessageHandler;
import org.projectodd.stilts.stomp.client.StompClient;
import org.projectodd.stilts.stomp.client.SubscriptionBuilder;
import org.projectodd.stilts.stomplet.Stomplet;

/**
 * A simple {@link Stomplet} test case.
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Sep-2010
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(StompletTestCase.StompletTestCaseServerSetup.class)
public class StompletTestCase {

    static final String STOMPLET_NAME = "simple-stomplet";

    @ArquillianResource
    ManagementClient managementClient;

    static class StompletTestCaseServerSetup implements ServerSetupTask {

        static final String STOMPLET_SERVER_PROVIDER = "stomplet-server-provider";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelControllerClient client = managementClient.getControllerClient();
            ServerDeploymentHelper server = new ServerDeploymentHelper(client);
            InputStream input = getStompletServerProviderArchive().as(ZipExporter.class).exportAsInputStream();
            server.deploy(STOMPLET_SERVER_PROVIDER, input);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
            server.undeploy(STOMPLET_SERVER_PROVIDER);
        }

        Archive<?> getStompletServerProviderArchive() {
            final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, STOMPLET_SERVER_PROVIDER);
            archive.addClasses(StompletServerActivator.class);
            archive.setManifest(new Asset() {
                @Override
                public InputStream openStream() {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleManifestVersion(2);
                    builder.addBundleActivator(StompletServerActivator.class);
                    builder.addImportPackages(XRequirementBuilder.class, XRequirement.class, Requirement.class, Repository.class);
                    builder.addImportPackages(BundleActivator.class, PackageAdmin.class, ModuleIdentifier.class);
                    return builder.openStream();
                }
            });
            return archive;
        }
    }

    @Deployment(name = STOMPLET_NAME, testable = false)
    public static Archive<?> getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, STOMPLET_NAME);
        archive.addClasses(SimpleStompletActivator.class, SimpleStomplet.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(SimpleStompletActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class);
                builder.addDynamicImportPackages("org.projectodd.stilts.*");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSendWithNoTx() throws Exception {

        StompClient client = new StompClient("stomp://" + managementClient.getMgmtAddress());
        client.connect();

        final Set<String> outbound = new HashSet<String>();
        final CountDownLatch outboundLatch = new CountDownLatch(2);
        SubscriptionBuilder builder = client.subscribe(DESTINATION_QUEUE_ONE);
        builder.withMessageHandler(new MessageHandler() {
            @Override
            public void handle(StompMessage message) {
                String content = message.getContentAsString();
                outbound.add(content);
                outboundLatch.countDown();
            }
        });
        ClientSubscription subscription = builder.start();

        client.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg1"));
        client.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg2"));

        Assert.assertTrue("No latch timeout", outboundLatch.await(10, TimeUnit.SECONDS));
        Assert.assertTrue("Contains msg1", outbound.contains("msg1"));
        Assert.assertTrue("Contains msg2", outbound.contains("msg2"));

        subscription.unsubscribe();
        client.disconnect();
    }

    @Test
    public void testSendWithTxCommit() throws Exception {

        StompClient client = new StompClient("stomp://" + managementClient.getMgmtAddress());
        client.connect();

        final Set<String> outbound = new HashSet<String>();
        final CountDownLatch outboundLatch = new CountDownLatch(2);
        SubscriptionBuilder builder = client.subscribe(DESTINATION_QUEUE_ONE);
        builder.withMessageHandler(new MessageHandler() {
            @Override
            public void handle(StompMessage message) {
                String content = message.getContentAsString();
                outbound.add(content);
                outboundLatch.countDown();
            }
        });
        ClientSubscription subscription = builder.start();

        ClientTransaction tx = client.begin();
        tx.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg1"));
        tx.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg2"));
        tx.commit();

        Assert.assertTrue("No latch timeout", outboundLatch.await(3, TimeUnit.SECONDS));
        Assert.assertTrue("Contains msg1", outbound.contains("msg1"));
        Assert.assertTrue("Contains msg2", outbound.contains("msg2"));

        subscription.unsubscribe();
        client.disconnect();
    }
}
