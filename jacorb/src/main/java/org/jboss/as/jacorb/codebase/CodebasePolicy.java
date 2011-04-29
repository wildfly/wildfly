/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jacorb.codebase;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

/**
 * <p>
 * This class implements a {@code org.omg.CORBA.Policy} object that contains a codebase string.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CodebasePolicy extends LocalObject implements Policy {

    private final String codebase;

    public static final int TYPE = 0x12345678; // TODO: contact request@omg.org to get a policy type

    /**
     * <p>
     * Creates an instance of {@code CodebasePolicy} with the specified codebase.
     * </p>
     *
     * @param codebase a {@code String} representing the codebase.
     */
    public CodebasePolicy(String codebase) {
        this.codebase = codebase;
    }

    /**
     * <p>
     * Returns the codebase string contained in this {@code Policy}.
     * </p>
     *
     * @return this policy's codebase string.
     */
    public String getCodebase() {
        return this.codebase;
    }

    @Override
    public Policy copy() {
        return new CodebasePolicy(this.codebase);
    }

    @Override
    public void destroy() {
    }

    @Override
    public int policy_type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "CodebasePolicy[" + this.codebase + "]";
    }

}
