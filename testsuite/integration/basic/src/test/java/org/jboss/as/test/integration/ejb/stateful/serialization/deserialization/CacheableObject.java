/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.serialization.deserialization;

public class CacheableObject {

    private transient boolean _mutable_one;
    private transient boolean _mutable_two;
    private transient boolean _mutable_three;

    public CacheableObject() {
        this._mutable_one = true;
        this._mutable_two = false;
        this._mutable_three = true;
    }

    public boolean is_mutable_one() {
        return _mutable_one;
    }

    public void set_mutable_one(boolean _mutable_one) {
        this._mutable_one = _mutable_one;
    }

    public boolean is_mutable_two() {
        return _mutable_two;
    }

    public void set_mutable_two(boolean _mutable_two) {
        this._mutable_two = _mutable_two;
    }

    public boolean is_mutable_three() {
        return _mutable_three;
    }

    public void set_mutable_three(boolean _mutable_three) {
        this._mutable_three = _mutable_three;
    }
}
