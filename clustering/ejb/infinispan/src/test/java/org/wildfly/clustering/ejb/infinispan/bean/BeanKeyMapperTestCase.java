/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Test;
import org.wildfly.clustering.ejb.infinispan.KeyMapper;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;

/**
 * Validates {@link org.infinispan.persistence.keymappers.TwoWayKey2StringMapper} instances for bean-related keys.
 * @author Paul Ferraro
 */
public class BeanKeyMapperTestCase {
    @Test
    public void test() {
        KeyMapperTester tester = new KeyMapperTester(new KeyMapper());

        SessionID id = new UUIDSessionID(UUID.randomUUID());
        tester.test(new InfinispanBeanMetaDataKey<>(id));
        tester.test(new InfinispanBeanGroupKey<>(id));
    }
}
