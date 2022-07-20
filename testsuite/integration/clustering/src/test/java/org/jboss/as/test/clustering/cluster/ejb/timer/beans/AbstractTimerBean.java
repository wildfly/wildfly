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
