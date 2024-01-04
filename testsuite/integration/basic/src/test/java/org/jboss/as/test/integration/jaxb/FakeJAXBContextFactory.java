/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb;

import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBContextFactory;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

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
