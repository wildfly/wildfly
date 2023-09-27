/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;
import org.wildfly.clustering.web.infinispan.session.SessionAccessMetaDataKey;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataKey;
import org.wildfly.clustering.web.infinispan.session.coarse.SessionAttributesKey;
import org.wildfly.clustering.web.infinispan.session.fine.SessionAttributeKey;
import org.wildfly.clustering.web.infinispan.session.fine.SessionAttributeNamesKey;
import org.wildfly.clustering.web.infinispan.sso.AuthenticationKey;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsKey;

/**
 * @author Paul Ferraro
 */
public class KeyMapperTestCase {
    @Test
    public void test() {
        KeyMapperTester tester = new KeyMapperTester(new KeyMapper());

        String id = "ABC123";

        tester.test(new SessionCreationMetaDataKey(id));
        tester.test(new SessionAccessMetaDataKey(id));
        tester.test(new SessionAttributesKey(id));
        tester.test(new SessionAttributeNamesKey(id));
        tester.test(new SessionAttributeKey(id, UUID.randomUUID()));

        tester.test(new AuthenticationKey(id));
        tester.test(new CoarseSessionsKey(id));
    }
}
