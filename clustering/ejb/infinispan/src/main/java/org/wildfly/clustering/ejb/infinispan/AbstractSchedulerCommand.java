/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.RemoveListener;

/**
 * Abstract scheduler command that handles de/serialization of the target bean.
 * @author Paul Ferraro
 */
public abstract class AbstractSchedulerCommand<G, I, T> implements Command<Void, SchedulerContext<G, I, T>> {
    private static final long serialVersionUID = -7937250587845981295L;

    private final transient Bean<G, I, T> bean;
    private final I id;

    protected AbstractSchedulerCommand(Bean<G, I, T> bean) {
        this.bean = bean;
        this.id = bean.getId();
    }

    protected Bean<G, I, T> getBean() {
        return (this.bean != null) ? this.bean : new MockBean<G, I, T>(this.id);
    }

    private static class MockBean<G, I, T> implements Bean<G, I, T> {

        private final I id;

        MockBean(I id) {
            this.id = id;
        }

        @Override
        public I getId() {
            return this.id;
        }

        @Override
        public G getGroupId() {
            return null;
        }

        @Override
        public T acquire() {
            return null;
        }

        @Override
        public boolean release() {
            return false;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public void remove(RemoveListener<T> listener) {
        }

        @Override
        public void close() {
        }
    }
}
