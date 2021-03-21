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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractMathTestCase {

    private final MarshallingTesterFactory factory;
    private final Random random = new Random(System.currentTimeMillis());

    public AbstractMathTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    private BigInteger probablePrime() {
        return BigInteger.probablePrime(Byte.MAX_VALUE, this.random);
    }

    @Test
    public void testBigInteger() throws IOException {
        this.testBigInteger(BigInteger.ZERO);
        this.testBigInteger(BigInteger.ONE);
        this.testBigInteger(BigInteger.TEN);
        this.testBigInteger(this.probablePrime());
    }

    private void testBigInteger(BigInteger value) throws IOException {
        this.factory.createTester().test(value);
        this.factory.createTester().test(value.negate());
    }

    @Test
    public void testBigDecimal() throws IOException {
        this.testBigDecimal(BigDecimal.ZERO);
        this.testBigDecimal(BigDecimal.ONE);
        this.testBigDecimal(BigDecimal.TEN);
        this.testBigDecimal(new BigDecimal(this.probablePrime(), Integer.MAX_VALUE));
        this.testBigDecimal(new BigDecimal(this.probablePrime(), Integer.MIN_VALUE));
    }

    private void testBigDecimal(BigDecimal value) throws IOException {
        this.factory.createTester().test(value);
        this.factory.createTester().test(value.negate());
    }

    @Test
    public void testMathContext() throws IOException {
        this.factory.createTester().test(new MathContext(0));
        this.factory.createTester().test(new MathContext(10, RoundingMode.UNNECESSARY));
    }

    @Test
    public void testRoundingMode() throws IOException {
        this.factory.createTester(RoundingMode.class).test();
    }
}
