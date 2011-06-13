/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.AttachmentKey;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Marius Bogoevici
 */
public class DelegatingClassFileTransformer implements ClassFileTransformer {

    private final List<ClassFileTransformer> delegateTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    public static final AttachmentKey<DelegatingClassFileTransformer> ATTACHMENT_KEY = AttachmentKey.create(DelegatingClassFileTransformer.class);

    private volatile boolean active = false;

    public DelegatingClassFileTransformer() {
    }

    public void addTransformer(ClassFileTransformer classFileTransformer) {
        delegateTransformers.add(classFileTransformer);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] originalBuffer) throws IllegalClassFormatException {
        byte[] transformedBuffer = originalBuffer;
        if (active) {
            for (ClassFileTransformer transformer : delegateTransformers) {
                byte[] result = transformer.transform(loader, className, classBeingRedefined, protectionDomain, transformedBuffer);
                if (result != null) {
                    transformedBuffer = result;
                }
            }
        }
        return transformedBuffer;
    }

}
