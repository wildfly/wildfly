/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.hibernate.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.hibernate.Hibernate51CompatibilityTransformer;
import org.junit.Test;

public class Hibernate51CompatibilityTransformerTest {

    @Test
    public void rewriteBitSetType() throws IOException {
        rewrite(BitSetType.class);
    }

    @Test
    public void rewriteProcedureParameterExtractionAwareType() throws IOException {
        rewrite(ProcedureParameterExtractionAwareType.class);
    }

    @Test
    public void rewriteProcedureParameterNamedBinderType() throws IOException {
        rewrite(ProcedureParameterNamedBinderType.class);
    }

    @Test
    public void rewritePersistentCollection() throws IOException {
        rewrite(MyPersistentCollection.class);
    }

    private void rewrite(Class<?> clazz) throws IOException {
        try ( InputStream is = clazz.getClassLoader().getResourceAsStream( clazz.getName().replace( '.', '/' ) + ".class" ) ) {
            byte[] classFileBuffer = new byte[is.available()];
            try ( BufferedInputStream bis = new BufferedInputStream( is ) ) {
                bis.read( classFileBuffer );

                Hibernate51CompatibilityTransformer.getInstance()
                        .transform( clazz.getClassLoader(), clazz.getName(), clazz,
                                clazz.getProtectionDomain(), classFileBuffer );
            }
        }
    }
}
