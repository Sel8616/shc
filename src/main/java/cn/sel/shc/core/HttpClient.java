/*
 * Copyright 2015-2016 Erlu Shang (sel8616@gmail.com/philshang@163.com)
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
package cn.sel.shc.core;

import cn.sel.shc.constant.RequestError;
import cn.sel.shc.constant.RequestMethod;
import cn.sel.shc.constant.StandardEncoding;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton/Less configuration->To be Simple!
 *
 * @see ResponseHandler
 */
public final class HttpClient
{
    //region Constants--------------------------------------------------------------------------------------------------
    /**
     * HTTP protocol name
     */
    private static final String PROTOCOL_HTTP = "HTTP";
    /**
     * HTTPS protocol name
     */
    private static final String PROTOCOL_HTTPS = "HTTPS";
    /**
     * Default Connection Timeout(ms)
     */
    private static final int DEFAULT_TIMEOUT_CONN = 5000;
    /**
     * Default Read Timeout(ms)
     */
    private static final int DEFAULT_TIMEOUT_READ = 10000;
    //endregion Constants-----------------------------------------------------------------------------------------------
    //region Properties-------------------------------------------------------------------------------------------------
    /**
     * Thread pool
     */
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    /**
     * Request will be encoded with this unless the 'requestEncoding' parameter was specified.
     * It will be initialized as {@link StandardEncoding#UTF_8}
     */
    private static final String DEFAULT_REQUEST_ENCODING = Charset.defaultCharset().name();
    //endregion Properties----------------------------------------------------------------------------------------------
    //region Constructors & Initialization------------------------------------------------------------------------------

    /**
     * Prevent instantiation by other class
     */
    private HttpClient()
    {
    }

    /**
     * Get the singleton instance, and init properties with default values.
     *
     * @return {@link SingletonHolder#INSTANCE}
     */
    public static HttpClient getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    //endregion Constructors & Initialization---------------------------------------------------------------------------
    //region Methods----------------------------------------------------------------------------------------------------

    /**
     * Create a holder to cache custom headers and connection attributes.
     *
     * @return {@link RequestHolder}
     */
    public final RequestHolder prepare()
    {
        return new RequestHolder();
    }

    /**
     * @param requestId       Registered in {@link ResponseHandler}
     * @param urlString       [Nullable] The string of the host urlString.
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
     */
    public final void get(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
    {
        sendHttpRequest(requestId, urlString, parameters, RequestMethod.GET, DEFAULT_REQUEST_ENCODING, DEFAULT_TIMEOUT_CONN, DEFAULT_TIMEOUT_READ, null, null, responseHandler);
    }

    /**
     * @param requestId       Registered in {@link ResponseHandler}
     * @param urlString       [Nullable] The string of the host urlString.
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
     */
    public final void post(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
    {
        sendHttpRequest(requestId, urlString, parameters, RequestMethod.POST, DEFAULT_REQUEST_ENCODING, DEFAULT_TIMEOUT_CONN, DEFAULT_TIMEOUT_READ, null, null, responseHandler);
    }

    /**
     * @param requestId       Registered in {@link ResponseHandler}
     * @param urlString       [Nullable] The string of the host urlString.
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
     */
    public final void put(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
    {
        sendHttpRequest(requestId, urlString, parameters, RequestMethod.PUT, DEFAULT_REQUEST_ENCODING, DEFAULT_TIMEOUT_CONN, DEFAULT_TIMEOUT_READ, null, null, responseHandler);
    }

    /**
     * @param requestId       Registered in {@link ResponseHandler}
     * @param urlString       [Nullable] The string of the host urlString.
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
     */
    public final void delete(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
    {
        sendHttpRequest(requestId, urlString, parameters, RequestMethod.DELETE, DEFAULT_REQUEST_ENCODING, DEFAULT_TIMEOUT_CONN, DEFAULT_TIMEOUT_READ, null, null, responseHandler);
    }

    /**
     * Send the HTTP request
     *
     * @param requestId       Registered in {@link ResponseHandler}
     * @param urlString       [Nullable] The string of the host urlString.
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param requestMethod   [Nullable] {@link RequestMethod}
     * @param requestEncoding [Nullable] Encoding for the request's parameters.
     * @param setHeaders      [Nullable] A Map that contains some http headers which should be set to the request.
     * @param addHeaders      [Nullable] A Map that contains some http headers which should be added to the request.
     * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
     */
    void sendHttpRequest(int requestId, String urlString, Map<String, Object> parameters, RequestMethod requestMethod, String requestEncoding, int timeoutConn, int timeoutRead, Map<String, String> setHeaders, Map<String, List<String>> addHeaders, ResponseHandler responseHandler)
    {
        Objects.requireNonNull(responseHandler);
        THREAD_POOL.submit(()->{
            responseHandler.registerRequest(requestId);
            Response response = new Response();
            try
            {
                HttpURLConnection connection = initConnection(urlString, requestMethod, parameters, requestEncoding, timeoutConn, timeoutRead, setHeaders, addHeaders);
                if(connection != null)
                {
                    connection.connect();
                    response.setURL(connection.getURL());
                    response.setStatusCode(connection.getResponseCode());
                    response.setContentType(connection.getContentType());
                    response.setContentEncoding(connection.getContentEncoding());
                    response.setHeaders(connection.getHeaderFields());
                    InputStream inputStream;
                    inputStream = response.getStatusCode() == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream();
                    if(inputStream != null)
                    {//When it actually received something, read them.
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while((len = inputStream.read(buffer)) != -1)
                        {
                            response.setContentLength(response.getContentLength() + len);
                            outputStream.write(buffer, 0, len);
                        }
                        response.setContentBytes(outputStream.toByteArray());
                        inputStream.close();
                        outputStream.close();
                    }
                } else
                {
                    response.setURL(new URL(urlString));
                    response.setStatusCode(0);
                    response.setErrorCode(RequestError.INTERNAL);
                }
            } catch(NoSuchElementException e)
            {
                response.setErrorCode(RequestError.INVALID_CONTENT_TYPE);
                e.printStackTrace();
            } catch(UnsupportedEncodingException e)
            {
                response.setErrorCode(RequestError.INVALID_ENCODING);
                e.printStackTrace();
            } catch(ProtocolException e)
            {
                response.setErrorCode(RequestError.NETWORK);
                e.printStackTrace();
            } catch(MalformedURLException e)
            {
                response.setErrorCode(RequestError.INVALID_URL);
                e.printStackTrace();
            } catch(IOException e)
            {
                response.setErrorCode(RequestError.NETWORK);
                e.printStackTrace();
            } catch(Exception e)
            {
                response.setErrorCode(RequestError.UNKNOWN);
                e.printStackTrace();
            } finally
            {
                responseHandler.handleResponse(requestId, response);
            }
        });
    }

    /**
     * Initialize the connection
     *
     * @param urlString       [NonNull] String form of host urlString. A full expression is expected!
     * @param requestMethod   [NonNull] {@link RequestMethod}
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param requestEncoding [Nullable] Encoding for the request's parameters.
     * @param timeoutConn     Connection Timeout(ms)
     * @param timeoutRead     Data Timeout(ms)
     * @param setHeaders      [Nullable] A Map that contains some http headers which should be set to the request.
     * @param addHeaders      [Nullable] A Map that contains some http headers which should be added to the request.
     *
     * @return Prepared HttpURLConnection or Null.
     *
     * @throws NoSuchElementException
     * @throws ProtocolException
     * @throws UnsupportedEncodingException
     * @throws MalformedURLException
     */
    private HttpURLConnection initConnection(String urlString, RequestMethod requestMethod, Map<String, Object> parameters, String requestEncoding, int timeoutConn, int timeoutRead, Map<String, String> setHeaders, Map<String, List<String>> addHeaders)
            throws IOException, UnsupportedEncodingException, MalformedURLException, ProtocolException
    {
        Objects.requireNonNull(urlString);
        Objects.requireNonNull(requestMethod);
        String requestData = createRequestData(parameters, requestEncoding);
        URL url = null;
        switch(requestMethod)
        {
            case GET:
            case DELETE:
                url = new URL(requestData != null && requestData.length() > 0 ? urlString + '?' + requestData : urlString);
                break;
            case POST:
            case PUT:
                url = new URL(urlString);
                break;
        }
        HttpURLConnection connection = null;
        if(PROTOCOL_HTTP.equalsIgnoreCase(url.getProtocol()))
        {
            connection = (HttpURLConnection)url.openConnection();
        } else if(PROTOCOL_HTTPS.equalsIgnoreCase(url.getProtocol()))
        {
            connection = (HttpsURLConnection)url.openConnection();
        }
        if(connection != null)
        {
            if(addHeaders != null)
            {
                for(Map.Entry<String, List<String>> entry : addHeaders.entrySet())
                {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    for(String value : values)
                    {
                        connection.addRequestProperty(key, value);
                    }
                }
            }
            if(setHeaders != null)
            {
                for(Map.Entry<String, String> entry : setHeaders.entrySet())
                {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.setRequestMethod(requestMethod.name());
            connection.setConnectTimeout(timeoutConn > 0 ? timeoutConn : DEFAULT_TIMEOUT_CONN);
            connection.setReadTimeout(timeoutRead > 0 ? timeoutRead : DEFAULT_TIMEOUT_READ);
            connection.setDoInput(true);
            if(requestMethod == RequestMethod.POST || requestMethod == RequestMethod.PUT)
            {
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                if(requestData != null && requestData.length() > 0)
                {//Fill request body
                    OutputStream out = connection.getOutputStream();
                    out.write(requestData.getBytes());
                    out.flush();
                    out.close();
                }
            }
        }
        return connection;
    }

    /**
     * Prepare the request data with the given parameters in the form of x-www-form-urlencoded.
     *
     * @param parameters      [Nullable] A Map that contains some parameters/Empty.
     * @param requestEncoding [Nullable] Encoding for the request's parameters.
     *
     * @return A String like "name1=value1&name2=value2"
     *
     * @throws UnsupportedEncodingException
     */
    private String createRequestData(Map<String, Object> parameters, String requestEncoding)
            throws UnsupportedEncodingException
    {
        if(parameters != null)
        {
            int size = parameters.size();
            if(size > 0)
            {
                //Specific the capacity based on speculation for better performance.
                StringBuilder stringBuilder = new StringBuilder(size * 10);
                if(requestEncoding == null || requestEncoding.length() == 0)
                {
                    requestEncoding = Charset.defaultCharset().name();
                }
                Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
                Map.Entry<String, Object> next = iterator.next();
                String key = next.getKey();
                Object value = next.getValue();
                if(key != null && key.length() > 0)
                {
                    stringBuilder.append(next.getKey()).append('=').append(URLEncoder.encode(paramFilter(value), requestEncoding));
                }
                while(iterator.hasNext())
                {
                    next = iterator.next();
                    key = next.getKey();
                    value = next.getValue();
                    if(key != null && key.length() > 0)
                    {
                        stringBuilder.append('&').append(key).append('=').append(URLEncoder.encode(paramFilter(value), requestEncoding));
                    }
                }
                return stringBuilder.toString();
            }
        }
        return null;
    }

    /**
     * Replace null with "".
     *
     * @param object Uncertain object.
     *
     * @return String
     */
    private String paramFilter(Object object)
    {
        return object == null ? "" : object.toString();
    }
    //endregion Methods-------------------------------------------------------------------------------------------------
    //region Inner classes----------------------------------------------------------------------------------------------

    /**
     * Singleton instance holder
     */
    private static class SingletonHolder
    {
        private static final HttpClient INSTANCE = new HttpClient();
    }

    /**
     * Hold attributes for a new request which is going to be send out but still has other attributes to add.
     */
    public static class RequestHolder
    {
        /**
         * To encode the request parameters.
         */
        private String REQUEST_ENCODING;
        /**
         * Connection Timeout(ms)
         */
        private int TIMEOUT_CONN = 0;
        /**
         * Data Timeout(ms)
         */
        private int TIMEOUT_READ = 0;
        /**
         * Custom headers to set
         */
        private final Map<String, String> SET_HEADERS = new HashMap<>();
        /**
         * Custom headers to add
         */
        private final Map<String, List<String>> ADD_HEADERS = new HashMap<>();

        /**
         * @param requestEncoding Value of {@link #REQUEST_ENCODING}
         *
         * @return this
         */
        public final RequestHolder setRequestEncoding(String requestEncoding)
        {
            REQUEST_ENCODING = requestEncoding;
            return this;
        }

        /**
         * @param connTimeout Value of {@link #TIMEOUT_CONN}
         *
         * @return this
         */
        public final RequestHolder setConnTimeout(int connTimeout)
        {
            TIMEOUT_CONN = connTimeout;
            return this;
        }

        /**
         * @param readTimeout Value of {@link #TIMEOUT_READ}
         *
         * @return this
         */
        public final RequestHolder setReadTimeout(int readTimeout)
        {
            TIMEOUT_READ = readTimeout;
            return this;
        }

        /**
         * Reset the specific header with new value.
         *
         * @param name  Header name
         * @param value Header value
         *
         * @return this
         */
        public final RequestHolder setHeader(String name, String value)
        {
            if(name != null && name.length() > 0 && value != null)
            {
                SET_HEADERS.put(name, value);
            }
            return this;
        }

        /**
         * Add new value to the specific header
         *
         * @param name  Header name
         * @param value Header value
         *
         * @return this
         */
        public final RequestHolder addHeader(String name, String value)
        {
            if(name != null && name.length() > 0 && value != null)
            {
                List<String> values = ADD_HEADERS.get(name);
                if(values != null)
                {
                    values.add(value);
                } else
                {
                    values = new ArrayList<>(1);
                    values.add(value);
                    ADD_HEADERS.put(name, values);
                }
            }
            return this;
        }

        /**
         * @param requestId       Registered in {@link ResponseHandler}
         * @param urlString       [Nullable] The string of the host urlString.
         * @param parameters      [Nullable] A Map that contains some parameters/Empty.
         * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
         */
        public final void get(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
        {
            getInstance().sendHttpRequest(requestId, urlString, parameters, RequestMethod.GET, REQUEST_ENCODING, TIMEOUT_CONN, TIMEOUT_READ, SET_HEADERS, ADD_HEADERS, responseHandler);
        }

        /**
         * @param requestId       Registered in {@link ResponseHandler}
         * @param urlString       [Nullable] The string of the host urlString.
         * @param parameters      [Nullable] A Map that contains some parameters/Empty.
         * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
         */
        public final void post(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
        {
            getInstance().sendHttpRequest(requestId, urlString, parameters, RequestMethod.POST, REQUEST_ENCODING, TIMEOUT_CONN, TIMEOUT_READ, SET_HEADERS, ADD_HEADERS, responseHandler);
        }

        /**
         * @param requestId       Registered in {@link ResponseHandler}
         * @param urlString       [Nullable] The string of the host urlString.
         * @param parameters      [Nullable] A Map that contains some parameters/Empty.
         * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
         */
        public final void put(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
        {
            getInstance().sendHttpRequest(requestId, urlString, parameters, RequestMethod.PUT, REQUEST_ENCODING, TIMEOUT_CONN, TIMEOUT_READ, SET_HEADERS, ADD_HEADERS, responseHandler);
        }

        /**
         * @param requestId       Registered in {@link ResponseHandler}
         * @param urlString       [Nullable] The string of the host urlString.
         * @param parameters      [Nullable] A Map that contains some parameters/Empty.
         * @param responseHandler [NonNull] Implementation of {@link ResponseHandler}
         */
        public final void delete(int requestId, String urlString, Map<String, Object> parameters, ResponseHandler responseHandler)
        {
            getInstance().sendHttpRequest(requestId, urlString, parameters, RequestMethod.DELETE, REQUEST_ENCODING, TIMEOUT_CONN, TIMEOUT_READ, SET_HEADERS, ADD_HEADERS, responseHandler);
        }
    }
    //endregion---------------------------------------------------------------------------------------------------------
}