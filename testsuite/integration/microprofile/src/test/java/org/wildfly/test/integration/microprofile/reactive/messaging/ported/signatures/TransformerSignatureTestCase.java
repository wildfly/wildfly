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

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.signatures;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.await;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.checkList;

import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
public class TransformerSignatureTestCase {

    @Deployment
    public static WebArchive enableExtensions() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-transformer-signature.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(TransformerSignatureTestCase.class,
                        BeanWithPublisherOfMessages.class,
                        BeanWithPublisherBuilderOfMessages.class,
                        BeanWithPublisherOfPayloads.class,
                        BeanWithPublisherBuilderOfPayloads.class,
                        Spy.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }
    @Inject
    BeanWithPublisherOfMessages beanWithPublisherOfMessages;
    @Inject
    BeanWithPublisherOfPayloads beanWithPublisherOfPayloads;
    @Inject
    BeanWithPublisherBuilderOfMessages beanWithPublisherBuilderOfMessages;
    @Inject
    BeanWithPublisherBuilderOfPayloads beanWithPublisherBuilderOfPayloads;

    @AfterClass
    public static void close() {
        Spy.executor.shutdown();
    }

    @Test
    public void test() {
        check(beanWithPublisherOfMessages);
        check(beanWithPublisherOfPayloads);
        check(beanWithPublisherBuilderOfMessages);
        check(beanWithPublisherBuilderOfPayloads);
    }

    private void check(Spy spy) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                spy.getEmitter().send(i);
            }
            spy.getEmitter().complete();
        }).start();

        await(() -> spy.items().size() == 10);
        checkList(spy.items(), "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @ApplicationScoped
    public static class BeanWithPublisherOfMessages extends Spy {

        @Inject
        @Channel("A")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("A")
        @Outgoing("AA")
        public Publisher<Message<String>> process(Publisher<Message<Integer>> publisher) {
            return ReactiveStreams.fromPublisher(publisher)
                    .flatMapCompletionStage(m -> CompletableFuture
                            .supplyAsync(() -> Message.of(Integer.toString(m.getPayload())), executor))
                    .buildRs();
        }

        @Incoming("AA")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherOfPayloads extends Spy {

        @Inject
        @Channel("B")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("B")
        @Outgoing("BB")
        public Publisher<String> process(Publisher<Integer> publisher) {
            return ReactiveStreams.fromPublisher(publisher)
                    .flatMapCompletionStage(i -> CompletableFuture.supplyAsync(() -> Integer.toString(i), executor))
                    .buildRs();
        }

        @Incoming("BB")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherBuilderOfMessages extends Spy {

        @Inject
        @Channel("C")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("C")
        @Outgoing("CC")
        public PublisherBuilder<Message<String>> process(PublisherBuilder<Message<Integer>> publisher) {
            return publisher
                    .flatMapCompletionStage(m -> CompletableFuture
                            .supplyAsync(() -> Message.of(Integer.toString(m.getPayload())), executor));
        }

        @Incoming("CC")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherBuilderOfPayloads extends Spy {

        @Inject
        @Channel("D")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("D")
        @Outgoing("DD")
        public PublisherBuilder<String> process(PublisherBuilder<Integer> publisher) {
            return publisher
                    .flatMapCompletionStage(i -> CompletableFuture.supplyAsync(() -> Integer.toString(i), executor));
        }

        @Incoming("DD")
        public void consume(String item) {
            items().add(item);
        }

    }

    public abstract static class Spy {
        List<String> items = new CopyOnWriteArrayList<>();
        static ExecutorService executor = Executors.newSingleThreadExecutor();

        public List<String> items() {
            return items;
        }

        public void close() {
            executor.shutdown();
        }

        abstract Emitter<Integer> getEmitter();

    }
}

