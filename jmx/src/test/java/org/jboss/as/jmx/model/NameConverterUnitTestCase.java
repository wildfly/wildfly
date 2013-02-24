/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import org.jboss.as.controller.PathElement;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class NameConverterUnitTestCase {

    @Test
    public void testConvertName() {
        Assert.assertEquals("test", NameConverter.convertToCamelCase("test"));
    }

    @Test
    public void testConvertNameWithReplacement() {
        Assert.assertEquals("testOne", NameConverter.convertToCamelCase("test-one"));
    }

    @Test
    public void testCreateAddChildNameWildCard() {
        Assert.assertEquals("addTest", NameConverter.createValidAddOperationName(PathElement.pathElement("test")));
    }

    @Test
    public void testCreateAddChildNameNoWildCard() {
        Assert.assertEquals("addTestOne", NameConverter.createValidAddOperationName(PathElement.pathElement("test", "one")));
    }

    @Test
    public void testCreateAddChildNameWildCardWithReplacement() {
        Assert.assertEquals("addTestOne", NameConverter.createValidAddOperationName(PathElement.pathElement("test-one")));
    }

    @Test
    public void testCreateAddChildNameNoWildCardWithReplacement() {
        Assert.assertEquals("addTestOneTwoThree", NameConverter.createValidAddOperationName(PathElement.pathElement("test-one", "two-three")));
    }

}
