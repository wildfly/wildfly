/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.marshalling.spi.DelimitedFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * Formatter for a {@link InfinispanTimerIndexKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class InfinispanTimerIndexKeyFormatter extends DelimitedFormatter<InfinispanTimerIndexKey> {

    public InfinispanTimerIndexKeyFormatter() {
        super(InfinispanTimerIndexKey.class, "#", InfinispanTimerIndexKeyFormatter::fromStrings, InfinispanTimerIndexKeyFormatter::toStrings);
    }

    static InfinispanTimerIndexKey fromStrings(String[] values) {
        String className = values[0];
        String methodName = values[1];
        int parameters = (values.length == 3) ? 0 : 1;
        int index = Integer.parseInt(values[values.length - 1]);
        return new InfinispanTimerIndexKey(new TimerIndex(className, methodName, parameters, index));
    }

    static String[] toStrings(InfinispanTimerIndexKey key) {
        TimerIndex index = key.getId();
        int parameters = index.getParameters();
        String indexValue = Integer.toString(index.getIndex());
        return (parameters == 0) ? new String[] { index.getDeclaringClassName(), index.getMethodName(), indexValue } : new String[] { index.getDeclaringClassName(), index.getMethodName(), "", indexValue };
    }
}
