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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.ejb.infinispan.KeyMapper;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;

/**
 * Validates {@link org.infinispan.persistence.keymappers.TwoWayKey2StringMapper} implementations for keys used by distributed timer service.
 * @author Paul Ferraro
 */
public class TimerKeyMapperTestCase {
    @Test
    public void test() throws NoSuchMethodException, SecurityException {
        KeyMapperTester tester = new KeyMapperTester(new KeyMapper());

        UUID id = UUID.randomUUID();
        tester.test(new TimerCreationMetaDataKey<>(id));
        tester.test(new TimerAccessMetaDataKey<>(id));
        tester.test(new TimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 0)));
        tester.test(new TimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 1)));
        tester.test(new TimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 0)));
        tester.test(new TimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 2)));
    }

    void ejbTimeout(Object timer) {
    }
}
