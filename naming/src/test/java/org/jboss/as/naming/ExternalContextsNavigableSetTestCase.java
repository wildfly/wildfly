/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming;

import org.jboss.as.naming.context.external.ExternalContextsNavigableSet;
import org.jboss.msc.service.ServiceName;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author emmartins
 */
public class ExternalContextsNavigableSetTestCase {
    /**
     * Asserts {@link ExternalContextsNavigableSet#getParentExternalContext(ServiceName)}
     * @throws Exception
     */
    @Test
    public void testGetParentContext() throws Exception {
        final ServiceName nameA = ServiceName.JBOSS.append("a");
        final ServiceName nameP = ServiceName.JBOSS.append("p");
        final ServiceName namePC = ServiceName.JBOSS.append("p","c");
        final ServiceName nameZ = ServiceName.JBOSS.append("z");
        ExternalContextsNavigableSet set = new ExternalContextsNavigableSet();
        set.addExternalContext(nameP);
        assertNull(set.getParentExternalContext(nameA));
        assertNull(set.getParentExternalContext(nameP));
        assertNotNull(set.getParentExternalContext(namePC));
        assertEquals(nameP, set.getParentExternalContext(namePC));
        assertNull(set.getParentExternalContext(nameZ));
    }
}
