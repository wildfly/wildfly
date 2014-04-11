package org.jboss.as.jdkorb.naming;

/*
 * JacORB - a free Java ORB
 *
 * Copyright (C) 1997-2012 Gerald Brose / The JacORB Team.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.UserException;
import org.omg.CosNaming.Binding;

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
            throw new INTERNAL("Caught exception destroying Iterator" + e);
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