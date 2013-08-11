/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.infinispan.session.Time;
import org.wildfly.clustering.web.session.SessionMetaData;

import static org.junit.Assert.*;

public class SimpleSessionMetaDataTestCase {
    @Test
    public void isNew() {
        SessionMetaData metaData = new SimpleSessionMetaData();
        assertTrue(metaData.isNew());

        metaData.setLastAccessedTime(new Date());
        assertFalse(metaData.isNew());

        metaData = new SimpleSessionMetaData(new Date(), new Date(), new Time(0, TimeUnit.SECONDS));
        assertFalse(metaData.isNew());
    }

    @Test
    public void isExpired() {
        SessionMetaData metaData = new SimpleSessionMetaData();
        assertFalse(metaData.isExpired());

        Date now = new Date();

        metaData.setMaxInactiveInterval(1, TimeUnit.MINUTES);
        assertFalse(metaData.isExpired());

        metaData.setLastAccessedTime(new Date(now.getTime() - metaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS) - 1));
        assertTrue(metaData.isExpired());
    }
}
