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

package org.jboss.as.jpa.container;


import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Close the non tx invocations on transaction entity manager
 *
 * @author Scott Marlow
 */
public class NonTxEmCloser {

    /**
     * Each thread will have its own list of SB invocations in progress.
     * Key = scoped persistence unit name
     */
    public static ThreadLocalStack<Map<String,EntityManager>> nonTxStack = new ThreadLocalStack<Map<String, EntityManager>>();

    /**
     * entered new session bean invocation, start new collection for tracking transactional entity managers created
     * without a JTA transaction.
     */
    public static void pushCall() {
        nonTxStack.push(new HashMap <String,EntityManager>());
    }

    /**
     * current session bean invocation is ending, close any transactional entity managers created without a JTA
     * transaction.
     */
    public static void popCall() {
        for (EntityManager entityManager: nonTxStack.pop().values()) {
            entityManager.close();
        }
    }

    /**
     * Return the transactional entity manager for the specified scoped persistence unit name
     * @param puScopedName
     * @return
     */
    public static EntityManager get(String puScopedName) {
        Map<String,EntityManager> map = nonTxStack.get();
        if (map != null) {
            return map.get(puScopedName);
        }
        return null;
    }

    public static void add(String puScopedName, EntityManager entityManager) {
        Map<String,EntityManager> map = nonTxStack.get();
        if (map != null) {
            nonTxStack.get().put(puScopedName, entityManager);
        }
    }
}
