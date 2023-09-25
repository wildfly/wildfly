/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.TopicConnectionFactory;

@Stateful
public class JMSResourceManagerConnectionFactoryIncrementorBean implements Incrementor {

    @Resource
    private QueueConnectionFactory queueConnectionFactory;

    @Resource
    private TopicConnectionFactory topicConnectionFactory;

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public int increment() {
        return this.count.incrementAndGet();
    }
}
