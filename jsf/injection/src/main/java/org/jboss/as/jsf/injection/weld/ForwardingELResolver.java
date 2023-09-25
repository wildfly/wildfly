/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

public abstract class ForwardingELResolver extends ELResolver {

    protected abstract ELResolver delegate();

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return delegate().getCommonPropertyType(context, base);
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return delegate().getFeatureDescriptors(context, base);
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