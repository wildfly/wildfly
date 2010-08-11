/**
 * 
 */
package org.jboss.as.server;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.jboss.as.Extension;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.as.model.StandaloneParser;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLMapper;

/**
 * A {@link StandardElementReaderRegistrar} that uses a static list of extensions.
 * 
 * @author Brian Stansberry
 */
public class StandardElementReaderRegistrarImpl implements StandardElementReaderRegistrar {
    
    /**
     * Standard modules that include parsing {@link Extension}s.
     */
    private static final List<String> EXTENSION_MODULES = Arrays.asList(new String[] {
            "org.jboss.as:jboss-as-logging",
            "org.jboss.as:jboss-as-threads",
            "org.jboss.as:jboss-as-remoting"
    });
    
    
    public synchronized void registerStandardStandaloneReaders(XMLMapper mapper) throws ModuleLoadException {
        
        for (Namespace ns : Namespace.STANDARD_NAMESPACES) {
            mapper.registerRootElement(new QName(ns.getUriString(), Element.STANDALONE.getLocalName()), StandaloneParser.getInstance());
        }
        
        registerExtensions(mapper);
    }
    
    private static void registerExtensions(XMLMapper mapper) throws ModuleLoadException {
        for (String module : EXTENSION_MODULES) {
            for (Extension extension : Module.loadService(module, Extension.class)) {
                extension.registerElementHandlers(mapper);
            }
        }
    }

}
