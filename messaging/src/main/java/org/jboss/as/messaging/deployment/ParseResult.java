package org.jboss.as.messaging.deployment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
class ParseResult {

    private final List<JmsDestination> queues = new ArrayList<JmsDestination>();

    private final List<JmsDestination> topics = new ArrayList<JmsDestination>();

    public List<JmsDestination> getQueues() {
        return queues;
    }

    public List<JmsDestination> getTopics() {
        return topics;
    }
}
