/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.persistence.DynamicKeyFormatMapper;

/**
 * {@link TwoWayKey2StringMapper} for Jakarta Enterprise Beans cache keys.
 * @author Paul Ferraro
 */
@MetaInfServices(TwoWayKey2StringMapper.class)
public class KeyMapper extends DynamicKeyFormatMapper {

    public KeyMapper() {
        super(KeyMapper.class.getClassLoader());
    }
}
