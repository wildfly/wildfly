/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.sql;

import java.sql.Date;
import java.sql.Time;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.util.DateExternalizer;

/**
 * @author Paul Ferraro
 */
public enum SQLExternalizerProvider implements ExternalizerProvider {

    SQL_DATE(new DateExternalizer<>(Date.class, Date::new)),
    SQL_TIME(new DateExternalizer<>(Time.class, Time::new)),
    SQL_TIMESTAMP(new TimestampExternalizer()),
    ;
    private final Externalizer<?> externalizer;

    SQLExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
