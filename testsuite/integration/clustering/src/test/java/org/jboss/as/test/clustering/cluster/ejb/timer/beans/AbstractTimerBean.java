/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.annotation.Resource;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import org.wildfly.clustering.group.Group;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractTimerBean implements TimerBean, TimerRecorder {

    private final BlockingQueue<Instant> timeouts = new LinkedBlockingQueue<>();

    @Resource
    TimerService service;
    @Resource(lookup = "java:jboss/clustering/group/ejb")
    private Group group;

    @Override
    public String getNodeName() {
        return this.group.getLocalMember().getName();
    }

    @Override
    public boolean isCoordinator() {
        return this.group.getMembership().isCoordinator();
    }

    @Override
    public void record(Timer timer) {
        Instant now = Instant.now();
        System.out.println(String.format("%s received %s(info=%s, persistent=%s) timeout @ %s", this.getNodeName(), this.getClass().getName(), timer.getInfo(), timer.isPersistent(), now));
        this.timeouts.add(now);
    }

    @Override
    public List<Instant> getTimeouts() {
        List<Instant> result = new LinkedList<>();
        this.timeouts.drainTo(result);
        return result;
    }

    @Override
    public int getTimers() {
        return this.service.getTimers().size();
    }
}
