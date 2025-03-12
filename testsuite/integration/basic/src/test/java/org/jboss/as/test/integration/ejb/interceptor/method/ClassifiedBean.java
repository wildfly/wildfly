/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.method;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(SecretInterceptor.class)
public class ClassifiedBean extends AbstractClassifiedBean {

    public String secretMethod() {
        return "Secret";
    }

    @Interceptors(TopSecretInterceptor.class)
    public String topSecretMethod() {
        return "TopSecret";
    }

    public String overloadedMethod(Integer i) {
        return "ArgInt:" + i.toString();
    }

    public String overloadedMethod(String str) {
        return "ArgStr:" + str;
    }

    public String methodWithByteArrayArgument(byte[] array)  {
        return "ByteArray:" + array;
    }

    public String methodWithBooleanArrayArgument(boolean[] array)  {
        return "BooleanArray:" + array;
    }

    public String methodWithCharArrayArgument(char[] array)  {
        return "CharArray:" + array;
    }

    public String methodWithDoubleArrayArgument(double[] array)  {
        return "DoubleArray:" + array;
    }

    public String methodWithFloatArrayArgument(float[] array)  {
        return "FloatArray:" + array;
    }

    public String methodWithIntArrayArgument(int[] array)  {
        return "IntArray:" + array;
    }

    public String methodWithShortArrayArgument(short[] array)  {
        return "ShortArray:" + array;
    }

    public String methodWithLongArrayArgument(long[] array)  {
        return "LongArray:" + array;
    }

    public String methodWithStringArrayArgument(String[] array)  {
        return "Array:" + array;
    }

    public String overridedMethod(String a) {
        return "Str " + a;
    }
}
