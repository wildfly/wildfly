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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jacorb.codebase;

import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.PortableInterceptor.PolicyFactory;

/**
 * <p>
 * This class implements a {@code org.omg.PortableInterceptor.PolicyFactory} that creates {@code CodebasePolicy} policies.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class CodebasePolicyFactory extends LocalObject implements PolicyFactory {

    /**
     * <p/>
     * Creates an instance of {@code CodebasePolicyFactory}.
     */
    public CodebasePolicyFactory() {
    }

    @Override
    public Policy create_policy(int type, Any value)
            throws PolicyError {
        if (type != CodebasePolicy.TYPE) {
            throw new PolicyError();
        }
        String codebase = value.extract_string();
        return new CodebasePolicy(codebase);
    }
}
