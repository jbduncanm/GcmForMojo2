package com.swjtu.gcmformojo;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 
 * @author xiaonan
 * 网络操作类，包装了get请求和post请求
 */
public class NetUtil {


    private static final int CONNECT_TIME_OUT = 5000;
    private static final int READ_TIME_OUT = 5000;
    private static final String TAG = "GcmForMojo";

    /**
     * 通过Post发送数据，参数使用HashMap传递
     * @param urlStr url参数
     * @param request (参数不可为None，可以传入为空map对象)
     * @return String
     */
    public static String doPostRequest(final String urlStr, final HashMap<String, String> request) {
        // 参数判空和地址判空操作
        if (request == null || "".equals(urlStr) || urlStr == null) {
            Log.d(TAG,"The doPost parmas is empty");
            return "";
        }
        Log.d(TAG,"request = " + request.toString() + " urlStr = " + urlStr);
        int responseCode = 0;
        String response = "";
        HttpURLConnection conn = null;
        OutputStream os = null;
        InputStream in = null;
        try {
            conn = getConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // 发送post数据需要在拿到ResponseCode之前
            os = conn.getOutputStream();
            os.write(Map2Byte(request));
            responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取服务器返回数据
                in = conn.getInputStream();
                response = InputStream2String(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }
    /**
     * 通过Get发送数据，参数使用HashMap传递
     * @param urlStr url参数
     * @param request (参数不可为None，可以传入为空map对象)
     * @return String
     */
    public static String doGetRequest(final String urlStr, final HashMap<String, String> request) {

        // 参数判空和地址判空操作
        if (request == null || "".equals(urlStr) || urlStr == null) {
            Log.d(TAG,"The doGet parmas is empty");
            return "";
        }
        Log.d(TAG,"request is " + request.toString() + " urlStr is"+ urlStr);
        int responseCode = 0;
        String response = "";
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = getConnection(urlStr + "?"+ new String(Map2Byte(request), "UTF-8"));
            Log.d(TAG,urlStr + "?"+ new String(Map2Byte(request), "UTF-8"));
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取服务器返回数据
                in = conn.getInputStream();
                response = InputStream2String(in);
                in.close();
            }
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return response;
    }


    /**
     * 获取连接
     * @return HttpURLConnection
     * @throws IOException
     */
    private static HttpURLConnection getConnection(String path)throws IOException {
        HttpURLConnection conn = null;
        URL url = new URL(path);
        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIME_OUT);
        conn.setReadTimeout(READ_TIME_OUT);
        conn.setUseCaches(false);
        return conn;
    }
    /**
     * 用于将传入Map形式参数转化为可以发送的byte数组
     * @param map 参数
     * @return byte[]
     * @throws UnsupportedEncodingException
     */
    private static byte[] Map2Byte(HashMap<String, String> map)throws UnsupportedEncodingException {
        // 组织请求参数
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        final StringBuilder params = new StringBuilder();
        while (it.hasNext()) {
            Entry<String, String> element = (Entry<String, String>) it.next();
            // 字符用URLEncoder.encode处理
            params.append(URLEncoder.encode(element.getKey(), "UTF-8"));
            params.append("=");
            params.append(URLEncoder.encode(element.getValue(),"UTF-8"));
            params.append("&");
        }
        if (params.length() > 0) {
            params.deleteCharAt(params.length() - 1);
        }
        return params.toString().getBytes("UTF-8");
    }

    /**
     * 可以将输入流直接转换为字符串并且返回
     * @param in 参数
     * @return string
     * @throws IOException
     */
    private static String InputStream2String(InputStream in) throws IOException {
        String temp = "";
        StringBuilder buffer = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in,"UTF-8"));
        while ((temp = br.readLine()) != null) {
            buffer.append(temp);
        }
        br.close();
        in.close();
        return buffer.toString();
    }

}
