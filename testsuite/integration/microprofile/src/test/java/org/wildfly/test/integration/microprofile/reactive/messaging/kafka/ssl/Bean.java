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

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class Bean {
    private final CountDownLatch latch = new CountDownLatch(4);
    private List<String> words = new ArrayList<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PreDestroy
    public void stop() {
        executorService.shutdown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    @Outgoing("to-kafka")
    public PublisherBuilder<String> source() {
        // We need to set the following in microprofile-config.properties for this approach to work
        //  mp.messaging.incoming.from-kafka.auto.offset.reset=earliest
        return ReactiveStreams.of("hello", "reactive", "messaging", "ssl");
    }

    @Incoming("from-kafka")
    public void sink(String word) {
        words.add(word);
        latch.countDown();
    }

    public List<String> getWords() {
        return words;
    }
}
