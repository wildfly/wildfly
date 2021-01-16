/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;

/**
 * @author Paul Ferraro
 */
public class SimpleSessionAccessMetaData implements SessionAccessMetaData {

    private volatile Duration sinceCreation = Duration.ZERO;
    private volatile Duration lastAccess = Duration.ZERO;

    @Override
    public Duration getSinceCreationDuration() {
        return this.sinceCreation;
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccess;
    }

    @Override
    public void setLastAccessDuration(Duration sinceCreation, Duration lastAccess) {
        this.sinceCreation = sinceCreation;
        this.lastAccess = lastAccess;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append('{');
        builder.append("since-creation=").append(this.sinceCreation);
        builder.append("last-access=").append(this.lastAccess);
        return builder.append('}').toString();
    }
}
