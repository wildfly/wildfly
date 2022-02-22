package org.jboss.as.weld;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

public class WeldSubsystem50Parser extends PersistentResourceXMLParser {

    public static final String NAMESPACE = "urn:jboss:domain:weld:5.0";
    static final WeldSubsystem50Parser INSTANCE = new WeldSubsystem50Parser();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = PersistentResourceXMLDescription.builder(WeldResourceDefinition.INSTANCE, NAMESPACE)
                .addAttributes(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE,
                        WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE, WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE,
                        WeldResourceDefinition.LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE)
                .build();
    }

    private WeldSubsystem50Parser() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
