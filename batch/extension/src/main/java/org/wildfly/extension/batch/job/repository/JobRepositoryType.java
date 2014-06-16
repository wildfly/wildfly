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

package org.wildfly.extension.batch.job.repository;

import java.util.HashMap;
import java.util.Map;

import org.wildfly.jberet._private.WildFlyBatchLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum JobRepositoryType {
    JDBC {
        @Override
        public String toString() {
            return "jdbc";
        }
    },
    IN_MEMORY {
        @Override
        public String toString() {
            return "in-memory";
        }
    };

    private static final Map<String, JobRepositoryType> MAP;

    static {
        MAP = new HashMap<>();
        MAP.put(JDBC.toString(), JDBC);
        MAP.put(IN_MEMORY.toString(), IN_MEMORY);
    }

    public static JobRepositoryType of(final String value) {
        if (MAP.containsKey(value)) {
            return MAP.get(value);
        }
        throw WildFlyBatchLogger.LOGGER.invalidJobRepositoryType(value);
    }
}
