package com.engine.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

public class PostRequestHelper {

    //Log instance
     private static final Logger LOG = Logger.getLogger(PostRequestHelper.class.getName());
     
    /**
     * Execute a post against the given URL with the given properties and returns the output
     * of the post method
     * @param url       Address to hit
     * @param params    Parameters to pass in bind
     * @return          String message returned
     * @throws IOException 
     */
    public static String post(String url, NameValuePair[] params) throws IOException {
        //Initialize variables
        String response         = null;
        HttpClient httpclient   = new HttpClient();
        GetMethod httpget       = new GetMethod(url);
        //Execute post and retrieve results
        try {
            httpget.setQueryString(params); 
            httpclient.executeMethod(httpget);
            response = httpget.getResponseBodyAsString();
        } catch (Exception ex){
            LOG.log(Level.SEVERE,"Exception - PostRequestHelper.post against .url:"+url,ex);
        } finally {
            //Close remaining connections and objects
            if (httpget != null){
                httpget.releaseConnection();
            }
        }
        return response;
    }
}