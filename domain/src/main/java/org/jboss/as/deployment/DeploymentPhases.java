/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment;

/**
 * Deployment phase constants used to establish general rules for ordering deployment processors.
 * 
 * @author John E. Bailey
 */
public enum DeploymentPhases {
    VALIDATE(0L),
    PARSE_DESCRIPTORS(2000000L),
    MODULARIZE(3000000L),
    //...
    INSTALL_SERVICES(10000000L),
    CLEANUP(20000000L);

    private final long priority;

    private DeploymentPhases(long priority) {
        this.priority = priority;
    }

    public long plus(final long offset) {
        return priority + offset;
    }

    public long getPriority() {
        return priority;
    }
}
