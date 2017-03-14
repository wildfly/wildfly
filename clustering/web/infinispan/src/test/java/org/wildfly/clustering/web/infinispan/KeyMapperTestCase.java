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

import java.util.HashSet;
import java.util.Set;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.junit.Assert;
import org.junit.Test;
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
        TwoWayKey2StringMapper mapper = new KeyMapper();
        Assert.assertTrue(mapper.isSupportedType(SessionCreationMetaDataKey.class));
        Assert.assertTrue(mapper.isSupportedType(SessionAccessMetaDataKey.class));
        Assert.assertTrue(mapper.isSupportedType(SessionAttributesKey.class));
        Assert.assertTrue(mapper.isSupportedType(SessionAttributeNamesKey.class));
        Assert.assertTrue(mapper.isSupportedType(SessionAttributeKey.class));
        Assert.assertTrue(mapper.isSupportedType(AuthenticationKey.class));
        Assert.assertTrue(mapper.isSupportedType(CoarseSessionsKey.class));

        Set<String> formatted = new HashSet<>();
        String id = "ABC123";

        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        String formattedCreationMetaDataKey = mapper.getStringMapping(creationMetaDataKey);
        Assert.assertEquals(creationMetaDataKey, mapper.getKeyMapping(formattedCreationMetaDataKey));
        Assert.assertTrue(formatted.add(formattedCreationMetaDataKey));

        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        String formattedAccessMetaDataKey = mapper.getStringMapping(accessMetaDataKey);
        Assert.assertEquals(accessMetaDataKey, mapper.getKeyMapping(formattedAccessMetaDataKey));
        Assert.assertTrue(formatted.add(formattedAccessMetaDataKey));

        SessionAttributesKey attributesKey = new SessionAttributesKey(id);
        String formattedAttributesKey = mapper.getStringMapping(attributesKey);
        Assert.assertEquals(attributesKey, mapper.getKeyMapping(formattedAttributesKey));
        Assert.assertTrue(formatted.add(formattedAttributesKey));

        SessionAttributeNamesKey attributeNamesKey = new SessionAttributeNamesKey(id);
        String formattedAttributeNamesKey = mapper.getStringMapping(attributeNamesKey);
        Assert.assertEquals(attributeNamesKey, mapper.getKeyMapping(formattedAttributeNamesKey));
        Assert.assertTrue(formatted.add(formattedAttributeNamesKey));

        for (int i = 0; i < 10; ++i) {
            SessionAttributeKey attributeKey = new SessionAttributeKey(id, i);
            String formattedAttributeKey = mapper.getStringMapping(attributeKey);
            Assert.assertEquals(attributeKey, mapper.getKeyMapping(formattedAttributeKey));
            Assert.assertTrue(formatted.add(formattedAttributeKey));
        }

        AuthenticationKey authKey = new AuthenticationKey(id);
        String formattedAuthKey = mapper.getStringMapping(authKey);
        Assert.assertEquals(authKey, mapper.getKeyMapping(formattedAuthKey));
        Assert.assertTrue(formatted.add(formattedAuthKey));

        CoarseSessionsKey sessionsKey = new CoarseSessionsKey(id);
        String formattedSessionsKey = mapper.getStringMapping(sessionsKey);
        Assert.assertEquals(sessionsKey, mapper.getKeyMapping(formattedSessionsKey));
        Assert.assertTrue(formatted.add(formattedSessionsKey));
    }
}
