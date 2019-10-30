/*
 * Copyright (c) 2004,2016 Red Hat, Inc.,. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Red Hat designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Red Hat in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.wildfly.iiop.openjdk.csiv2;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;
import org.omg.IOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.TaggedComponent;
import org.omg.SSLIOP.SSL;
import org.omg.SSLIOP.SSLHelper;
import org.omg.SSLIOP.TAG_SSL_SEC_TRANS;
import org.wildfly.iiop.openjdk.Constants;

import com.sun.corba.se.impl.encoding.CDRInputStream;
import com.sun.corba.se.impl.encoding.EncapsInputStream;
import com.sun.corba.se.spi.ior.IOR;
import com.sun.corba.se.spi.ior.iiop.AlternateIIOPAddressComponent;
import com.sun.corba.se.spi.ior.iiop.IIOPAddress;
import com.sun.corba.se.spi.ior.iiop.IIOPProfileTemplate;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.IORToSocketInfo;
import com.sun.corba.se.spi.transport.SocketInfo;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

import static java.security.AccessController.doPrivileged;

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

    private static boolean clientRequiresSsl;

    public static void setClientRequiresSSL(final boolean clientRequiresSSL) {
        CSIV2IORToSocketInfo.clientRequiresSsl = clientRequiresSSL;
    }

    public List getSocketInfo(IOR ior) {

        List result = new ArrayList();

        IIOPProfileTemplate iiopProfileTemplate = (IIOPProfileTemplate) ior.getProfile().getTaggedProfileTemplate();
        IIOPAddress primary = iiopProfileTemplate.getPrimaryAddress();
        String hostname = primary.getHost().toLowerCase(Locale.ENGLISH);
        int primaryPort = primary.getPort();

        // NOTE: we could check for 0 (i.e., CSIv2) but, for a
        // non-CSIv2-configured client ORB talking to a CSIv2 configured
        // server ORB you might end up with an empty contact info list
        // which would then report a failure which would not be as
        // instructive as leaving a ContactInfo with a 0 port in the list.
        SocketInfo socketInfo;

        TransportAddress sslAddress = selectSSLTransportAddress(ior);
        SSL ssl = getSSL(ior);
        if (sslAddress != null) {
            socketInfo = createSSLSocketInfo(hostname, sslAddress.port);
        } else if (ssl != null) {
            socketInfo = createSSLSocketInfo(hostname, ssl.port);
        } else {
            // FIXME not all corba object export ssl port
            // if (clientRequiresSsl) {
            // throw new RuntimeException("Client requires SSL but target does not support it");
            // }
            socketInfo = createSocketInfo(hostname, primaryPort);
        }
        result.add(socketInfo);

        addAlternateSocketInfos(iiopProfileTemplate, result);

        return result;
    }

    private SSL getSSL(IOR ior){
        Iterator iter = ior.getProfile().getTaggedProfileTemplate().iteratorById(TAG_SSL_SEC_TRANS.value);
        if(!iter.hasNext()){
            return null;
        }
        ORB orb = ior.getORB();
        TaggedComponent compList = ((com.sun.corba.se.spi.ior.TaggedComponent) iter.next()).getIOPComponent(orb);
        CDRInputStream in = doPrivileged(new PrivilegedAction<CDRInputStream>() {
            @Override
            public CDRInputStream run() {
                return new EncapsInputStream(orb, compList.component_data, compList.component_data.length);
            }
        });
        in.consumeEndian();

        SSL ssl = SSLHelper.read(in);
        boolean targetRequiresSsl = ssl.target_requires > 0;
        boolean targetSupportsSsl = ssl.target_supports >0;
        if(!targetSupportsSsl && clientRequiresSsl){
            throw IIOPLogger.ROOT_LOGGER.serverDoesNotSupportSsl();
        }
        return targetSupportsSsl && (targetRequiresSsl || clientRequiresSsl) ? ssl : null;
    }

    private TransportAddress selectSSLTransportAddress(IOR ior) {
        CompoundSecMechList compoundSecMechList = readCompoundSecMechList(ior);
        if (compoundSecMechList != null) {
            for (CompoundSecMech mech : compoundSecMechList.mechanism_list) {
                TLS_SEC_TRANS sslMech = extractTlsSecTrans(ior, mech);
                if (sslMech == null) {
                    continue;
                }
                boolean targetSupportsSsl = checkSSL(sslMech.target_supports);
                boolean targetRequiresSsl = checkSSL(sslMech.target_requires);
                if(!targetSupportsSsl && clientRequiresSsl){
                    throw IIOPLogger.ROOT_LOGGER.serverDoesNotSupportSsl();
                }
                if (targetSupportsSsl && (targetRequiresSsl || clientRequiresSsl)) {
                    return extractAddress(sslMech);
                }
            }
        }
        return null;
    }

    private boolean checkSSL(int options) {
        return (options & (Integrity.value | Confidentiality.value | DetectReplay.value | DetectMisordering.value
                | EstablishTrustInTarget.value | EstablishTrustInClient.value)) != 0;
    }

    private CompoundSecMechList readCompoundSecMechList(IOR ior) {
        Iterator iter = ior.getProfile().getTaggedProfileTemplate().iteratorById(TAG_CSI_SEC_MECH_LIST.value);
        if (!iter.hasNext()) {
            return null;
        }
        ORB orb = ior.getORB();
        TaggedComponent compList = ((com.sun.corba.se.spi.ior.TaggedComponent) iter.next()).getIOPComponent(orb);
        CDRInputStream in = doPrivileged(new PrivilegedAction<CDRInputStream>() {
            @Override
            public CDRInputStream run() {
                return new EncapsInputStream(orb, compList.component_data, compList.component_data.length);
            }
        });
        in.consumeEndian();
        return CompoundSecMechListHelper.read(in);
    }

    private TLS_SEC_TRANS extractTlsSecTrans(IOR ior, CompoundSecMech mech) {
        TaggedComponent comp = mech.transport_mech;
        if (comp.tag != TAG_TLS_SEC_TRANS.value) {
            return null;
        }
        ORB orb = ior.getORB();
        CDRInputStream in = doPrivileged(new PrivilegedAction<CDRInputStream>() {
            @Override
            public CDRInputStream run() {
                return new EncapsInputStream(orb, comp.component_data, comp.component_data.length);
            }
        });
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

    private SocketInfo createSocketInfo(final String hostname, final int port) {
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

    private SocketInfo createSSLSocketInfo(final String hostname, final int port) {
        return new SocketInfo() {
            public String getType() {
                return Constants.SSL_SOCKET_TYPE;
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
