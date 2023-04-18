/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.DelimitedFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * Formatter for a {@link TimerIndexKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class TimerIndexKeyFormatter extends DelimitedFormatter<TimerIndexKey> {

    public TimerIndexKeyFormatter() {
        super(TimerIndexKey.class, "#", TimerIndexKeyFormatter::fromStrings, TimerIndexKeyFormatter::toStrings);
    }

    static TimerIndexKey fromStrings(String[] values) {
        String className = values[0];
        String methodName = values[1];
        int parameters = (values.length == 3) ? 0 : 1;
        int index = Integer.parseInt(values[values.length - 1]);
        return new TimerIndexKey(new TimerIndex(className, methodName, parameters, index));
    }

    static String[] toStrings(TimerIndexKey key) {
        TimerIndex index = key.getId();
        int parameters = index.getParameters();
        String indexValue = Integer.toString(index.getIndex());
        return (parameters == 0) ? new String[] { index.getDeclaringClassName(), index.getMethodName(), indexValue } : new String[] { index.getDeclaringClassName(), index.getMethodName(), "", indexValue };
    }
}
