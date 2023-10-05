/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import org.junit.Test;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;
import org.wildfly.clustering.web.infinispan.session.attributes.SessionAttributesKey;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKey;
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

        tester.test(new SessionMetaDataKey(id));
        tester.test(new SessionAttributesKey(id));

        tester.test(new AuthenticationKey(id));
        tester.test(new CoarseSessionsKey(id));
    }
}
