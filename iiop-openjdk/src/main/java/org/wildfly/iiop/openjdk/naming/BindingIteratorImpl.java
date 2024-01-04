/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.naming;

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
            bl.value = new Binding[0];
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