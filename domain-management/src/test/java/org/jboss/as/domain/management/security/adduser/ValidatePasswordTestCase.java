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

package org.jboss.as.domain.management.security.adduser;

import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test the ValidatePasswordState state
 *
 * @author <a href="mailto:g.grossetie@gmail.com">Guillaume Grossetie</a>
 */
public class ValidatePasswordTestCase extends PropertyTestHelper {

    @Test
    public void testValidatePassword_weakPassword() throws IOException, StartException {
        values.setGroups(ROLES);
        values.setUserName("Donny");
        values.setPassword("admin".toCharArray());

        // At least one validation state must be in error... the password is weak
        ValidatePasswordState validatePasswordState = new ValidatePasswordState(consoleMock, values);
        Iterator<State> iterator = validatePasswordState.getValidationStates().iterator();
        boolean errorState = false;
        while(iterator.hasNext() && !errorState) {
            errorState = iterator.next().execute() instanceof  ErrorState;
        }
        assertTrue(errorState);
    }

    @Test
    public void testValidatePassword_relaxWeakPassword() throws IOException, StartException {
        values.setGroups(ROLES);
        values.setUserName("Hugo");
        values.setPassword("admin".toCharArray());
        // Relax... take it easy!
        values.getOptions().setRelaxPassword(true);

        // Validation states must be empty... no check
        ValidatePasswordState validatePasswordState = new ValidatePasswordState(consoleMock, values);
        assertEquals(0, validatePasswordState.getValidationStates().size());
    }
}
