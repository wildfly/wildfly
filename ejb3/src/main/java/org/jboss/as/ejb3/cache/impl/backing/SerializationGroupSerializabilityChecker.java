/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.cache.impl.backing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.marshalling.SerializabilityChecker;

/**
 * @author Paul Ferraro
 *
 */
public class SerializationGroupSerializabilityChecker implements SerializabilityChecker {

    private final List<SerializabilityChecker> checkers = new CopyOnWriteArrayList<SerializabilityChecker>();

    public void addSerializabilityChecker(SerializabilityChecker checker) {
        this.checkers.add(checker);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.SerializabilityChecker#isSerializable(java.lang.Class)
     */
    @Override
    public boolean isSerializable(Class<?> clazz) {
        for (SerializabilityChecker checker: this.checkers) {
            if (checker.isSerializable(clazz)) return true;
        }
        return false;
    }
}
