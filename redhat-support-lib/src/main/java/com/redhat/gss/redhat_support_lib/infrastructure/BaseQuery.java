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
import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import com.redhat.gss.redhat_support_lib.errors.RequestException;

public class BaseQuery {
    protected <T> T get(ResteasyClient client, String uri, Class<T> c)
            throws RequestException {
        Response response = client.target(uri).request()
                .accept(MediaType.APPLICATION_XML).get();

        if (response.getStatus() != HttpStatus.SC_OK) {
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

        if (response.getStatus() != HttpStatus.SC_OK) {
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
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
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
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
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
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;

    }

    protected boolean delete(ResteasyClient client, String uri)
            throws RequestException {
        Response response = (Response) client.target(uri).request()
                .delete(Response.class);
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
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
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(
                mdo) {
        };

        javax.ws.rs.client.Invocation.Builder builder = client.target(uri)
                .request(MediaType.APPLICATION_XML);
        if (description != null) {
            builder.header("description", description);
        }
        Response response = builder.post(Entity.entity(entity,
                MediaType.MULTIPART_FORM_DATA_TYPE));

        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }
}
