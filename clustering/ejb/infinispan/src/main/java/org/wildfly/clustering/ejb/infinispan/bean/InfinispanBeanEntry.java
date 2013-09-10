/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.ejb.infinispan.BeanEntry;

import java.util.Date;

/**
 * The cache entry for a bean.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 */
public class InfinispanBeanEntry<G> implements BeanEntry<G> {

    private final G groupId;
    private volatile Date lastAccessedTime = new Date();

    public InfinispanBeanEntry(G groupId) {
        this.groupId = groupId;
    }

    @Override
    public G getGroupId() {
        return this.groupId;
    }

    @Override
    public Date getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setLastAccessedTime(Date time) {
        this.lastAccessedTime = time;
    }
}
