package SearchUs.server.engine;


import SearchUs.shared.data.FileType;
import SearchUs.shared.data.SearchDetails;
import com.google.gwt.thirdparty.json.JSONException;
import com.google.gwt.thirdparty.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by Paulo on 20/05/2015.
 */
public class ElasticManager {

    private String serviceUrl;

    public ElasticManager(String url) {

        this.serviceUrl = url+":9200/files/doc/"; //todo: verifier url
    }


    private String makePost(String endpoint, String data )
    {
        String responseBody = null;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {

            HttpPost httpPost = new HttpPost(this.serviceUrl+"_search");
            httpPost.setEntity(new StringEntity(data));

            System.out.println("Executing request " + httpPost.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse( final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            responseBody = httpclient.execute(httpPost, responseHandler);

        }
        catch (IOException ex)
        {

        }
        finally {
            try { httpclient.close(); } catch (Exception ignore) {}
        }
        return responseBody;
    }

    private String makeGet(String endpoint)
    {
        String responseBody =  null;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try
        {
            HttpGet httpget = new HttpGet(this.serviceUrl+endpoint);

            System.out.println("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
             responseBody = httpclient.execute(httpget, responseHandler);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return responseBody;
    }



    public JSONObject search(SearchDetails searchInfo) {

        String queryString = searchInfo.getSearchString();
        String query;
        JSONObject jsonQuery = new JSONObject();
        JSONObject match = new JSONObject();
        JSONObject completeQuery = new JSONObject();

        ArrayList<FileType> searchInDocTypes = searchInfo.getSearchFor();

        if(queryString.contains("*"))
        {

            try {
                jsonQuery.put("wildcard",new JSONObject().put("_all",queryString));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        else
        {
            try {
                match.put("_all",queryString);
                jsonQuery.put("match",match);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(searchInDocTypes != null)
        {
            if(!searchInDocTypes.contains(FileType.ALL)) {
                JSONObject filtered = new JSONObject();
                JSONObject or = new JSONObject();
                JSONObject filter = new JSONObject();

                try {

                    filtered.put("query", jsonQuery);


                    filter.put("or", or);
                    filtered.put("filter", filter);
                    completeQuery.put("filtered", filtered);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                try {
                    completeQuery.put("query",jsonQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
        else
        {
            try {
                completeQuery.put("query",jsonQuery);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        query = completeQuery.toString();

        String searchResult = makePost("_search",query);
        System.out.println("Search result: "+searchResult);
        JSONObject jsonObj = null;
        System.out.println("query: "+query);

        if(searchResult != null)
        {
            try
            {
                jsonObj = new JSONObject(searchResult);
            }
            catch(com.google.gwt.thirdparty.json.JSONException ex)
            {
                System.out.println("Json decoding exception: "+ex.toString());
            }
        }


        return jsonObj;

    }
}