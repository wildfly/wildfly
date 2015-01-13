package org.wildfly.iiop.openjdk.naming;

/*
 * Copyright (c) 1996, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.omg.CORBA.UserException;
import org.omg.CosNaming.Binding;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Implementation of the "BindingIterator" interface
 *
 * @author Gerald Brose
 */
public class BindingIteratorImpl extends org.omg.CosNaming.BindingIteratorPOA {
    Binding[] bindings;

    int iterator_pos = 0;

    public BindingIteratorImpl(Binding[] b) {
        bindings = b;
        if (b.length > 0)
            iterator_pos = 0;
    }

    public void destroy() {
        bindings = null;
        try {
            _poa().deactivate_object(_poa().servant_to_id(this));
        } catch (UserException e) {
            throw IIOPLogger.ROOT_LOGGER.exceptionDestroingIterator(e.getMessage());
        }
    }

    public boolean next_n(int how_many, org.omg.CosNaming.BindingListHolder bl) {
        int diff = bindings.length - iterator_pos;
        if (diff > 0) {
            Binding[] bndgs = null;
            if (how_many <= diff) {
                bndgs = new Binding[how_many];
                System.arraycopy(bindings, iterator_pos, bndgs, 0, how_many);
                iterator_pos += how_many;
            } else {
                bndgs = new Binding[diff];
                System.arraycopy(bindings, iterator_pos, bndgs, 0, diff);
                iterator_pos = bindings.length;
            }
            bl.value = bndgs;
            return true;
        } else {
            bl.value = new org.omg.CosNaming.Binding[0];
            return false;
        }
    }

    public boolean next_one(org.omg.CosNaming.BindingHolder b) {
        if (iterator_pos < bindings.length) {
            b.value = bindings[iterator_pos++];
            return true;
        } else {
            b.value = new Binding(new org.omg.CosNaming.NameComponent[0], org.omg.CosNaming.BindingType.nobject);
            return false;
        }
    }
}