/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.hibernate.management;

import org.jboss.as.jpa.hibernate5.management.QueryName;
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test calls QueryName.displayable() and checks amount of CPU it consumes.
 * Test for [ WFLY-9720 ].
 *
 * @author Daniel Cihak
 */
public class QueryNameDisplayableTestCase {

    private static final int TIMEOUT = TimeoutUtil.adjust(3000);

    @Test
    public void testQueryNameDisplayable() {
        String test = "select * from employee e (where e.id <> 0 and e.name ='foo' and e.other != 'other'";
        long start = System.currentTimeMillis();

        for ( int looper = 0; looper < 500000; looper ++) {
            QueryName name = new QueryName(test);
        }
        long end = System.currentTimeMillis();
        long duration = (end - start);

        Assert.assertTrue("Duration of QueryName.displayable called in a 500000 loop must be lower than 3000 milliseconds, but was " + duration, duration < TIMEOUT);
    }
}
