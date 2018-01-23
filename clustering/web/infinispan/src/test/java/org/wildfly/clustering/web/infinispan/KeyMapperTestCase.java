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

package org.wildfly.clustering.web.infinispan;

import java.util.stream.IntStream;

import org.junit.Test;
import org.wildfly.clustering.infinispan.spi.persistence.KeyMapperTester;
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
        IntStream.range(0, 10).mapToObj(i -> new SessionAttributeKey(id, i)).forEach(key -> tester.test(key));

        tester.test(new AuthenticationKey(id));
        tester.test(new CoarseSessionsKey(id));
    }
}
