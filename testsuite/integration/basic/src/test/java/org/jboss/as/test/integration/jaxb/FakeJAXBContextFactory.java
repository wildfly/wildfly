/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020 Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jaxb;

import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBContextFactory;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;

/**
 * <p>Fake JAXB context factory for testing.</p>
 *
 * @author rmartinc
 */
public class FakeJAXBContextFactory implements JAXBContextFactory {

    private static class FakeJAXBContext extends JAXBContext {

        @Override
        public Unmarshaller createUnmarshaller() throws JAXBException {
            throw new UnsupportedOperationException("Fake JAXB context, method not implemented!");
        }

        @Override
        public Marshaller createMarshaller() throws JAXBException {
            throw new UnsupportedOperationException("Fake JAXB context, method not implemented!");
        }

        @Override
        public Validator createValidator() throws JAXBException {
            throw new UnsupportedOperationException("Fake JAXB context, method not implemented!");
        }

        @Override
        public String toString() {
            return FakeJAXBContext.class.getSimpleName();
        }
    }

    @Override
    public JAXBContext createContext(Class<?>[] types, Map<String, ?> map) throws JAXBException {
        return new FakeJAXBContext();
    }

    @Override
    public JAXBContext createContext(String string, ClassLoader cl, Map<String, ?> map) throws JAXBException {
        return new FakeJAXBContext();
    }
}
