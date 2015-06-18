package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.CommentType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Comments extends BaseQuery {
    private static final Logger LOGGER = Logger.getLogger(Comments.class
            .getName());
    private ConnectionManager connectionManager = null;

    public Comments(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given solution ID. RESTful method:
     * https://api.access.redhat.com/rs/cases/<caseNumber>/comments/<commentID>
     *
     * @param caseNumber
     *            The exact caseNumber you are interested in.
     * @param commentID
     *            The exact comment ID you're interested in
     *            (e.g.a0aA00000079GpQIAU)
     * @return A comment object that represents the given comment ID.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public CommentType get(String caseNumber, String commentID)
            throws RequestException, MalformedURLException {

        String url = "/rs/cases/{caseNumber}/comments/{commentID}";
        url = url.replace("{caseNumber}", caseNumber);
        url = url.replace("{commentID}", commentID);
        String fullUrl = connectionManager.getConfig().getUrl() + url;
        return get(connectionManager.getConnection(), fullUrl,
                CommentType.class);
    }

    /**
     * Gets all of the comments for a given case number. You can then
     * search/filter the returned comments using any of the properties of a
     * 'comment' RESTful method:
     * https://api.access.redhat.com/rs/cases/<caseNumber>/comments.
     *
     * @param caseNumber
     *            A case number (e.g.00595293)
     * @param startDate
     *            Must be either: yyyy-MM-ddTHH:mm:ss or yyyy-MM-dd
     * @param endDate
     *            Must be either: yyyy-MM-ddTHH:mm:ss or yyyy-MM-dd
     * @param kwargs
     *            Additional properties to filter on. The RESTful interface can
     *            only search on keywords; however, you can use this method to
     *            post-filter the results returned. Simply supply a String array
     *            of valid properties and their associated values.
     * @return A list of comment objects
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public List<CommentType> list(String caseNumber, String startDate,
            String endDate, String[] kwargs) throws RequestException,
            MalformedURLException {

        String url = "/rs/cases/{caseNumber}/comments";
        url = url.replace("{caseNumber}", caseNumber);
        List<String> queryParams = new ArrayList<String>();
        if (startDate != null) {
            queryParams.add("startDate=" + startDate);
        }
        if (endDate != null) {
            queryParams.add("endDate=" + endDate);
        }
        String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
                .getUrl() + url, queryParams);
        com.redhat.gss.redhat_support_lib.parsers.CommentsType comments = get(
                connectionManager.getConnection(), fullUrl,
                com.redhat.gss.redhat_support_lib.parsers.CommentsType.class);
        return comments.getComment();
    }

    /**
     * Add a new comment
     *
     * @param comment
     *            The comment to be added.
     * @return The same comment with the ID and view_uri set if successful.
     * @throws Exception
     *             An exception if there was a connection, file open, etc.
     *             issue.
     */
    public CommentType add(CommentType comment) throws Exception {

        String url = "/rs/cases/{caseNumber}/comments";
        url = url.replace("{caseNumber}", comment.getCaseNumber());

        String fullUrl = connectionManager.getConfig().getUrl() + url;
        Response resp = add(connectionManager.getConnection(), fullUrl, comment);
        MultivaluedMap<String, String> headers = resp.getStringHeaders();
        URL caseurl = null;
        try {
            caseurl = new URL(headers.getFirst("Location"));
        } catch (MalformedURLException e) {
            LOGGER.debug("Failed : Adding comment " + comment.getText()
                    + " was unsuccessful.");
            throw new Exception();
        }
        String path = caseurl.getPath();
        comment.setId(path.substring(path.lastIndexOf('/') + 1, path.length()));
        comment.setViewUri(caseurl.toString());
        return comment;
    }
}
