/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.iiop.naming;


import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingListHolder;

/**
 * @author Stuart Douglas
 */
public class BindingIteratorImpl extends org.omg.CosNaming.BindingIteratorPOA {

    private final Binding[] bindings;
    private int pos = 0;

    public BindingIteratorImpl(final Binding[] bindings) {
        this.bindings = bindings;
    }

    @Override
    public boolean next_one(final BindingHolder b) {
        if (pos == bindings.length) {
            return false;
        }
        b.value = bindings[pos++];
        return true;
    }

    @Override
    public boolean next_n(final int how_many, final BindingListHolder bl) {
        int left = bindings.length - pos;
        if (left == 0) {
            bl.value = new Binding[0];
            return false;
        } else if (left <= how_many) {
            bl.value = new Binding[how_many];
            System.arraycopy(bindings, pos, bl.value, 0, how_many);
            pos += how_many;
        } else {
            bl.value = new Binding[left];
            System.arraycopy(bindings, pos, bl.value, 0, left);
            pos = bindings.length;
        }
        return true;
    }

    @Override
    public void destroy() {

    }
}
