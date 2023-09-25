/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Month;
import java.time.MonthDay;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for a {@link MonthDay}.
 * @author Paul Ferraro
 */
public class MonthDayExternalizer implements Externalizer<MonthDay> {

    @Override
    public void writeObject(ObjectOutput output, MonthDay value) throws IOException {
        TimeExternalizerProvider.MONTH.cast(Month.class).writeObject(output, value.getMonth());
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, value.getDayOfMonth());
    }

    @Override
    public MonthDay readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Month month = TimeExternalizerProvider.MONTH.cast(Month.class).readObject(input);
        int day = IndexSerializer.UNSIGNED_BYTE.readInt(input);
        return MonthDay.of(month, day);
    }

    @Override
    public Class<MonthDay> getTargetClass() {
        return MonthDay.class;
    }

    @Override
    public OptionalInt size(MonthDay value) {
        return OptionalInt.of(TimeExternalizerProvider.MONTH.size(value.getMonth()).getAsInt() + IndexSerializer.UNSIGNED_BYTE.size(value.getDayOfMonth()));
    }
}
