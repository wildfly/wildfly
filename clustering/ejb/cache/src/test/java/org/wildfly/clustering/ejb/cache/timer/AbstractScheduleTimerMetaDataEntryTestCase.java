/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;

/**
 * Abstract unit test for validating schedule-based timer metadata entries.
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public abstract class AbstractScheduleTimerMetaDataEntryTestCase extends AbstractTimerMetaDataEntryTestCase<ScheduleTimerMetaDataEntry<UUID>> {

    @Parameters
    public static Iterable<Map.Entry<ScheduleTimerConfiguration, Method>> parameters() {
        ImmutableScheduleExpression expression = ImmutableScheduleExpressionMarshaller.INSTANCE.createInitialValue().start(Instant.now()).end(Instant.now().plus(Duration.ofHours(1))).year("1970").month("1").dayOfMonth("1").dayOfWeek("1").zone(ZoneId.of("GMT")).hour("0").minute("0").second("0").get();
        ScheduleTimerConfiguration config = () -> expression;
        try {
            Method method = AbstractScheduleTimerMetaDataEntryTestCase.class.getDeclaredMethod("ejbTimeout");
            return List.of(new AbstractMap.SimpleImmutableEntry<>(config, null), Map.entry(config, method));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private final ScheduleTimerConfiguration config;
    private final Method method;

    AbstractScheduleTimerMetaDataEntryTestCase(Map.Entry<ScheduleTimerConfiguration, Method> entry) {
        super(entry.getKey());
        this.config = entry.getKey();
        this.method = entry.getValue();
    }

    @Override
    public ScheduleTimerMetaDataEntry<UUID> apply(UUID context) {
        return (this.method != null) ? new ScheduleTimerMetaDataEntry<>(context, this.config, this.method) : new ScheduleTimerMetaDataEntry<>(context, this.config);
    }

    @Override
    void verifyDefaultState(ScheduleTimerMetaDataEntry<UUID> entry) {
        super.verifyDefaultState(entry);
        this.verifyState(entry);
    }

    @Override
    void verifyOriginalState(ScheduleTimerMetaDataEntry<UUID> entry) {
        super.verifyOriginalState(entry);
        this.verifyState(entry);
    }

    @Override
    void verifyUpdatedState(ScheduleTimerMetaDataEntry<UUID> entry) {
        super.verifyUpdatedState(entry);
        this.verifyState(entry);
    }

    private void verifyState(ScheduleTimerMetaDataEntry<UUID> entry) {
        Assert.assertEquals(this.config.getScheduleExpression().getStart(), entry.getScheduleExpression().getStart());
        Assert.assertEquals(this.config.getScheduleExpression().getEnd(), entry.getScheduleExpression().getEnd());
        Assert.assertEquals(this.config.getScheduleExpression().getYear(), entry.getScheduleExpression().getYear());
        Assert.assertEquals(this.config.getScheduleExpression().getMonth(), entry.getScheduleExpression().getMonth());
        Assert.assertEquals(this.config.getScheduleExpression().getDayOfMonth(), entry.getScheduleExpression().getDayOfMonth());
        Assert.assertEquals(this.config.getScheduleExpression().getDayOfWeek(), entry.getScheduleExpression().getDayOfWeek());
        Assert.assertEquals(this.config.getScheduleExpression().getHour(), entry.getScheduleExpression().getHour());
        Assert.assertEquals(this.config.getScheduleExpression().getMinute(), entry.getScheduleExpression().getMinute());
        Assert.assertEquals(this.config.getScheduleExpression().getSecond(), entry.getScheduleExpression().getSecond());
        Assert.assertEquals(this.config.getScheduleExpression().getZone(), entry.getScheduleExpression().getZone());
        if (this.method != null) {
            Assert.assertEquals(new TimeoutDescriptor(this.method), entry.getTimeoutMatcher());
        } else {
            Assert.assertSame(DefaultTimeoutMatcher.INSTANCE, entry.getTimeoutMatcher());
        }
    }

    void ejbTimeout() {
        // Ignore
    }
}
