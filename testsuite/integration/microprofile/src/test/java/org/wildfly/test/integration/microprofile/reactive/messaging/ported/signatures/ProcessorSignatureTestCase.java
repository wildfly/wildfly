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
import java.util.concurrent.CompletionStage;
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
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
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
import org.reactivestreams.Processor;
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
public class ProcessorSignatureTestCase {

    @Deployment
    public static WebArchive enableExtensions() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-processor-signature.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(ProcessorSignatureTestCase.class,
                        BeanProducingAProcessorOfMessage.class,
                        BeanProducingAProcessorOfPayload.class,
                        BeanProducingAProcessorBuilderOfMessage.class,
                        BeanProducingAProcessorBuilderOfPayload.class,
                        BeanProducingAPublisherOfPayload.class,
                        BeanProducingAPublisherOfMessage.class,
                        BeanProducingAPublisherBuilderOfPayload.class,
                        BeanProducingAPublisherBuilderOfMessage.class,
                        BeanConsumingMessages.class,
                        BeanConsumingPayloads.class,
                        BeanConsumingMessagesAsynchronously.class,
                        BeanConsumingPayloadsAsynchronously.class,
                        Spy.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }

    @Inject
    BeanProducingAProcessorOfMessage beanProducingAProcessorOfMessage;
    @Inject
    BeanProducingAProcessorOfPayload beanProducingAProcessorOfPayload;
    @Inject
    BeanProducingAProcessorBuilderOfMessage beanProducingAPublisherBuilderOfMessage;
    @Inject
    BeanProducingAProcessorBuilderOfPayload beanProducingAProcessorBuilderOfPayload;
    @Inject
    BeanProducingAPublisherOfPayload beanProducingAPublisherOfPayload;
    @Inject
    BeanProducingAPublisherOfMessage beanProducingAPublisherOfMessage;
    @Inject
    BeanProducingAPublisherBuilderOfPayload beanProducingAPublisherBuilderOfPayloads;
    @Inject
    BeanProducingAPublisherBuilderOfMessage beanProducingAPublisherBuilderOfMessages;
    @Inject
    BeanConsumingMessages beanConsumingMessages;
    @Inject
    BeanConsumingPayloads beanConsumingPayloads;
    @Inject
    BeanConsumingMessagesAsynchronously beanConsumingMessagesAsynchronously;
    @Inject
    BeanConsumingPayloadsAsynchronously beanConsumingPayloadsAsynchronously;

    @AfterClass
    public static void close() {
        Spy.executor.shutdown();
    }

    @Test
    public void test() {
        check(beanProducingAProcessorOfMessage);
        check(beanProducingAPublisherBuilderOfMessage);
        check(beanProducingAProcessorOfPayload);
        check(beanProducingAProcessorBuilderOfPayload);
        checkDouble(beanProducingAPublisherOfPayload);
        checkDouble(beanProducingAPublisherOfMessage);
        checkDouble(beanProducingAPublisherBuilderOfMessages);
        checkDouble(beanProducingAPublisherBuilderOfPayloads);
        check(beanConsumingMessages);
        check(beanConsumingPayloads);
        check(beanConsumingPayloadsAsynchronously);
        check(beanConsumingMessagesAsynchronously);
    }

    private void check(Spy spy) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                spy.getEmitter().send(i);
            }
            spy.getEmitter().complete();
        }).start();

        await(() -> spy.getItems().size() == 10);
        checkList(spy.getItems(), "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    private void checkDouble(Spy spy) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                spy.getEmitter().send(i);
            }
            spy.getEmitter().complete();
        }).start();

        await(() -> spy.getItems().size() == 20);
        checkList(spy.getItems(), "0", "0", "1", "1", "2", "2", "3", "3", "4", "4",
                "5", "5", "6", "6", "7", "7", "8", "8", "9", "9");
    }

    @ApplicationScoped
    public static class BeanProducingAProcessorOfMessage extends Spy {

        @Inject
        @Channel("A")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("A")
        @Outgoing("AA")
        public Processor<Message<Integer>, Message<String>> process() {
            return ReactiveStreams.<Message<Integer>> builder()
                    .map(m -> Message.of(Integer.toString(m.getPayload())))
                    .buildRs();
        }

        @Incoming("AA")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAProcessorBuilderOfMessage extends Spy {

        @Inject
        @Channel("B")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("B")
        @Outgoing("BB")
        public ProcessorBuilder<Message<Integer>, Message<String>> process() {
            return ReactiveStreams.<Message<Integer>> builder()
                    .map(m -> Message.of(Integer.toString(m.getPayload())));
        }

        @Incoming("BB")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAProcessorOfPayload extends Spy {

        @Inject
        @Channel("C")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("C")
        @Outgoing("CC")
        public Processor<Integer, String> process() {
            return ReactiveStreams.<Integer> builder()
                    .map(m -> Integer.toString(m))
                    .buildRs();
        }

        @Incoming("CC")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAProcessorBuilderOfPayload extends Spy {

        @Inject
        @Channel("D")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("D")
        @Outgoing("DD")
        public ProcessorBuilder<Integer, String> process() {
            return ReactiveStreams.<Integer> builder()
                    .map(m -> Integer.toString(m));
        }

        @Incoming("DD")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfPayload extends Spy {

        @Inject
        @Channel("E")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("E")
        @Outgoing("EE")
        public Publisher<String> process(int item) {
            return ReactiveStreams.of(item, item).map(i -> Integer.toString(i)).buildRs();
        }

        @Incoming("EE")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfMessage extends Spy {

        @Inject
        @Channel("F")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("F")
        @Outgoing("FF")
        public Publisher<Message<String>> process(Message<Integer> item) {
            return ReactiveStreams.of(item.getPayload(), item.getPayload()).map(i -> Integer.toString(i))
                    .map(Message::of)
                    .buildRs();
        }

        @Incoming("FF")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfMessage extends Spy {

        @Inject
        @Channel("G")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("G")
        @Outgoing("GG")
        public PublisherBuilder<Message<String>> process(Message<Integer> item) {
            return ReactiveStreams.of(item.getPayload(), item.getPayload()).map(i -> Integer.toString(i))
                    .map(Message::of);
        }

        @Incoming("GG")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfPayload extends Spy {

        @Inject
        @Channel("H")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("H")
        @Outgoing("HH")
        public PublisherBuilder<String> process(int item) {
            return ReactiveStreams.of(item, item).map(i -> Integer.toString(i));
        }

        @Incoming("HH")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingMessages extends Spy {

        @Inject
        @Channel("I")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("I")
        @Outgoing("II")
        public Message<String> process(Message<Integer> item) {
            return Message.of(Integer.toString(item.getPayload()));
        }

        @Incoming("II")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingPayloads extends Spy {

        @Inject
        @Channel("J")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("J")
        @Outgoing("JJ")
        public String process(Integer item) {
            return Integer.toString(item);
        }

        @Incoming("JJ")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingMessagesAsynchronously extends Spy {

        @Inject
        @Channel("K")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("K")
        @Outgoing("KK")
        public CompletionStage<Message<String>> process(Message<Integer> item) {
            return CompletableFuture.supplyAsync(() -> Message.of(Integer.toString(item.getPayload())), executor);
        }

        @Incoming("KK")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingPayloadsAsynchronously extends Spy {

        @Inject
        @Channel("L")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("L")
        @Outgoing("LL")
        public CompletableFuture<String> process(Integer item) {
            return CompletableFuture.supplyAsync(() -> Integer.toString(item), executor);
        }

        @Incoming("LL")
        public void consume(String item) {
            getItems().add(item);
        }

    }

    public abstract static class Spy {
        List<String> items = new CopyOnWriteArrayList<>();
        static ExecutorService executor = Executors.newSingleThreadExecutor();

        public List<String> getItems() {
            return items;
        }

        abstract Emitter<Integer> getEmitter();

    }
}
