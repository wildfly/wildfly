package org.wildfly.build.plugin.model;

import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Stuart Douglas
 */
public class BuildModelParser {

    private final Properties properties;

    private static final QName ROOT_1_0 = new QName(BuildModelParser10.NAMESPACE_1_0, "build");

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public BuildModelParser(Properties properties) {
        this.properties = properties;
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new BuildModelParser10(properties));
    }

    public Build parse(final InputStream input) throws XMLStreamException {

        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        Build build = new Build();
        mapper.parseDocument(build, streamReader);
        return build;
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

}
