/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.context;

import java.util.Enumeration;
import javax.naming.RefAddr;
import javax.naming.Reference;

/**
 * Holder object to maintain an association between a reference and the classloader of the binding caller.
 *
 * @author John Bailey
 */
public class ReferenceWithClassLoader extends Reference {
    private static final long serialVersionUID = 6226293401536779201L;
    private final Reference reference;
    private final transient ClassLoader bindingLoader;

    public ReferenceWithClassLoader(final Reference reference, final ClassLoader bindingLoader) {
        super(reference.getClassName());
        this.reference = reference;
        this.bindingLoader = bindingLoader;
    }

    public ClassLoader getBindingLoader() {
        return bindingLoader;
    }

    public String getClassName() {
        return reference.getClassName();
    }

    public String getFactoryClassName() {
        return reference.getFactoryClassName();
    }

    public String getFactoryClassLocation() {
        return reference.getFactoryClassLocation();
    }

    public RefAddr get(String addrType) {
        return reference.get(addrType);
    }

    public RefAddr get(int posn) {
        return reference.get(posn);
    }

    public Enumeration<RefAddr> getAll() {
        return reference.getAll();
    }

    public int size() {
        return reference.size();
    }

    public void add(RefAddr addr) {
        reference.add(addr);
    }

    public void add(int posn, RefAddr addr) {
        reference.add(posn, addr);
    }

    public Object remove(int posn) {
        return reference.remove(posn);
    }

    public void clear() {
        reference.clear();
    }

    public int hashCode() {
        return reference.hashCode();
    }

    public String toString() {
        return reference.toString();
    }
}
