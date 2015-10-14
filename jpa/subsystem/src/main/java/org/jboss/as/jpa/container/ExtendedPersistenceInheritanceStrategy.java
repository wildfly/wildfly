/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/**
 *  for extended persistence inheritance strategies.
 *
 * @author Scott Marlow
 */
public interface ExtendedPersistenceInheritanceStrategy {

    /**
     * Register the extended persistence context so it is accessible to other SFSB's during the
     * creation process.
     *
     * Used when the SFSB bean is injecting fields (at creation time), after the bean is created but before its
     * PostConstruct has been invoked.
     *
     * @param scopedPuName
     * @param entityManager
     */
    void registerExtendedPersistenceContext(
            String scopedPuName,
            ExtendedEntityManager entityManager);


    /**
     * Check if the current EJB container instance contains the specified extended persistence context.
     *
     * This is expected to be used when the SFSB bean is injecting fields, after it the bean is created but before its
     * PostConstruct has been invoked.
     *
     * @param puScopedName Scoped pu name
     *
     * @return the extended persistence context that matches puScopedName or null if not found
     */
    ExtendedEntityManager findExtendedPersistenceContext(String puScopedName);
}
