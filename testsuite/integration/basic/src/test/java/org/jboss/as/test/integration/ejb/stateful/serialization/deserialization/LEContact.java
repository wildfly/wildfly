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

import java.io.Serializable;

public class LEContact extends CacheableObject implements Serializable {

    private static final long serialVersionUID = 2989987093308217800L;

    private String _name;
    private int _id;

    public LEContact(final String name) {
        this._id = 0;
        this._name = name;
    }

    public int getId() {
        return this._id;
    }

    public void setId(final int id) {
        this._id = id;
    }

    public String getName() {
        return this._name;
    }

    public void setName(final String name) {
        this._name = name;
    }

    public String toString() {
        return "{\n\t name : " + this.getName() + ",\n" + "\t id : " + this.getId() + " \n" + "\t mutable_one:" + this.is_mutable_one() + " \n" + "\t mutable_two:" + this.is_mutable_two() + "\n"  + "\t mutable_three:" + this.is_mutable_three() + "\n}";
    }
}
