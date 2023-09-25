/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.jboss.as.test.integration.messaging.jms.context.notclosinginjectedcontext.auxiliary;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.util.concurrent.ScheduledFuture;

/**
 * Start of ejb send message after creation of this bean.
 *
 * @author Gunter Zeilinger <gunterze@gmail.com>, Jiri Ondrusek <jondruse@redhat.com>
 * @since Sep 2018
 */
@Singleton
@Startup
public class StartUp {

    private volatile ScheduledFuture<?> running;

    @Inject
    private Ejb ejb;

    @PostConstruct
    public void init() {
        ejb.send("msg");
    }

    @PreDestroy
    public void destroy() {
        if (running != null) {
            running.cancel(true);
        }
    }
}
