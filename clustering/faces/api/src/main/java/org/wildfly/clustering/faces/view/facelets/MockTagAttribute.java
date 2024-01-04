/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.view.facelets;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.view.Location;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;

/**
 * Mock {@link TagAttribute} that implements a fixed {@link #toString()}.
 * @author Paul Ferraro
 */
public class MockTagAttribute extends TagAttribute {

    private final String value;

    public MockTagAttribute(String value) {
        this.value = value;
    }

    @Override
    public boolean getBoolean(FaceletContext ctx) {
        return false;
    }

    @Override
    public int getInt(FaceletContext ctx) {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public MethodExpression getMethodExpression(FaceletContext ctx, Class type, Class[] paramTypes) {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public Object getObject(FaceletContext ctx) {
        return null;
    }

    @Override
    public String getQName() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getValue(FaceletContext ctx) {
        return null;
    }

    @Override
    public Object getObject(FaceletContext ctx, Class type) {
        return null;
    }

    @Override
    public ValueExpression getValueExpression(FaceletContext ctx, Class type) {
        return null;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
