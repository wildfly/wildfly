/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Month;
import java.time.YearMonth;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link YearMonth}.
 * @author Paul Ferraro
 */
public class YearMonthExternalizer implements Externalizer<YearMonth> {

    @Override
    public void writeObject(ObjectOutput output, YearMonth value) throws IOException {
        output.writeInt(value.getYear());
        TimeExternalizerProvider.MONTH.cast(Month.class).writeObject(output, value.getMonth());
    }

    @Override
    public YearMonth readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int year = input.readInt();
        Month month = TimeExternalizerProvider.MONTH.cast(Month.class).readObject(input);
        return YearMonth.of(year, month);
    }

    @Override
    public Class<YearMonth> getTargetClass() {
        return YearMonth.class;
    }

    @Override
    public OptionalInt size(YearMonth value) {
        return OptionalInt.of(Integer.BYTES + TimeExternalizerProvider.MONTH.size(value.getMonth()).getAsInt());
    }
}
