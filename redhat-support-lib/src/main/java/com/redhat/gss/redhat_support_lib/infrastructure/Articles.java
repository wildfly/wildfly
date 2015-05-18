package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.ArticleType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Articles extends BaseQuery {
    private static final Logger LOGGER = Logger.getLogger(API.class.getName());
    private ConnectionManager connectionManager = null;
    static String url = "/rs/articles/";

    public Articles(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given ArticleType ID. RESTful method:
     * https://api.access.redhat.com/rs/ArticleTypes/<ArticleTypeID>
     *
     * @param artID
     *            The exact ArticleTypeID you are interested in.
     * @return A ArticleType object that represents the given ArticleType ID.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public ArticleType get(String artID) throws RequestException,
            MalformedURLException {
        String fullUrl = connectionManager.getConfig().getUrl() + url + artID;
        return get(connectionManager.getConnection(), fullUrl,
                ArticleType.class);
    }

    /**
     * Queries the ArticleTypes RESTful interface with a given set of keywords.
     * RESTful method: https://api.access.redhat.com/rs/ArticleTypes?keyword=NFS
     *
     * @param keywords
     *            A string array of keywords to search on.
     * @param kwargs
     *            Additional properties to filter on. The RESTful interface can
     *            only search on keywords; however, you can use this method to
     *            post-filter the results returned. Simply supply a string array
     *            of valid properties and their associated values.
     * @return A list of ArticleType objects
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */

    @SuppressWarnings("unchecked")
    public List<ArticleType> list(String[] keywords, String[] kwargs)
            throws RequestException, MalformedURLException {
        List<String> queryParams = new ArrayList<String>();
        for (String arg : keywords) {
            queryParams.add("keyword=" + arg);
        }
        String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
                .getUrl() + url, queryParams);
        com.redhat.gss.redhat_support_lib.parsers.ArticlesType articlesType = get(
                connectionManager.getConnection(), fullUrl,
                com.redhat.gss.redhat_support_lib.parsers.ArticlesType.class);
        return (List<ArticleType>) FilterHelper.filterResults(
                articlesType.getArticle(), kwargs);
    }

    /**
     * @param art
     *            The ArticleType to be added
     * @return The same solution with the ID and view_uri set if successful.
     * @throws Exception
     *             An exception if there was a connection related issue
     */
    public ArticleType add(ArticleType art) throws Exception {
        String fullUrl = connectionManager.getConfig().getUrl() + url;
        Response resp = add(connectionManager.getConnection(), fullUrl, art);
        MultivaluedMap<String, String> headers = resp.getStringHeaders();
        URL url = null;
        try {
            url = new URL(headers.getFirst("view-uri"));
        } catch (MalformedURLException e) {
            LOGGER.debug("Failed : Adding ArticleType " + art.getTitle()
                    + " was unsuccessful.");
            throw new Exception();
        }
        String path = url.getPath();
        art.setId(path.substring(path.lastIndexOf('/') + 1, path.length()));
        art.setViewUri(url.toString());
        return art;
    }
}