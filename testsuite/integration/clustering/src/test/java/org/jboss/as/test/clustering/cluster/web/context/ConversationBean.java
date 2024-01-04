/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.context;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.inject.Inject;

/**
 * @author Paul Ferraro
 */
@ConversationScoped
public class ConversationBean implements Serializable {
    private static final long serialVersionUID = 3657454752821514883L;

    @Inject
    private Conversation conversation;
    private final AtomicInteger value = new AtomicInteger(0);

    public int increment() {
        if (this.conversation.isTransient()) {
            this.conversation.begin();
        }
        return this.value.incrementAndGet();
    }

    public String getConversationId() {
        return this.conversation.getId();
    }

    public void end() {
        this.conversation.end();
    }
}
