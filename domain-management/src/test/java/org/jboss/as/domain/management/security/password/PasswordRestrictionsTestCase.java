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

package org.jboss.as.domain.management.security.password;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author baranowb
 * 
 */
public class PasswordRestrictionsTestCase {

    @Test
    public void testLengthRestrictionFail() {
        LengthRestriction lr = new LengthRestriction(2);
        assertFalse(lr.pass("1"));
    }
    
    @Test
    public void testLengthRestrictionPass() {
        LengthRestriction lr = new LengthRestriction(2);
        assertTrue(lr.pass("12"));
    }
    
    @Test
    public void testValueRestrictionFail() {
        ValueRestriction lr = new ValueRestriction("restricted");
        assertFalse(lr.pass("restricted"));
    }
    
    @Test
    public void testValueRestrictionPass() {
        ValueRestriction lr = new ValueRestriction("restricted");
        assertTrue(lr.pass("12"));
    }
    
    @Test
    public void testRegexRestrictionFail() {
        RegexRestriction lr = new RegexRestriction("\\d*","");
        assertFalse(lr.pass("xxxAAA"));
    }
    
    @Test
    public void testRegexRestrictionPass() {
        RegexRestriction lr = new RegexRestriction("x*ax+","");
        assertTrue(lr.pass("xxax"));
    }
}
