/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.jdkorb.csiv2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.jdkorb.JdkORBSubsystemConstants;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;
import org.omg.IOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.TaggedComponent;

import com.sun.corba.se.impl.encoding.CDRInputStream;
import com.sun.corba.se.impl.encoding.EncapsInputStream;
import com.sun.corba.se.spi.ior.IOR;
import com.sun.corba.se.spi.ior.iiop.AlternateIIOPAddressComponent;
import com.sun.corba.se.spi.ior.iiop.IIOPAddress;
import com.sun.corba.se.spi.ior.iiop.IIOPProfileTemplate;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.IORToSocketInfo;
import com.sun.corba.se.spi.transport.SocketInfo;

/**
 * <p>
 * Implements an {@code com.sun.corba.se.spi.transport.IORToSocketInfo} which creates SocketInfo based on IOR contents. If CSIv2
 * tagged component is present and it contains {@code org.omg.CSIIOP.TLS_SEC_TRANS} security mechanism then SSL socket is
 * created.
 * </p>
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class CSIV2IORToSocketInfo implements IORToSocketInfo {
    public List getSocketInfo(IOR ior) {

        List result = new ArrayList();

        IIOPProfileTemplate iiopProfileTemplate = (IIOPProfileTemplate) ior.getProfile().getTaggedProfileTemplate();
        IIOPAddress primary = iiopProfileTemplate.getPrimaryAddress();
        String hostname = primary.getHost().toLowerCase();
        int primaryPort = primary.getPort();
        // NOTE: we could check for 0 (i.e., CSIv2) but, for a
        // non-CSIv2-configured client ORB talking to a CSIv2 configured
        // server ORB you might end up with an empty contact info list
        // which would then report a failure which would not be as
        // instructive as leaving a ContactInfo with a 0 port in the list.

        SocketInfo socketInfo;

        TransportAddress sslAddress = extractSSLTransportAddress(ior);
        if (sslAddress != null) {
            socketInfo = createSSLSocketInfo(hostname, sslAddress.port);
        } else {
            socketInfo = createSocketInfo(hostname, primaryPort);
        }
        result.add(socketInfo);

        addAlternateSocketInfos(iiopProfileTemplate, result);

        return result;
    }

    private TransportAddress extractSSLTransportAddress(IOR ior) {
        CompoundSecMechList compoundSecMechList = readCompoundSecMechList(ior);
        if (compoundSecMechList == null || compoundSecMechList.mechanism_list.length == 0) {
            return null;
        }
        // TODO security mechanism selection
        CompoundSecMech mech = compoundSecMechList.mechanism_list[0];
        TLS_SEC_TRANS sslMech = extractTlsSecTrans(ior, mech);
        if(sslMech==null){
            return null;
        }
        return extractAddress(sslMech);
    }

    private CompoundSecMechList readCompoundSecMechList(IOR ior) {
        Iterator iter = ior.getProfile().getTaggedProfileTemplate().iteratorById(TAG_CSI_SEC_MECH_LIST.value);
        if (!iter.hasNext()) {
            return null;
        }
        ORB orb = ior.getORB();
        TaggedComponent compList = ((com.sun.corba.se.spi.ior.TaggedComponent) iter.next()).getIOPComponent(orb);
        CDRInputStream in = new EncapsInputStream(orb, compList.component_data, compList.component_data.length);
        in.consumeEndian();
        return CompoundSecMechListHelper.read(in);
    }

    private TLS_SEC_TRANS extractTlsSecTrans(IOR ior, CompoundSecMech mech) {
        TaggedComponent comp = mech.transport_mech;
        if (comp.tag != TAG_TLS_SEC_TRANS.value) {
            return null;
        }
        ORB orb = ior.getORB();
        CDRInputStream in = new EncapsInputStream(orb, comp.component_data, comp.component_data.length);
        in.consumeEndian();
        return TLS_SEC_TRANSHelper.read(in);
    }

    private TransportAddress extractAddress(TLS_SEC_TRANS sslMech) {
        if (sslMech.addresses.length == 0) {
            return null;
        }
        return sslMech.addresses[0];
    }

    private void addAlternateSocketInfos(IIOPProfileTemplate iiopProfileTemplate, final List result) {
        Iterator iterator = iiopProfileTemplate.iteratorById(TAG_ALTERNATE_IIOP_ADDRESS.value);

        while (iterator.hasNext()) {
            AlternateIIOPAddressComponent alternate = (AlternateIIOPAddressComponent) iterator.next();
            String hostname = alternate.getAddress().getHost().toLowerCase();
            int port = alternate.getAddress().getPort();
            SocketInfo socketInfo = createSocketInfo(hostname, port);
            result.add(socketInfo);
        }
    }

    private SocketInfo createSocketInfo(final String hostname,final int port) {
        return new SocketInfo() {
            public String getType() {
                return SocketInfo.IIOP_CLEAR_TEXT;
            }

            public String getHost() {
                return hostname;
            }

            public int getPort() {
                return port;
            }
        };
    }

    private SocketInfo createSSLSocketInfo(final String hostname,final int port) {
        return new SocketInfo() {
            public String getType() {
                return JdkORBSubsystemConstants.SSL_SOCKET_TYPE;
            }

            public String getHost() {
                return hostname;
            }

            public int getPort() {
                return port;
            }
        };
    }
}
