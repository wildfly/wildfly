/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.stateful;

import javax.ejb.StatefulTimeout;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
public class StatefulTimeoutInfo {

    private final long value;
    private final TimeUnit timeUnit;

    public StatefulTimeoutInfo(final long value, final TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public StatefulTimeoutInfo(final StatefulTimeout statefulTimeout) {
        this.value = statefulTimeout.value();
        this.timeUnit = statefulTimeout.unit();
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getValue() {
        return value;
    }
}
