/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.csiv2;

import java.nio.charset.StandardCharsets;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.jboss.metadata.ejb.jboss.IORASContextMetaData;
import org.jboss.metadata.ejb.jboss.IORSASContextMetaData;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.jboss.metadata.ejb.jboss.IORTransportConfigMetaData;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.SAS_ContextSec;
import org.omg.CSIIOP.ServiceConfiguration;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.GSSUP.InitialContextToken;
import org.omg.GSSUP.InitialContextTokenHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.TAG_NULL_TAG;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.SSLIOP.SSL;
import org.omg.SSLIOP.SSLHelper;
import org.omg.SSLIOP.TAG_SSL_SEC_TRANS;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.service.CorbaORBService;

/**
 * <p>
 * This class defines utility methods for creating, comparing, encoding and decoding CSIv2 components.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public final class CSIv2Util {

    /**
     * DER-encoded ASN.1 representation of the GSSUP mechanism OID.
     */
    private static final byte[] gssUpMechOidArray = createGSSUPMechOID();

    /**
     * <p>
     * Private constructor to implement the singleton pattern.
     * </p>
     */
    private CSIv2Util() {
    }

    /**
     * <p>
     * Make a deep copy of an {@code IOP:TaggedComponent}.
     * </p>
     *
     * @param tc the {@code TaggedComponent} to be copied.
     * @return a reference to the created copy.
     */
    public static TaggedComponent createCopy(TaggedComponent tc) {
        TaggedComponent copy = null;

        if (tc != null) {
            byte[] buf = new byte[tc.component_data.length];
            System.arraycopy(tc.component_data, 0, buf, 0, tc.component_data.length);
            copy = new TaggedComponent(tc.tag, buf);
        }
        return copy;
    }

    /**
     * <p>
     * Return a top-level {@code IOP::TaggedComponent} to be stuffed into an IOR, containing a structure
     * {@code SSLIOP::SSL}, tagged as {@code TAG_SSL_SEC_TRANS}.
     * </p>
     * <p>
     * Should be called with non-null metadata, in which case we probably don't want to include security info in the IOR.
     * </p>
     *
     * @param metadata the metadata object that contains the SSL configuration info.
     * @param codec    the {@code Codec} used to encode the SSL component.
     * @param sslPort  an {@code int} representing the SSL port.
     * @param orb      a reference to the running {@code ORB}.
     * @return a {@code TaggedComponent} representing the encoded SSL component.
     */
    public static TaggedComponent createSSLTaggedComponent(IORSecurityConfigMetaData metadata, Codec codec, int sslPort,
                                                           ORB orb) {
        if (metadata == null) {
            IIOPLogger.ROOT_LOGGER.debug("Method createSSLTaggedComponent() called with null metadata");
            return null;
        }

        if (sslPort == 0) {
            // no support for transport security.
            return null;
        }

        TaggedComponent tc;
        try {
            int supports = createTargetSupports(metadata.getTransportConfig());
            int requires = createTargetRequires(metadata.getTransportConfig());
            SSL ssl = new SSL((short) supports, (short) requires, (short) sslPort);
            Any any = orb.create_any();
            SSLHelper.insert(any, ssl);
            byte[] componentData = codec.encode_value(any);
            tc = new TaggedComponent(TAG_SSL_SEC_TRANS.value, componentData);
        } catch (InvalidTypeForEncoding e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
        return tc;
    }

    /**
     * <p>
     * Return a top-level {@code IOP:TaggedComponent} to be stuffed into an IOR, containing a {@code org.omg.CSIIOP}.
     * {@code CompoundSecMechList}, tagged as {@code TAG_CSI_SEC_MECH_LIST}. Only one such component can exist inside
     * an IOR.
     * </p>
     * <p>
     * Should be called with non-null metadata, in which case we probably don't want to include security info in the IOR.
     * </p>
     *
     * @param metadata the metadata object that contains the CSIv2 security configuration info.
     * @param codec    the {@code Codec} used to encode the CSIv2 security component.
     * @param sslPort  an {@code int} representing the SSL port.
     * @param orb      a reference to the running {@code ORB}.
     * @return a {@code TaggedComponent} representing the encoded CSIv2 security component.
     */
    public static TaggedComponent createSecurityTaggedComponent(IORSecurityConfigMetaData metadata, Codec codec,
                                                                int sslPort, ORB orb) {
        if (metadata == null) {
            IIOPLogger.ROOT_LOGGER.debug("Method createSecurityTaggedComponent() called with null metadata");
            return null;
        }

        TaggedComponent tc;

        // get the the supported security mechanisms.
        CompoundSecMech[] mechList = createCompoundSecMechanisms(metadata, codec, sslPort, orb);

        // the above is wrapped into a org.omg.CSIIOP.CompoundSecMechList structure, which is NOT a CompoundSecMech[].
        // we don't support stateful/reusable security contexts (false).
        CompoundSecMechList csmList = new CompoundSecMechList(false, mechList);
        // finally, the CompoundSecMechList must be encoded as a TaggedComponent
        try {
            Any any = orb.create_any();
            CompoundSecMechListHelper.insert(any, csmList);
            byte[] b = codec.encode_value(any);
            tc = new TaggedComponent(TAG_CSI_SEC_MECH_LIST.value, b);
        } catch (InvalidTypeForEncoding e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
        return tc;
    }

    /**
     * <p>
     * Create a {@code org.omg.CSIIOP.CompoundSecMechanisms} which is a sequence of {@code CompoundSecMech}. Here we only
     * support one security mechanism.
     * </p>
     *
     * @param metadata the metadata object that contains the CSIv2 security configuration info.
     * @param codec    the {@code Codec} used to encode the CSIv2 security component.
     * @param sslPort  an {@code int} representing the SSL port.
     * @param orb      a reference to the running {@code ORB}.
     * @return the constructed {@code CompoundSecMech} array.
     */
    public static CompoundSecMech[] createCompoundSecMechanisms(IORSecurityConfigMetaData metadata, Codec codec,
                                                                int sslPort, ORB orb) {
        // support just 1 security mechanism for now (and ever).
        CompoundSecMech[] csmList = new CompoundSecMech[1];

        // a CompoundSecMech contains: target_requires, transport_mech, as_context_mech, sas_context_mech.
        TaggedComponent transport_mech = createTransportMech(metadata.getTransportConfig(), codec, sslPort, orb);

        // create AS Context.
        AS_ContextSec asContext = createAuthenticationServiceContext(metadata);

        // create SAS Context.
        SAS_ContextSec sasContext = createSecureAttributeServiceContext(metadata);

        // create target_requires bit field (AssociationOption) can't read directly the transport_mech TaggedComponent.
        int target_requires = createTargetRequires(metadata.getTransportConfig()) | asContext.target_requires |
                sasContext.target_requires;

        CompoundSecMech csm = new CompoundSecMech((short) target_requires, transport_mech, asContext, sasContext);
        csmList[0] = csm;

        return csmList;
    }

    /**
     * <p>
     * Create the Secure Attribute Service (SAS) context included in a {@code CompoundSecMech} definition.
     * </p>
     *
     * @param metadata the metadata object that contains the CSIv2 security configuration info.
     * @return the constructed {@code SAS_ContextSec} instance.
     */
    public static SAS_ContextSec createSecureAttributeServiceContext(IORSecurityConfigMetaData metadata) {
        SAS_ContextSec context;

        // context contains target_supports, target_requires, privilige_authorities, supported_naming_mechanisms, supported_identity_types.
        int support = 0;
        int require = 0;
        ServiceConfiguration[] privilAuth = new ServiceConfiguration[0];
        byte[][] supNamMechs = {};
        int supIdenTypes = 0; // 0 means ITTAbsent

        // the the SasContext metadata.
        IORSASContextMetaData sasMeta = metadata.getSasContext();

        // if no SAS context metadata, or caller propagation is not supported, we return with a more or less empty sas context.
        if (sasMeta == null || sasMeta.getCallerPropagation().equals(IORSASContextMetaData.CALLER_PROPAGATION_NONE)) {
            context = new SAS_ContextSec((short) support, (short) require, privilAuth, supNamMechs, supIdenTypes);
        } else {
            support = IdentityAssertion.value;

            // supporting GSSUP (username/password) naming mechanism.
            byte[] upMech = createGSSUPMechOID();
            supNamMechs = new byte[1][upMech.length];
            System.arraycopy(upMech, 0, supNamMechs[0], 0, upMech.length);

            // since we support IdentityAssertion we need to specify supported identity types. CTS says we need them all
            supIdenTypes = ITTAnonymous.value | ITTPrincipalName.value | ITTX509CertChain.value | ITTDistinguishedName.value;
            context = new SAS_ContextSec((short) support, (short) require, privilAuth, supNamMechs, supIdenTypes);
        }

        return context;
    }

    /**
     * <p>
     * Create the client Authentication Service (AS) context included in a {@code CompoundSecMech} definition.
     * </p>
     *
     * @param metadata the metadata object that contains the CSIv2 security configuration info.
     * @return the constructed {@code AS_ContextSec} instance.
     */
    public static AS_ContextSec createAuthenticationServiceContext(IORSecurityConfigMetaData metadata) {

        AS_ContextSec context;

        // the content of the context.
        int support = 0;
        int require = 0;
        byte[] clientAuthMech = {};
        byte[] targetName = {};

        IORASContextMetaData asMeta = metadata.getAsContext();

        // if no AS context metatada exists, or authentication method "none" is specified, we can produce an empty AS context.
        if (asMeta == null || asMeta.getAuthMethod().equals(IORASContextMetaData.AUTH_METHOD_NONE)) {
            context = new AS_ContextSec((short) support, (short) require, clientAuthMech, targetName);
        } else {
            // we do support.
            support = EstablishTrustInClient.value;

            // required depends on the metadata.
            if (asMeta.isRequired()) {
                require = EstablishTrustInClient.value;
            }

            // we only support GSSUP authentication method.
            clientAuthMech = createGSSUPMechOID();

            // finally, encode the "realm" name as a CSI.GSS_NT_ExportedName.
            // clientAuthMech should contain the DER encoded GSSUPMechOID at this point.
            String realm = asMeta.getRealm();
            targetName = createGSSExportedName(clientAuthMech, realm.getBytes());

            context = new AS_ContextSec((short) support, (short) require, clientAuthMech, targetName);
        }

        return context;
    }

    /**
     * <p>
     * Create a transport mechanism {@code TaggedComponent} to be stuffed into a {@code CompoundSecMech}.
     * </p>
     * <p>
     * If no {@code TransportConfig} metadata is specified, or ssl port is negative, or the specified metadata indicates
     * that transport config is not supported, then a {@code TAG_NULL_TAG} (empty) {@code TaggedComponent} will be returned.
     * </p>
     * <p>
     * Otherwise a {@code org.omg.CSIIOP.TLS_SEC_TRANS}, tagged as {@code TAG_TLS_SEC_TRANS} will be returned, indicating support
     * for TLS/SSL as a CSIv2 transport mechanism.
     * </p>
     * <p>
     * Multiple {@code TransportAddress} may be included in the SSL info (host/port pairs), but we only include one.
     * </p>
     *
     * @param tconfig the transport configuration metadata.
     * @param codec   the {@code Codec} used to encode the transport configuration.
     * @param sslPort an {@code int} representing the SSL port.
     * @param orb     a reference to the running {@code ORB}.
     * @return the constructed {@code TaggedComponent}.
     */
    public static TaggedComponent createTransportMech(IORTransportConfigMetaData tconfig, Codec codec, int sslPort, ORB orb) {

        TaggedComponent tc;

        // what we support and require as a target.
        int support = 0;
        int require = 0;

        if (tconfig != null) {
            require = createTargetRequires(tconfig);
            support = createTargetSupports(tconfig);
        }

        if (tconfig == null || support == 0 || sslPort == 0) {
            // no support for transport security.
            tc = new TaggedComponent(TAG_NULL_TAG.value, new byte[0]);
        } else {
            // my ip address.
            String host = CorbaORBService.getORBProperty(Constants.ORB_ADDRESS);

            // this will create only one transport address.
            TransportAddress[] taList = createTransportAddress(host, sslPort);
            TLS_SEC_TRANS tst = new TLS_SEC_TRANS((short) support, (short) require, taList);


            // The tricky part, we must encode TLS_SEC_TRANS into an octet sequence.
            try {
                Any any = orb.create_any();
                TLS_SEC_TRANSHelper.insert(any, tst);
                byte[] b = codec.encode_value(any);
                tc = new TaggedComponent(TAG_TLS_SEC_TRANS.value, b);
            } catch (InvalidTypeForEncoding e) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
            }
        }

        return tc;
    }

    /**
     * <p>
     * Create a {@code TransportAddress[]} with a single {@code TransportAddress}.
     * </p>
     *
     * @param host a {@code String} representing the address host.
     * @param port an {@code int} representing the address port.
     * @return the constructed {@code TransportAddress} array.
     */
    public static TransportAddress[] createTransportAddress(String host, int port) {
        // idl type is unsigned sort, so we need this trick
        short short_port = (port > 32767) ? (short) (port - 65536) : (short) port;


        TransportAddress ta = new TransportAddress(host, short_port);
        TransportAddress[] taList = new TransportAddress[1];
        taList[0] = ta;

        return taList;
    }

    /**
     * <p>
     * Create the bitmask of what the target requires.
     * </p>
     *
     * @param tc the transport configuration metadata.
     * @return an {@code int} representing the transport mechanism required by the target.
     */
    public static int createTargetRequires(IORTransportConfigMetaData tc) {
        int requires = 0;

        if (tc != null) {
            if (tc.getIntegrity().equals(IORTransportConfigMetaData.INTEGRITY_REQUIRED)) {
                requires = requires | Integrity.value;
            }
            if (tc.getConfidentiality().equals(IORTransportConfigMetaData.CONFIDENTIALITY_REQUIRED)) {
                requires = requires | Confidentiality.value;
            }
            if (tc.getDetectMisordering().equalsIgnoreCase(IORTransportConfigMetaData.DETECT_MISORDERING_REQUIRED)) {
                requires = requires | DetectMisordering.value;
            }
            if (tc.getDetectReplay().equalsIgnoreCase(IORTransportConfigMetaData.DETECT_REPLAY_REQUIRED)) {
                requires = requires | DetectReplay.value;
            }
            // no EstablishTrustInTarget required - client decides
            if (tc.getEstablishTrustInClient().equals(IORTransportConfigMetaData.ESTABLISH_TRUST_IN_CLIENT_REQUIRED)) {
                requires = requires | EstablishTrustInClient.value;
            }
        }

        return requires;
    }

    /**
     * <p>
     * Create the bitmask of what the target supports.
     * </p>
     *
     * @param tc the transport configuration metadata.
     * @return an {@code int} representing the transport mechanisms supported by the target.
     */
    public static int createTargetSupports(IORTransportConfigMetaData tc) {
        int supports = 0;

        if (tc != null) {
            if (!tc.getIntegrity().equals(IORTransportConfigMetaData.INTEGRITY_NONE)) {
                supports = supports | Integrity.value;
            }
            if (!tc.getConfidentiality().equals(IORTransportConfigMetaData.CONFIDENTIALITY_NONE)) {
                supports = supports | Confidentiality.value;
            }
            if (!tc.getDetectMisordering().equalsIgnoreCase(IORTransportConfigMetaData.DETECT_MISORDERING_NONE)) {
                supports = supports | DetectMisordering.value;
            }
            if (!tc.getDetectReplay().equalsIgnoreCase(IORTransportConfigMetaData.DETECT_REPLAY_NONE)) {
                supports = supports | DetectReplay.value;
            }
            if (!tc.getEstablishTrustInTarget().equals(IORTransportConfigMetaData.ESTABLISH_TRUST_IN_TARGET_NONE)) {
                supports = supports | EstablishTrustInTarget.value;
            }
            if (!tc.getEstablishTrustInClient().equals(IORTransportConfigMetaData.ESTABLISH_TRUST_IN_CLIENT_NONE)) {
                supports = supports | EstablishTrustInClient.value;
            }
        }

        return supports;
    }

    /**
     * <p>
     * Create an ASN.1, DER encoded representation for the GSSUP OID mechanism.
     * </p>
     *
     * @return the DER encoded representation of the GSSUP OID.
     */
    public static byte[] createGSSUPMechOID() {
        // kudos to org.ietf.jgss.Oid for the Oid utility need to strip the "oid:" part of the GSSUPMechOID first.

        byte[] retval = {};
        try {
            Oid oid = new Oid(GSSUPMechOID.value.substring(4));
            retval = oid.getDER();
        } catch (GSSException e) {
            IIOPLogger.ROOT_LOGGER.caughtExceptionEncodingGSSUPMechOID(e);
        }
        return retval;
    }

    /**
     * <p/>
     * Generate an exported name as specified in [RFC 2743], section 3.2 copied below:
     * <p/>
     * 3.2: Mechanism-Independent Exported Name Object Format
     * <p/>
     * This section specifies a mechanism-independent level of encapsulating representation for names exported via the
     * GSS_Export_name() call, including an object identifier representing the exporting mechanism. The format of names
     * encapsulated via this representation shall be defined within individual mechanism drafts.  The Object Identifier
     * value to indicate names of this type is defined in Section 4.7 of this document.
     * <p/>
     * No name type OID is included in this mechanism-independent level of format definition, since (depending on
     * individual mechanism specifications) the enclosed name may be implicitly typed or may be explicitly typed using
     * a means other than OID encoding.
     * <p/>
     * The bytes within MECH_OID_LEN and NAME_LEN elements are represented most significant byte first (equivalently,
     * in IP network byte order).
     * <p/>
     * Length          Name            Description
     * <p/>
     * 2               TOK_ID          Token Identifier
     * For exported name objects, this must be hex 04 01.
     * 2               MECH_OID_LEN    Length of the Mechanism OID
     * MECH_OID_LEN    MECH_OID        Mechanism OID, in DER
     * 4               NAME_LEN        Length of name
     * NAME_LEN        NAME            Exported name; format defined in applicable mechanism draft.
     * <p/>
     * A concrete example of the contents of an exported name object, derived from the Kerberos Version 5 mechanism, is
     * as follows:
     * <p/>
     * 04 01 00 0B 06 09 2A 86 48 86 F7 12 01 02 02 hx xx xx xl pp qq ... zz
     * <p/>
     * ...
     *
     * @param oid  the DER encoded OID.
     * @param name the name to be converted to {@code GSSExportedName}.
     * @return a {@code byte[]} representing the exported name.
     */
    public static byte[] createGSSExportedName(byte[] oid, byte[] name) {
        int olen = oid.length;
        int nlen = name.length;

        // size according to spec.
        int size = 2 + 2 + olen + 4 + nlen;

        // allocate space for the exported name.
        byte[] buf = new byte[size];
        // index.
        int i = 0;

        // standard header.
        buf[i++] = 0x04;
        buf[i++] = 0x01;

        // encode oid length.
        buf[i++] = (byte) (olen & 0xFF00);
        buf[i++] = (byte) (olen & 0x00FF);

        // copy the oid in the exported name buffer.
        System.arraycopy(oid, 0, buf, i, olen);
        i += olen;

        // encode the name length in the exported buffer.
        buf[i++] = (byte) (nlen & 0xFF000000);
        buf[i++] = (byte) (nlen & 0x00FF0000);
        buf[i++] = (byte) (nlen & 0x0000FF00);
        buf[i++] = (byte) (nlen & 0x000000FF);

        // finally, copy the name bytes.
        System.arraycopy(name, 0, buf, i, nlen);

        return buf;
    }

    /**
     * <p>
     * ASN.1-encode an {@code InitialContextToken} as defined in RFC 2743, Section 3.1, "Mechanism-Independent Token
     * Format", pp. 81-82. The encoded token contains the ASN.1 tag 0x60, followed by a token length (which is itself
     * stored in a variable-length format and takes 1 to 5 bytes), the GSSUP mechanism identifier, and a mechanism-specific
     * token, which in this case is a CDR encapsulation of the GSSUP {@code InitialContextToken} in the {@code authToken}
     * parameter.
     * </p>
     *
     * @param authToken the {@code InitialContextToken} to be encoded.
     * @param codec     the {@code Codec} used to encode the token.
     * @return a {@code byte[]} representing the encoded token.
     */
    public static byte[] encodeInitialContextToken(InitialContextToken authToken, Codec codec) {
        byte[] out;
        Any any = ORB.init().create_any();
        InitialContextTokenHelper.insert(any, authToken);
        try {
            out = codec.encode_value(any);
        } catch (Exception e) {
            return new byte[0];
        }


        int length = out.length + gssUpMechOidArray.length;
        int n;


        if (length < (1 << 7)) {
            n = 0;
        } else if (length < (1 << 8)) {
            n = 1;
        } else if (length < (1 << 16)) {
            n = 2;
        } else if (length < (1 << 24)) {
            n = 3;
        } else {// if (length < (1 << 32))
            n = 4;
        }

        byte[] encodedToken = new byte[2 + n + length];
        encodedToken[0] = 0x60;

        if (n == 0) {
            encodedToken[1] = (byte) length;
        } else {
            encodedToken[1] = (byte) (n | 0x80);
            switch (n) {
                case 1:
                    encodedToken[2] = (byte) length;
                    break;
                case 2:
                    encodedToken[2] = (byte) (length >> 8);
                    encodedToken[3] = (byte) length;
                    break;
                case 3:
                    encodedToken[2] = (byte) (length >> 16);
                    encodedToken[3] = (byte) (length >> 8);
                    encodedToken[4] = (byte) length;
                    break;
                default: // case 4:
                    encodedToken[2] = (byte) (length >> 24);
                    encodedToken[3] = (byte) (length >> 16);
                    encodedToken[4] = (byte) (length >> 8);
                    encodedToken[5] = (byte) length;
            }
        }
        System.arraycopy(gssUpMechOidArray, 0, encodedToken, 2 + n, gssUpMechOidArray.length);
        System.arraycopy(out, 0, encodedToken, 2 + n + gssUpMechOidArray.length, out.length);

        return encodedToken;
    }

    /**
     * <p>
     * Decodes an ASN.1-encoded {@code InitialContextToken}. See {@code encodeInitialContextToken} for a description of
     * the encoded token format.
     * </p>
     *
     * @param encodedToken the encoded token.
     * @param codec        the {@code Codec} used to decode the token.
     * @return the decoded {@code InitialContextToken} instance.
     * @see #encodeInitialContextToken(org.omg.GSSUP.InitialContextToken, org.omg.IOP.Codec)
     */
    public static InitialContextToken decodeInitialContextToken(byte[] encodedToken, Codec codec) {
        if (encodedToken[0] != 0x60)
            return null;

        int encodedLength = 0;
        int n = 0;

        if (encodedToken[1] >= 0)
            encodedLength = encodedToken[1];
        else {
            n = encodedToken[1] & 0x7F;
            for (int i = 1; i <= n; i++) {
                encodedLength += (encodedToken[1 + i] & 0xFF) << (n - i) * 8;
            }
        }

        int length = encodedLength - gssUpMechOidArray.length;
        byte[] encodedInitialContextToken = new byte[length];

        System.arraycopy(encodedToken, 2 + n + gssUpMechOidArray.length,
                encodedInitialContextToken, 0,
                length);
        Any any;
        try {
            any = codec.decode_value(encodedInitialContextToken, InitialContextTokenHelper.type());
        } catch (Exception e) {
            return null;
        }

        return InitialContextTokenHelper.extract(any);
    }

    /**
     * <p>
     * ASN.1-encodes a GSS exported name with the GSSUP mechanism OID. See {@code createGSSExportedName} for a
     * description of the encoding format.
     * </p>
     *
     * @param name the exported name to be encoded.
     * @return a {@code byte[]} representing the encoded exported name.
     * @see #createGSSExportedName(byte[], byte[])
     */
    public static byte[] encodeGssExportedName(byte[] name) {
        return createGSSExportedName(gssUpMechOidArray, name);
    }

    /**
     * <p>
     * Decodes a GSS exported name that has been encoded with the GSSUP mechanism OID. See {@code createGSSExportedName}
     * for a description of the encoding format.
     * </p>
     *
     * @param encodedName the encoded exported name.
     * @return a {@code byte[]} representing the decoded exported name.
     * @see #createGSSExportedName(byte[], byte[])
     */
    public static byte[] decodeGssExportedName(byte[] encodedName) {
        if (encodedName[0] != 0x04 || encodedName[1] != 0x01)
            return null;

        int mechOidLength = (encodedName[2] & 0xFF) << 8; //MECH_OID_LEN
        mechOidLength += (encodedName[3] & 0xFF);      // MECH_OID_LEN

        byte[] oidArray = new byte[mechOidLength];
        System.arraycopy(encodedName, 4, oidArray, 0, mechOidLength);

        for (int i = 0; i < mechOidLength; i++) {
            if (gssUpMechOidArray[i] != oidArray[i]) {
                return null;
            }
        }

        int offset = 4 + mechOidLength;
        int nameLength = (encodedName[offset] & 0xFF) << 24;
        nameLength += (encodedName[++offset] & 0xFF) << 16;
        nameLength += (encodedName[++offset] & 0xFF) << 8;
        nameLength += (encodedName[++offset] & 0xFF);

        byte[] name = new byte[nameLength];
        System.arraycopy(encodedName, ++offset, name, 0, nameLength);

        return name;
    }

    /**
     * <p>
     * Helper method to be called from a client request interceptor. The {@code ri} parameter refers to the current
     * request. This method returns the first {@code CompoundSecMech} found in the target IOR such that
     * <ul>
     * <li>all {@code CompoundSecMech} requirements are satisfied by the options in the {@code clientSupports}
     * parameter, and</li>
     * <li>every requirement in the {@code clientRequires} parameter is satisfied by the {@code CompoundSecMech}.
     * </li>
     * </ul>
     * The method returns null if the target IOR contains no {@code CompoundSecMech}s or if no matching
     * {@code CompoundSecMech} is found.
     * </p>
     * <p>
     * Since this method is intended to be called from a client request interceptor, it converts unexpected exceptions
     * into {@code MARSHAL} exceptions.
     * </p>
     *
     * @param ri             a reference to the current {@code ClientRequestInfo}.
     * @param codec          the {@code Codec} used to decode the CSIv2 components.
     * @param clientSupports the client supported transport options that must be satisfied by the {@code CompoundSecMech}.
     * @param clientRequires the client required transport options that must be satisfied by the {@code CompoundSecMech}.
     * @return the {@code CompoundSecMech} instance that satisfies all client options, or {@code null} if no such object
     *         can be found.
     */
    public static CompoundSecMech getMatchingSecurityMech(ClientRequestInfo ri, Codec codec, short clientSupports,
                                                          short clientRequires) {
        CompoundSecMechList csmList;
        try {
            TaggedComponent tc = ri.get_effective_component(org.omg.IOP.TAG_CSI_SEC_MECH_LIST.value);

            Any any = codec.decode_value(tc.component_data, CompoundSecMechListHelper.type());
            csmList = CompoundSecMechListHelper.extract(any);

            // look for the first matching security mech.
            for (int i = 0; i < csmList.mechanism_list.length; i++) {
                CompoundSecMech securityMech = csmList.mechanism_list[i];
                AS_ContextSec authConfig = securityMech.as_context_mech;

                if ((EstablishTrustInTarget.value & (clientRequires ^ authConfig.target_supports)
                        & ~authConfig.target_supports) != 0) {
                    // client requires EstablishTrustInTarget, but target does not support it: skip this securityMech.
                    continue;
                }

                if ((EstablishTrustInClient.value & (authConfig.target_requires ^ clientSupports)
                        & ~clientSupports) != 0) {
                    // target requires EstablishTrustInClient, but client does not support it: skip this securityMech.
                    continue;
                }

                SAS_ContextSec identityConfig = securityMech.sas_context_mech;

                if ((IdentityAssertion.value & (identityConfig.target_requires ^ clientSupports)
                        & ~clientSupports) != 0) {
                    // target requires IdentityAssertion, but client does not support it: skip this securityMech
                    continue;
                }

                // found matching securityMech.
                return securityMech;
            }
            // no matching securityMech was found.
            return null;
        } catch (BAD_PARAM e) {
            // no component with TAG_CSI_SEC_MECH_LIST was found.
            return null;
        } catch (org.omg.IOP.CodecPackage.TypeMismatch e) {
            // unexpected exception in codec
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        } catch (org.omg.IOP.CodecPackage.FormatMismatch e) {
            // unexpected exception in codec
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
    }

    /**
     * <p>
     * Generate a string representation of the {@code CompoundSecMech}.
     * </p>
     *
     * @param securityMech the {@code CompoundSecMech} to create the string for.
     * @param builder      the buffer to write to.
     */
    public static void toString(CompoundSecMech securityMech, StringBuilder builder) {
        AS_ContextSec asMech = securityMech != null ? securityMech.as_context_mech : null;
        SAS_ContextSec sasMech = securityMech != null ? securityMech.sas_context_mech : null;
        if (securityMech != null) {
            builder.append("CompoundSecMech[");
            builder.append("target_requires: ");
            builder.append(securityMech.target_requires);
            if (asMech != null) {
                builder.append("AS_ContextSec[");

                builder.append("client_authentication_mech: ");
                builder.append(new String(asMech.client_authentication_mech, StandardCharsets.UTF_8));
                builder.append(", target_name: ");
                builder.append(new String(asMech.target_name, StandardCharsets.UTF_8));
                builder.append(", target_requires: ");
                builder.append(asMech.target_requires);
                builder.append(", target_supports: ");
                builder.append(asMech.target_supports);
                builder.append("]");
            }
            if (sasMech != null) {
                builder.append("SAS_ContextSec[");
                builder.append("supported_identity_types: ");
                builder.append(sasMech.supported_identity_types);
                builder.append(", target_requires: ");
                builder.append(sasMech.target_requires);
                builder.append(", target_supports: ");
                builder.append(sasMech.target_supports);
                builder.append("]");
            }
            builder.append("]");
        }
    }
}
