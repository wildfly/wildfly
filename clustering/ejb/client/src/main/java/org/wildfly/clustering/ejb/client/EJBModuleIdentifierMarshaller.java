package org.wildfly.clustering.ejb.client;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

import java.io.IOException;

public class EJBModuleIdentifierMarshaller implements ProtoStreamMarshaller<EJBModuleIdentifier> {
    private static final int APPLICATION_INDEX = 1;
    private static final int MODULE_INDEX = 2;
    private static final int DISTINCT_INDEX = 3;

    @Override
    public EJBModuleIdentifier readFrom(ProtoStreamReader reader) throws IOException {
        String applicationName = "";
        String moduleName = "";
        String distinctName = "";
        while(!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case APPLICATION_INDEX:
                    applicationName = reader.readString();
                    break;
                case MODULE_INDEX:
                    moduleName = reader.readString();
                    break;
                case DISTINCT_INDEX:
                    distinctName = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new EJBModuleIdentifier(applicationName, moduleName, distinctName);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, EJBModuleIdentifier moduleId) throws IOException {
        if (moduleId.getAppName() != null) {
            writer.writeString(APPLICATION_INDEX, moduleId.getAppName());
        }
        if (moduleId.getModuleName() != null) {
            writer.writeString(MODULE_INDEX, moduleId.getModuleName());
        }
        if (moduleId.getDistinctName() != null) {
            writer.writeString(DISTINCT_INDEX, moduleId.getDistinctName());
        }
    }

    @Override
    public Class<EJBModuleIdentifier> getJavaClass() {
        return EJBModuleIdentifier.class;
    }
}
