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

import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.TAG_JAVA_CODEBASE;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

/**
 * <p>
 * This class implements an {@code org.omg.PortableInterceptor.IORInterceptor} that adds a Java codebase component
 * to IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CodebaseInterceptor extends LocalObject implements IORInterceptor {

    private Codec codec;

    /**
     * <p>
     * Creates an instance of {@code CodebaseInterceptor} with the specified codec.
     * </p>
     *
     * @param codec the {@code Codec} used to encode the codebase string.
     */
    public CodebaseInterceptor(Codec codec) {
        this.codec = codec;
    }

    @Override
    public void establish_components(IORInfo info) {

        CodebasePolicy codebasePolicy = (CodebasePolicy) info.get_effective_policy(CodebasePolicy.TYPE);

        if (codebasePolicy != null) {
            // get codebase string from CodebasePolicy.
            String codebase = codebasePolicy.getCodebase();

            // encapsulate codebase string into TaggedComponent.
            Any any = ORB.init().create_any();
            any.insert_string(codebase);
            byte[] taggedComponentData;
            try {
                taggedComponentData = this.codec.encode_value(any);
            } catch (InvalidTypeForEncoding e) {
                throw new RuntimeException("Exception establishing Java codebase component:" + e);
            }
            info.add_ior_component(new TaggedComponent(TAG_JAVA_CODEBASE.value, taggedComponentData));
        }
    }

    @Override
    public String name() {
        return CodebaseInterceptor.class.getName();
    }

    @Override
    public void destroy() {
    }

}
