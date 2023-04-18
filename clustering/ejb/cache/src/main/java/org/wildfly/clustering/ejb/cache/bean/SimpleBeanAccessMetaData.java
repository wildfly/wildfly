/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

/**
 * A simple {@link BeanAccessMetaData} implementation.
 * @author Paul Ferraro
 */
public class SimpleBeanAccessMetaData implements BeanAccessMetaData {

    static SimpleBeanAccessMetaData valueOf(Duration duration) {
        SimpleBeanAccessMetaData metaData = new SimpleBeanAccessMetaData();
        metaData.setLastAccessDuration(duration);
        return metaData;
    }

    private volatile Duration lastAccessDuration = Duration.ZERO;

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccessDuration;
    }

    @Override
    public void setLastAccessDuration(Duration duration) {
        this.lastAccessDuration = duration;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("last-accessed = ").append(this.lastAccessDuration);
        return builder.append(" }").toString();
    }
}
