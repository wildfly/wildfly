/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.certs.ocsp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;
import org.wildfly.common.iteration.ByteIterator;
import org.xipki.datasource.DataSourceFactory;
import org.xipki.datasource.DataSourceWrapper;
import org.xipki.http.servlet.ServletURI;
import org.xipki.http.servlet.SslReverseProxyMode;
import org.xipki.ocsp.server.impl.HttpOcspServlet;
import org.xipki.ocsp.server.impl.OcspServer;
import org.xipki.security.SecurityFactoryImpl;
import org.xipki.security.SignerFactoryRegisterImpl;

/**
 * Trivial XiPKI based OCSP server for OCSP support testing.
 */
public class TestingOcspServer {

    private int port;
    private OcspServer ocspServer = null;
    private ClientAndServer server;
    private Connection connection;
    private SecurityFactoryImpl securityFactory = new SecurityFactoryImpl();

    public TestingOcspServer(int port) throws Exception {
        this.port = port;
        initDatabase();
    }

    private void initDatabase() throws Exception {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        DataSourceWrapper dataSourceWrapper = dataSourceFactory.createDataSource("datasource1", TestingOcspServer.class.getResource("ocsp-db.properties").openStream(), securityFactory.getPasswordResolver());
        connection = dataSourceWrapper.getConnection();

        // structure described in:
        // https://github.com/xipki/xipki/blob/v3.0.0/ca-server/src/main/resources/sql/ocsp-init.xml

        connection.prepareStatement("CREATE TABLE ISSUER (\n"
                + "    ID INT NOT NULL,\n"
                + "    SUBJECT VARCHAR(350) NOT NULL,\n"
                + "    NBEFORE BIGINT NOT NULL,\n" // notBefore
                + "    NAFTER BIGINT NOT NULL,\n" // notAfter
                + "    S1C CHAR(28) NOT NULL,\n" // base64 encoded SHA1 sum of the certificate
                + "    REV SMALLINT DEFAULT 0,\n" // whether the certificate is revoked
                + "    RR SMALLINT,\n" // revocation reason
                + "    RT BIGINT,\n" // revocation time
                + "    RIT BIGINT,\n" // revocation invalidity time
                + "    CERT VARCHAR(4000) NOT NULL,\n"
                + "    CRL_INFO VARCHAR(1000)\n" // CRL information if this issuer is imported from a CRL
                + ");").execute();

        connection.prepareStatement("CREATE TABLE CERT (\n"
                + "    ID BIGINT NOT NULL,\n"
                + "    IID INT NOT NULL,\n" // issuer id (reference into ISSUER table)
                + "    SN VARCHAR(40) NOT NULL,\n" // serial number
                + "    LUPDATE BIGINT NOT NULL,\n" // last update
                + "    NBEFORE BIGINT,\n" // notBefore
                + "    NAFTER BIGINT,\n" // notAfter
                + "    REV SMALLINT DEFAULT 0,\n" // whether the certificate is revoked
                + "    RR SMALLINT,\n" // revocation reason
                + "    RT BIGINT,\n" // revocation time
                + "    RIT BIGINT,\n" // revocation invalidity time
                + "    PN VARCHAR(45)\n" // certificate profile name
                + ");").execute();
    }

    public void start() throws Exception {
        Assert.assertNull("OCSP server already started", ocspServer);

        ocspServer = new OcspServer();
        ocspServer.setConfFile(TestingOcspServer.class.getResource("ocsp-responder.xml").getFile());

        securityFactory.setSignerFactoryRegister(new SignerFactoryRegisterImpl());
        ocspServer.setSecurityFactory(securityFactory);

        ocspServer.init();
        HttpOcspServlet servlet = new HttpOcspServlet();
        servlet.setServer(ocspServer);

        server = new ClientAndServer(port);
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/ocsp"),
                Times.unlimited())
                .respond(request -> getHttpResponse(request, servlet));
        server.when(
                request()
                        .withMethod("GET")
                        .withPath("/ocsp/.*"),
                Times.unlimited())
                .respond(request -> getHttpResponse(request, servlet));
    }

    public HttpResponse getHttpResponse(HttpRequest request, HttpOcspServlet servlet){
        byte[] body;
        HttpMethod method;
        if (request.getBody() == null) {
            method = HttpMethod.GET;
            body = request.getPath().getValue().split("/ocsp/", 2)[1].getBytes(UTF_8);
        } else {
            method = HttpMethod.POST;
            body = request.getBody().getRawBytes();
        }
        ByteBuf buffer = Unpooled.wrappedBuffer(body);
        FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, method, request.getPath().getValue(), buffer);
        for (Header header : request.getHeaderList()) {
            for (NottableString value : header.getValues()) {
                nettyRequest.headers().add(header.getName().getValue(), value.getValue());
            }
        }

        FullHttpResponse nettyResponse;
        try {
            nettyResponse = servlet.service(nettyRequest, new ServletURI(request.getPath().getValue()), null, SslReverseProxyMode.NONE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpResponse response = response()
                .withStatusCode(nettyResponse.status().code())
                .withBody(nettyResponse.content().array());

        for (Map.Entry<String, String> header : nettyResponse.headers()) {
            response.withHeader(header.getKey(), header.getValue());
        }
        return response;
    }

    public void stop() throws SQLException {
        Assert.assertNotNull("OCSP server not started", ocspServer);

        if (server !=null ) {
            server.stop();
        }

        ocspServer.shutdown();

        if (connection != null) {
            connection.close();
        }

        ocspServer = null;
    }

    public void createIssuer(int id, X509Certificate issuer) throws SQLException, CertificateException, NoSuchAlgorithmException {
        Assert.assertNull("OCSP server already started", ocspServer);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        PreparedStatement statement = connection.prepareStatement("INSERT INTO ISSUER (ID, SUBJECT, NBEFORE, NAFTER, S1C, CERT) VALUES (?, ?, ?, ?, ?, ?)");
        statement.setInt(1, id);
        statement.setString(2, issuer.getSubjectDN().toString());
        statement.setLong(3, issuer.getNotBefore().toInstant().getEpochSecond());
        statement.setLong(4, issuer.getNotAfter().toInstant().getEpochSecond());
        statement.setString(5, ByteIterator.ofBytes(digest.digest(issuer.getEncoded())).base64Encode().drainToString());
        statement.setString(6, ByteIterator.ofBytes(issuer.getEncoded()).base64Encode().drainToString());
        statement.execute();
    }

    public void createCertificate(int id, int issuerId, X509Certificate certificate) throws SQLException {
        long time = Instant.now().getEpochSecond();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO CERT (ID, IID, SN, LUPDATE, NBEFORE, NAFTER) VALUES (?, ?, ?, ?, ?, ?)");
        statement.setInt(1, id);
        statement.setInt(2, issuerId);
        statement.setString(3, certificate.getSerialNumber().toString(16));
        statement.setLong(4, time);
        statement.setLong(5, certificate.getNotBefore().toInstant().getEpochSecond());
        statement.setLong(6, certificate.getNotAfter().toInstant().getEpochSecond());
        statement.execute();
    }

    public void revokeCertificate(int id, int reason) throws SQLException {
        long time = Instant.now().getEpochSecond();
        PreparedStatement statement = connection.prepareStatement("UPDATE CERT SET REV = 1, RR = ?, RT = ?, RIT = ? WHERE ID = ?");
        statement.setInt(1, reason);
        statement.setLong(2, time);
        statement.setLong(3, time);
        statement.setInt(4, id);
        statement.execute();
    }

}
