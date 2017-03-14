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

package org.wildfly.clustering.ejb.infinispan;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroupKey;

/**
 * @author Paul Ferraro
 */
public class KeyMapperTestCase {
    @Test
    public void test() {
        TwoWayKey2StringMapper mapper = new KeyMapper();
        Assert.assertTrue(mapper.isSupportedType(InfinispanBeanKey.class));
        Assert.assertTrue(mapper.isSupportedType(InfinispanBeanGroupKey.class));

        Set<String> formatted = new HashSet<>();

        SessionID id = new UUIDSessionID(UUID.randomUUID());
        BeanKey<SessionID> beanKey = new InfinispanBeanKey<>(id);

        String formattedBeanKey = mapper.getStringMapping(beanKey);
        Assert.assertEquals(beanKey, mapper.getKeyMapping(formattedBeanKey));
        Assert.assertTrue(formatted.add(formattedBeanKey));

        BeanGroupKey<SessionID> beanGroupKey = new InfinispanBeanGroupKey<>(id);

        String formattedBeanGroupKey = mapper.getStringMapping(beanGroupKey);
        Assert.assertEquals(beanGroupKey, mapper.getKeyMapping(formattedBeanGroupKey));
        Assert.assertTrue(formatted.add(formattedBeanGroupKey));
    }
}
