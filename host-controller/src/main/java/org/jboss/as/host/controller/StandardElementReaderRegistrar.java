/**
 *
 */
package org.jboss.as.host.controller;

import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * An object responsible for registering the standard set of {@link XMLElementReader}
 * implementations needed for parsing a domain.xml or a host.xml.
 *
 * @author Brian Stansberry
 */
public interface StandardElementReaderRegistrar {

    void registerStandardHostReaders(XMLMapper mapper) throws ModuleLoadException;

    void registerStandardDomainReaders(XMLMapper mapper) throws ModuleLoadException;

    /**
     * A factory for creating an instance of {@link StandardElementReaderRegistrar}.
     */
    class Factory {

        private static StandardElementReaderRegistrar registrar = new StandardElementReaderRegistrarImpl();
        private Factory() {
        }

        /**
         * Gets a StandardElementHandlerRegistrar instance.
         *
         * @return the registrar instance
         */
        public static StandardElementReaderRegistrar getRegistrar() {
            return registrar;
        }
    }
}
