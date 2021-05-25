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
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
public class PublisherSignatureTestCase {

    @Deployment
    public static WebArchive enableExtensions() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-publisher-signature.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(PublisherSignatureTestCase.class,
                        BeanProducingAPublisherOfMessage.class,
                        BeanProducingAPublisherOfPayload.class,
                        BeanProducingAPublisherBuilderOfMessage.class,
                        BeanProducingAPublisherBuilderOfPayload.class,
                        BeanProducingPayloads.class,
                        BeanProducingMessages.class,
                        BeanProducingPayloadsAsynchronously.class,
                        BeanProducingMessagesAsynchronously.class,
                        Spy.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }

    @Inject
    BeanProducingAPublisherOfMessage beanProducingAPublisherOfMessage;
    @Inject
    BeanProducingAPublisherOfPayload beanProducingAPublisherOfPayload;
    @Inject
    BeanProducingAPublisherBuilderOfMessage beanProducingAPublisherBuilderOfMessage;
    @Inject
    BeanProducingAPublisherBuilderOfPayload beanProducingAPublisherBuilderOfPayload;
    @Inject
    BeanProducingPayloads beanProducingPayloads;
    @Inject
    BeanProducingMessages beanProducingMessages;
    @Inject
    BeanProducingPayloadsAsynchronously beanProducingPayloadsAsynchronously;
    @Inject
    BeanProducingMessagesAsynchronously beanProducingMessagesAsynchronously;

    @After
    public void closing() {
        beanProducingAPublisherOfMessage.close();
        beanProducingAPublisherOfPayload.close();
        beanProducingAPublisherBuilderOfMessage.close();
        beanProducingAPublisherBuilderOfPayload.close();
        beanProducingPayloads.close();
        beanProducingMessages.close();
        beanProducingPayloadsAsynchronously.close();
        beanProducingMessagesAsynchronously.close();
    }

    @Test
    public void test() {
        check(beanProducingAPublisherBuilderOfMessage);
        check(beanProducingAPublisherOfPayload);
        check(beanProducingAPublisherBuilderOfMessage);
        check(beanProducingAPublisherBuilderOfPayload);
        check(beanProducingPayloads);
        check(beanProducingMessages);
        check(beanProducingPayloadsAsynchronously);
        check(beanProducingMessagesAsynchronously);
    }

    private void check(Spy spy) {
        await(() -> spy.getItems().size() == 10);
        checkList(spy.getItems(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfMessage extends Spy {

        @Outgoing("A")
        public Publisher<Message<Integer>> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .map(Message::of)
                    .buildRs();
        }

        @Incoming("A")
        public void consume(Integer item) {
            items.add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfPayload extends Spy {
        @Outgoing("B")
        public Publisher<Integer> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .buildRs();
        }

        @Incoming("B")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfMessage extends Spy {
        @Outgoing("C")
        public PublisherBuilder<Message<Integer>> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .map(Message::of);
        }

        @Incoming("C")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfPayload extends Spy {
        @Outgoing("D")
        public PublisherBuilder<Integer> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        }

        @Incoming("D")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingPayloads extends Spy {

        AtomicInteger count = new AtomicInteger();

        @Outgoing("E")
        public int produce() {
            return count.getAndIncrement();
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("E")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingMessages extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("F")
        public Message<Integer> produce() {
            return Message.of(count.getAndIncrement());
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("F")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingPayloadsAsynchronously extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("G")
        public CompletionStage<Integer> produce() {
            return CompletableFuture.supplyAsync(() -> count.getAndIncrement(), executor);
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("G")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingMessagesAsynchronously extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("H")
        public CompletionStage<Message<Integer>> produce() {
            return CompletableFuture.supplyAsync(() -> count.getAndIncrement(), executor).thenApply(Message::of);
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("H")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    public static class Spy {
        List<Integer> items = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        public List<Integer> getItems() {
            return items;
        }

        public void close() {
            executor.shutdown();
        }

    }
}
