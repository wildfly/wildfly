/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
class ParseResult {

    private final List<JmsDestination> queues = new ArrayList<>();

    private final List<JmsDestination> topics = new ArrayList<>();

    public List<JmsDestination> getQueues() {
        return queues;
    }

    public List<JmsDestination> getTopics() {
        return topics;
    }
}
