/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice.distributable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.UnaryOperator;

import jakarta.ejb.ScheduleExpression;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerOperationProvider;

/**
 * @author Paul Ferraro
 */
public class DefaultScheduleTimerOperationProviderTestCase {

    private final ScheduleTimerOperationProvider provider = new DefaultScheduleTimerOperationProvider();

    @Test
    public void test() {
        ImmutableScheduleExpression expression = new SimpleImmutableScheduleExpression(new ScheduleExpression().hour("*").minute("*"));
        UnaryOperator<Instant> operator = this.provider.createOperator(expression);
        Instant lastTimeout = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant nextTimeout = operator.apply(lastTimeout);
        Assert.assertEquals(Duration.between(lastTimeout, nextTimeout), Duration.ofMinutes(1));
        Assert.assertEquals(Duration.between(nextTimeout, operator.apply(nextTimeout)), Duration.ofMinutes(1));
        // Validate that operation is idempotent
        Assert.assertEquals(Duration.between(nextTimeout, operator.apply(nextTimeout)), Duration.ofMinutes(1));
    }

    @Test
    public void testInitialPast() {
        // Verify start date in past
        Instant start = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        ImmutableScheduleExpression expression = new SimpleImmutableScheduleExpression(new ScheduleExpression().hour("*").minute("*").second("*").start(Date.from(start)));
        UnaryOperator<Instant> operator = this.provider.createOperator(expression);
        Instant initialTimeout = operator.apply(null);
        Assert.assertEquals(start, initialTimeout);
        Assert.assertEquals(Duration.between(initialTimeout, operator.apply(initialTimeout)), Duration.ofSeconds(1));
    }

    @Test
    public void testInitialFuture() {
        // Verify start date in future
        Instant start = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        ImmutableScheduleExpression expression = new SimpleImmutableScheduleExpression(new ScheduleExpression().hour("*").minute("*").second("*").start(Date.from(start)));
        UnaryOperator<Instant> operator = this.provider.createOperator(expression);
        Instant initialTimeout = operator.apply(null);
        Assert.assertEquals(start, initialTimeout);
        Assert.assertEquals(Duration.between(initialTimeout, operator.apply(initialTimeout)), Duration.ofSeconds(1));
    }
}
