/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
