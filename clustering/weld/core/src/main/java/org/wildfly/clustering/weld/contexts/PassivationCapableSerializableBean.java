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

package org.wildfly.clustering.weld.contexts;

import java.io.Serializable;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

import org.jboss.weld.bean.ForwardingBean;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableBean<B extends Bean<I> & PassivationCapable, I> extends ForwardingBean<I> implements PassivationCapableContextual<B, I> {
    private static final long serialVersionUID = -840179425829721439L;

    private final String contextId;
    private final B instance;

    public PassivationCapableSerializableBean(String contextId, B instance) {
        this.contextId = contextId;
        this.instance = instance;
    }

    @Override
    public String getId() {
        return this.instance.getId();
    }

    @Override
    public B get() {
        return this.instance;
    }

    @Override
    public Bean<I> delegate() {
        return this.get();
    }

    @Override
    public String getContextId() {
        return this.contextId;
    }

    private Object writeReplace() {
        return (this.instance instanceof Serializable) ? this : new PassivationCapableSerializableContextualProxy<>(this.contextId, this.instance);
    }
}
