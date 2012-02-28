/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.inject.Inject;

import org.jboss.ejb3.annotation.Clustered;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Clustered
@Remote(RemoteCounter.class)
public class CounterBean implements RemoteCounter {

    @EJB
    private DestructionCounterRemote counter;

    private DecoratorInterface bean;

    @Inject
    public CounterBean(DecoratorInterface bean) {
        if(!"Hello World".equals(bean.getMessage())) {
            throw new RuntimeException("bean was not decorated");
        }
        this.bean = bean;
    }

    public CounterBean() {

    }

    private int count;

    @Override
    public CounterResult increment() {
        this.count++;
        return new CounterResult(this.count, getNodeName());
    }

    @Override
    public CounterResult decrement() {
        this.count--;
        return new CounterResult(this.count, getNodeName());
    }

    @Override
    public CounterResult getCount() {
        return new CounterResult(this.count, getNodeName());
    }

    @Override
    @Remove
    public void remove() {

    }

    private String getNodeName() {
        return System.getProperty("jboss.node.name");
    }

    @PreDestroy
    public void destroy() {
        counter.incrementSFSBDestructionCount();
    }
}
