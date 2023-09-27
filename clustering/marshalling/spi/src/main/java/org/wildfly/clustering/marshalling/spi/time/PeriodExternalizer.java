/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Period;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link Period}.
 * @author Paul Ferraro
 */
public class PeriodExternalizer implements Externalizer<Period> {

    @Override
    public void writeObject(ObjectOutput output, Period period) throws IOException {
        output.writeInt(period.getYears());
        output.writeInt(period.getMonths());
        output.writeInt(period.getDays());
    }

    @Override
    public Period readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int years = input.readInt();
        int months = input.readInt();
        int days = input.readInt();
        return Period.of(years, months, days);
    }

    @Override
    public Class<Period> getTargetClass() {
        return Period.class;
    }

    @Override
    public OptionalInt size(Period object) {
        return OptionalInt.of(Integer.BYTES * 3);
    }
}
