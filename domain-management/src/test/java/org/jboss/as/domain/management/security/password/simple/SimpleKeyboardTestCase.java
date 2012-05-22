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

package org.jboss.as.domain.management.security.password.simple;

import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author baranowb
 *
 */
public class SimpleKeyboardTestCase {

    private final String tested = "axcvnhy";

    private SimpleKeyboard keyboard = new SimpleKeyboard();

    @Test
    public void testSiblings(){
        assertFalse(keyboard.siblings(tested, 0));
        assertTrue(keyboard.siblings(tested, 1));
        assertTrue(keyboard.siblings(tested, 2));
        assertFalse(keyboard.siblings(tested, 3));
        assertTrue(keyboard.siblings(tested, 4));
        assertTrue(keyboard.siblings(tested, 5));
    }
    
    @Test
    public void testSequence(){
        assertEquals(0, keyboard.sequence(tested, 0));
        assertEquals(2, keyboard.sequence(tested, 1));
        assertEquals(2, keyboard.sequence(tested, 4));
    }
    
}
