/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
import java.lang.reflect.Proxy;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 * Validates marshalling of java.lang* objects.
 * @author Paul Ferraro
 */
public abstract class AbstractLangTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractLangTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testBoolean() throws IOException {
        this.factory.createTester().test(true);
    }

    @Test
    public void testByte() throws IOException {
        Tester<Byte> tester = this.factory.createTester();
        for (int i = 0; i < Byte.SIZE; ++i) {
            tester.test(Integer.valueOf((1 << i) - 1).byteValue());
            tester.test(Integer.valueOf(-1 << i).byteValue());
        }
    }

    @Test
    public void testShort() throws IOException {
        Tester<Short> tester = this.factory.createTester();
        for (int i = 0; i < Short.SIZE; ++i) {
            tester.test(Integer.valueOf((1 << i) - 1).shortValue());
            tester.test(Integer.valueOf(-1 << i).shortValue());
        }
    }

    @Test
    public void testInteger() throws IOException {
        Tester<Integer> tester = this.factory.createTester();
        for (int i = 0; i < Integer.SIZE; ++i) {
            tester.test((1 << i) - 1);
            tester.test(-1 << i);
        }
    }

    @Test
    public void testLong() throws IOException {
        Tester<Long> tester = this.factory.createTester();
        for (int i = 0; i < Long.SIZE; ++i) {
            tester.test((1L << i) - 1L);
            tester.test(-1L << i);
        }
    }

    @Test
    public void testFloat() throws IOException {
        Tester<Float> tester = this.factory.createTester();
        tester.test(Float.NEGATIVE_INFINITY);
        tester.test(Float.MIN_VALUE);
        tester.test(0F);
        tester.test(Float.MAX_VALUE);
        tester.test(Float.POSITIVE_INFINITY);
        tester.test(Float.NaN);
    }

    @Test
    public void testDouble() throws IOException {
        Tester<Double> tester = this.factory.createTester();
        tester.test(Double.NEGATIVE_INFINITY);
        tester.test(Double.MIN_VALUE);
        tester.test(0D);
        tester.test(Double.MAX_VALUE);
        tester.test(Double.POSITIVE_INFINITY);
        tester.test(Double.NaN);
    }

    @Test
    public void testCharacter() throws IOException {
        Tester<Character> tester = this.factory.createTester();
        tester.test(Character.MIN_VALUE);
        tester.test('A');
        tester.test(Character.MAX_VALUE);
    }

    @Test
    public void testString() throws IOException {
        Tester<String> tester = this.factory.createTester();
        tester.test("A");
        tester.test(UUID.randomUUID().toString());
    }

    @Test
    public void testBooleanArray() throws IOException {
        boolean[] array = new boolean[] { true, false };
        this.factory.<boolean[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<boolean[][]>createTester().test(new boolean[][] { array, array }, Assert::assertArrayEquals);
        Boolean[] objectArray = new Boolean[] { true, false };
        this.factory.<Boolean[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Boolean[][]>createTester().test(new Boolean[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testByteArray() throws IOException {
        byte[] array = new byte[] { Byte.MIN_VALUE, 0, Byte.MAX_VALUE };
        this.factory.<byte[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<byte[][]>createTester().test(new byte[][] { array, array }, Assert::assertArrayEquals);
        Byte[] objectArray = new Byte[] { Byte.MIN_VALUE, 0, Byte.MAX_VALUE };
        this.factory.<Byte[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Byte[][]>createTester().test(new Byte[][] { objectArray, objectArray}, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray}, Assert::assertArrayEquals);
    }

    @Test
    public void testShortArray() throws IOException {
        short[] array = new short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE };
        this.factory.<short[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<short[][]>createTester().test(new short[][] { array, array }, Assert::assertArrayEquals);
        Short[] objectArray = new Short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE };
        this.factory.<Short[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Short[][]>createTester().test(new Short[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testIntegerArray() throws IOException {
        int[] array = new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE };
        this.factory.<int[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<int[][]>createTester().test(new int[][] { array, array }, Assert::assertArrayEquals);
        Integer[] objectArray = new Integer[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE };
        this.factory.<Integer[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Integer[][]>createTester().test(new Integer[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testLongArray() throws IOException {
        long[] array = new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE };
        this.factory.<long[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<long[][]>createTester().test(new long[][] { array, array }, Assert::assertArrayEquals);
        Long[] objectArray = new Long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE };
        this.factory.<Long[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Long[][]>createTester().test(new Long[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testFloatArray() throws IOException {
        float[] array = new float[] { Float.MIN_VALUE, 0f, Float.MAX_VALUE };
        this.factory.<float[]>createTester().test(array, AbstractLangTestCase::assertArrayEquals);
        this.factory.<float[][]>createTester().test(new float[][] { array, array }, Assert::assertArrayEquals);
        Float[] objectArray = new Float[] { Float.MIN_VALUE, 0f, Float.MAX_VALUE };
        this.factory.<Float[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Float[][]>createTester().test(new Float[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testDoubleArray() throws IOException {
        double[] array = new double[] { Double.MIN_VALUE, 0d, Double.MAX_VALUE };
        this.factory.<double[]>createTester().test(array, AbstractLangTestCase::assertArrayEquals);
        this.factory.<double[][]>createTester().test(new double[][] { array, array }, Assert::assertArrayEquals);
        Double[] objectArray = new Double[] { Double.MIN_VALUE, 0d, Double.MAX_VALUE };
        this.factory.<Double[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Double[][]>createTester().test(new Double[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testCharArray() throws IOException {
        char[] array = new char[] { Character.MIN_VALUE, 'A', Character.MAX_VALUE };
        this.factory.<char[]>createTester().test(array, Assert::assertArrayEquals);
        this.factory.<char[][]>createTester().test(new char[][] { array, array }, Assert::assertArrayEquals);
        Character[] objectArray = new Character[] { Character.MIN_VALUE, 'A', Character.MAX_VALUE };
        this.factory.<Character[]>createTester().test(objectArray, Assert::assertArrayEquals);
        this.factory.<Character[][]>createTester().test(new Character[][] { objectArray, objectArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { objectArray, objectArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testObjectArray() throws IOException {
        String string1 = "foo";
        String string2 = "bar";
        String[] stringArray = new String[] { string1, string2 };
        this.factory.<String[]>createTester().test(stringArray, Assert::assertArrayEquals);
        // Test array with shared object references
        this.factory.<String[]>createTester().test(new String[] { string1, string1 }, Assert::assertArrayEquals);
        this.factory.<String[][]>createTester().test(new String[][] { stringArray, stringArray }, Assert::assertArrayEquals);
        this.factory.<Object[][]>createTester().test(new Object[][] { stringArray, stringArray }, Assert::assertArrayEquals);
    }

    @Test
    public void testNull() throws IOException {
        this.factory.createTester().test(null, Assert::assertSame);
    }

    @Test
    public void testClass() throws IOException {
        Tester<Class<?>> tester = this.factory.createTester();
        tester.test(Object.class, Assert::assertSame);
        tester.test(Integer.class, Assert::assertSame);
        tester.test(Throwable.class, Assert::assertSame);
        tester.test(Exception.class, Assert::assertSame);
    }

    @Test
    public void testException() throws IOException {
        try {
            try {
                try {
                    throw new Error("foo");
                } catch (Throwable e) {
                    throw new RuntimeException("bar", e);
                }
            } catch (Throwable e) {
                throw new Exception(e);
            }
        } catch (Throwable e) {
            this.factory.<Throwable>createTester().test(e, AbstractLangTestCase::assertEquals);
        }
    }

    @Test
    public void testProxy() throws IOException {
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Iterable.class }, new TestInvocationHandler("foo"));

        this.factory.createTester().test(proxy, AbstractLangTestCase::assertProxyEquals);
    }

    private static void assertProxyEquals(Object expected, Object actual) {
        Assert.assertTrue(Proxy.isProxyClass(actual.getClass()));
        TestInvocationHandler actualHandler = (TestInvocationHandler) Proxy.getInvocationHandler(actual);
        TestInvocationHandler expectedHandler = (TestInvocationHandler) Proxy.getInvocationHandler(expected);
        Assert.assertEquals(expectedHandler.getValue(), actualHandler.getValue());
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        Assert.assertArrayEquals(expected, actual, 0);
    }

    private static void assertArrayEquals(double[] expected, double[] actual) {
        Assert.assertArrayEquals(expected, actual, 0);
    }

    private static void assertEquals(Throwable expected, Throwable actual) {
        Assert.assertEquals(expected.getMessage(), actual.getMessage());

        StackTraceElement[] expectedStackTrace = expected.getStackTrace();
        StackTraceElement[] actualStackTrace = expected.getStackTrace();
        // Java 9 adds other fields to stack trace, for which normal equality checks will fail
        Assert.assertEquals(expectedStackTrace.length, actualStackTrace.length);
        for (int i = 0; i < expectedStackTrace.length; ++i) {
            StackTraceElement expectedElement = expectedStackTrace[i];
            StackTraceElement actualElement = actualStackTrace[i];
            Assert.assertEquals(expectedElement.getClassName(), actualElement.getClassName());
            Assert.assertEquals(expectedElement.getMethodName(), actualElement.getMethodName());
            Assert.assertEquals(expectedElement.getFileName(), actualElement.getFileName());
            Assert.assertEquals(expectedElement.getLineNumber(), actualElement.getLineNumber());
        }

        Throwable[] expectedSuppressed = expected.getSuppressed();
        Throwable[] actualSuppressed = actual.getSuppressed();
        Assert.assertEquals(expectedSuppressed.length, actualSuppressed.length);
        for (int i = 0; i < expectedSuppressed.length; ++i) {
            assertEquals(expectedSuppressed[i], actualSuppressed[i]);
        }

        Throwable cause1 = expected.getCause();
        Throwable cause2 = actual.getCause();
        if ((cause1 != null) && (cause2 != null)) {
            assertEquals(cause1, cause2);
        } else {
            Assert.assertSame(cause1, cause2);
        }
    }
}
