package com.redhat.gss.redhat_support_lib.infrastructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import javax.mail.internet.MimeUtility;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;

public class BaseQuery {

    private static final Logger LOGGER = Logger.getLogger(API.class.getName());

    protected <T> T get(ResteasyClient client, String uri, Class<T> c)
            throws RequestException {
        Response response = client.target(uri).request()
                .accept(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != 200) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        T returnObject = response.readEntity(c);
        return returnObject;
    }

    protected String getFile(ResteasyClient client, String uri,
            String fileName, String destDir) throws RequestException,
            IOException, javax.mail.internet.ParseException {
        Response response = client.target(uri).request()
                .accept(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != 200) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }

        // copy file using buffer to temporary file
        response.bufferEntity();
        File file = response.readEntity(File.class);

        // if destDir supplied then move temporary file to specified directory
        StringBuffer filePath = new StringBuffer();
        if (destDir != null) {
            String nameOfFile;
            filePath.append(destDir);
            if (!destDir.endsWith(File.separator)) {
                filePath.append(File.separator);
            }

            if (fileName != null) {
                nameOfFile = fileName;
            } else {
                String name = response.getStringHeaders().getFirst(
                        "Content-Disposition");
                String[] temp = name.split("\"");
                nameOfFile = MimeUtility.decodeWord(temp[1]);
            }
            filePath.append(nameOfFile);
            File movedFile = new File(filePath.toString());
            FileUtils.moveFile(file, movedFile);
            file.delete();
            file = movedFile;
        }

        return file.getAbsolutePath();
    }

    protected Response add(ResteasyClient client, String uri, Object object)
            throws RequestException {
        Response response = (Response) client.target(uri).request()
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.entity(object, MediaType.APPLICATION_XML));
        if (response.getStatus() > 399) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;

    }

    protected <T> T add(ResteasyClient client, String uri, Object object,
            Class<T> c) throws RequestException {
        Response response = (Response) client.target(uri)
                .request(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.entity(object, MediaType.APPLICATION_XML));
        if (response.getStatus() > 399) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }

        T returnObject = response.readEntity(c);
        return returnObject;
    }

    protected Response update(ResteasyClient client, String uri, Object object)
            throws RequestException {
        Response response = (Response) client.target(uri).request()
                .accept(MediaType.APPLICATION_XML)
                .put(Entity.entity(object, MediaType.APPLICATION_XML));
        if (response.getStatus() > 399) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;

    }

    protected boolean delete(ResteasyClient client, String uri)
            throws RequestException {
        Response response = (Response) client.target(uri).request()
                .delete(Response.class);
        if (response.getStatus() > 399) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return true;
    }

    protected Response upload(ResteasyClient client, String uri, File file,
            String description) throws FileNotFoundException, ParseException,
            RequestException {
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        if (description != null) {
            mdo.addFormData("description", description,
                    MediaType.APPLICATION_XML_TYPE);
        }
        mdo.addFormData("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                file.getName());
        // mdo.addPart(new FileInputStream(new
        // File("/Users/Spense/Desktop/scsiErrorMessages/test.txt")),
        // MediaType.APPLICATION_OCTET_STREAM_TYPE, "file");
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(
                mdo) {
        };

        // Response r = target.request().post( Entity.entity(entity,
        // MediaType.MULTIPART_FORM_DATA_TYPE));

        byte[] encodedString = null;
        // if (password != null && !password.isEmpty()) {
        String tmp = "sshumake" + ":" + "redhat1";
        encodedString = Base64.encodeBase64(tmp.getBytes());
        // } else {
        // encodedString = Base64.encodeBase64(username.getBytes());
        // }
        // requestContext.getHeaders().add("Proxy-Connection", "Keep-Alive");
        // requestContext.getHeaders().add("Proxy-authorization", "Basic " +
        // encodedString);

        javax.ws.rs.client.Invocation.Builder builder = client.target(uri)
                .request(MediaType.APPLICATION_XML)
                .header("Authorization", "Basic " + encodedString.toString());
        if (description != null) {
            builder.header("description", description);
            // builder.header("Content-Type",
            // "multipart/form-data; boundary=--64f32fd8-b239-4a1e-a580-6ed5976f8a82");
        }
        Response response = builder.post(Entity.entity(entity,
                MediaType.MULTIPART_FORM_DATA_TYPE));

        // FormDataMultiPart part = new FormDataMultiPart();
        // if (description != null) {
        // part.bodyPart(new FormDataBodyPart("description", description));
        // }
        // part.bodyPart(new FileDataBodyPart("file", file,
        // MediaType.APPLICATION_OCTET_STREAM_TYPE));
        // Response response = (Response)
        // target.request(MediaType.APPLICATION_XML)
        // .type(Boundary.addBoundary(MediaType.MULTIPART_FORM_DATA_TYPE))
        // .header("description", description)
        // .post(Response.class, part);
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            LOGGER.debug("Failed : HTTP error code : "
                    + response.getStatusInfo().getStatusCode() + " - "
                    + response.getStatusInfo().getReasonPhrase());
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }
}
