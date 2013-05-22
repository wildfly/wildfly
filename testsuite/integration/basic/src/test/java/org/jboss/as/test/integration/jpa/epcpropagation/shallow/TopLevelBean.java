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

package org.jboss.as.test.integration.jpa.epcpropagation.shallow;

import javax.ejb.EJB;
import javax.ejb.Stateful;

/**
 * top-level stateful bean that has no extended persistence context of its own
 *
 * @author Scott Marlow
 */
@Stateful
public class TopLevelBean {

    @EJB
    FirstDAO firstDAO;

    @EJB
    SecondDAO secondDAO;

    /**
     * Failure is expected when the SecondAO bean is invoked, which will bring in a
     * separate extended persistence context
     */
    public void referenceTwoDistinctExtendedPersistenceContextsInSameTX_fail() {
        firstDAO.noop();
        secondDAO.noop();
    }

    /**
     * not expected to Fail (created bean should use the same XPC)
     */
    public void induceCreationViaJNDILookup() {
        firstDAO.induceCreationViaJNDILookup();

    }

    /**
     * not expected to Fail (created bean should use the same XPC)
     */
    public void induceCreationViaTwoLevelJNDILookup() {
        firstDAO.induceTwoLevelCreationViaJNDILookup();

    }

}
