/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld;

import java.beans.FeatureDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

public abstract class ForwardingELResolver extends ELResolver {

    // This method was removed in EL 6.0 so in an EL 5 env we need to invoke the delegate reflectively
    private static final Method GET_FEATURE_DESCRIPTORS;

    static {
        Method m = null;
        try {
            m = ELResolver.class.getDeclaredMethod("getFeatureDescriptors", ELContext.class, Object.class);
        } catch (NoSuchMethodException e) {
            // We're running an EL version where this has been removed
        }
        GET_FEATURE_DESCRIPTORS = m;
    }

    protected abstract ELResolver delegate();

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return delegate().getCommonPropertyType(context, base);
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        try {
            //noinspection unchecked
            return (Iterator<FeatureDescriptor>) GET_FEATURE_DESCRIPTORS.invoke(delegate(), context, base);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return delegate().getType(context, base, property);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        return delegate().getValue(context, base, property);
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return delegate().isReadOnly(context, base, property);
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        delegate().setValue(context, base, property, value);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

}