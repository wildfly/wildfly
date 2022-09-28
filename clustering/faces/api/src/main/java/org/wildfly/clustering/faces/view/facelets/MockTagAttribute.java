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
