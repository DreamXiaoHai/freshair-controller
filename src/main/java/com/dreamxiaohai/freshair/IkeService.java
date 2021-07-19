package com.dreamxiaohai.freshair;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class IkeService {
    private static IkeService instance = new IkeService();
    private CloseableHttpClient httpClient;
    private static final String BASE_URL = "https://cn.ikelink.com/ubus";
    private IkeService(){
        this.httpClient = new DefaultHttpClient();
    }

    public static IkeService getInstance(){
        return instance;
    }

    public TokenInfo getToken(String user, String password){
        String requestBodyString = "{  \"id\" : 40,  \"method\" : \"call\",  \"jsonrpc\" : \"2.0\",  \"params\" : [    \"\",    \"db_agent2\",    \"user_login\",    {      \"version\" : 1,      \"lang\" : 1,      \"flavor\" : \"iKECIN\",      \"platform\" : 1    }  ]}";
        JSONObject requestBody = (JSONObject) JSON.parse(requestBodyString);
        ((JSONObject)((JSONArray)requestBody.get("params")).get(3)).put("phone", user);
        ((JSONObject)((JSONArray)requestBody.get("params")).get(3)).put("passwd", password);
        HttpPost post = new HttpPost(BASE_URL);
        post.setEntity(new StringEntity(requestBody.toJSONString(), "UTF-8"));
        try {
            CloseableHttpResponse response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200){
                System.out.println(String.format("Get token failed, result status is %s, response: %s", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity())));
            }else{
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = (JSONObject) JSONObject.parse(resultString);
                if(null != result.get("result") && ((JSONArray)result.get("result")).size() > 1){
                    JSONObject userInfo = ((JSONObject)((JSONArray)result.get("result")).get(1));
                    String session = userInfo.getString("session");
                    String userId = userInfo.getString("user_id");
                    return new TokenInfo(session, userId);
                }else{
                    System.out.println(String.format("Can't parse result:%s", resultString));
                }
            }
        } catch (IOException e) {
            System.err.println("Get token failed");
            e.printStackTrace();
        }
        return null;
    }

//    private int fakePower = 50;
//    public int fakeGetPower(String psn, TokenInfo tokenInfo) throws TokenExpireException{
//        return fakePower;
//    }
//
//    public void fakeDownPower(){
//        fakePower = 0;
//    }

    /**
     * 获取电源功率
     * @param psn
     * @param token
     * @return
     * @throws TokenExpireException
     */
    public int getPower(String psn, TokenInfo token) throws TokenExpireException{
        JSONObject requestBody = this.getDeviceInfoRequestBody(psn, 27, token.getToken());
        HttpPost post = new HttpPost(BASE_URL);
        post.setEntity(new StringEntity(requestBody.toJSONString(), "UTF-8"));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200){
                if(response.getStatusLine().getStatusCode() == 401){
                    throw new TokenExpireException(response.toString());
                }
                System.out.println(String.format("get power info failed, result status is %s, response: %s", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity())));
            }else{
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = (JSONObject) JSONObject.parse(resultString);
                this.throwTokenTimeOut(result);
                if(null != result.get("result") && ((JSONArray)result.get("result")).size() > 1){
                    JSONObject powerInfo = ((JSONObject)((JSONArray)result.get("result")).get(1));
                    return powerInfo.getInteger("key_P");
                }else{
                    System.out.println(String.format("Can't parse result:%s", resultString));
                }
            }
        } catch (IOException e) {
            System.err.println("get power failed");
            e.printStackTrace();
        }finally {
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    /**
     * 新风是否开启
     * @param fsn
     * @param token
     * @return
     * @throws TokenExpireException
     */
    public boolean isFreshAirOn(String fsn, TokenInfo token) throws TokenExpireException{
        JSONObject requestBody = this.getDeviceInfoRequestBody(fsn, 123, token.getToken());
        HttpPost post = new HttpPost(BASE_URL);
        post.setEntity(new StringEntity(requestBody.toJSONString(), "UTF-8"));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200){
                if(response.getStatusLine().getStatusCode() == 401){
                    throw new TokenExpireException(response.toString());
                }
                System.out.println(String.format("get freshair info failed, result status is %s, response: %s", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity())));
            }else{
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = (JSONObject) JSONObject.parse(resultString);
                this.throwTokenTimeOut(result);
                if(null != result.get("result") && ((JSONArray)result.get("result")).size() > 1){
                    JSONObject freshAirInfo = ((JSONObject)((JSONArray)result.get("result")).get(1));
                    return !freshAirInfo.getBoolean("k_close");
                }else{
                    System.out.println(String.format("Can't parse result:%s", resultString));
                }
            }
        } catch (IOException e) {
            System.err.println("get power failed");
            e.printStackTrace();
        }finally {
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public String getDevicePassword(String sn, TokenInfo tokenInfo) throws TokenExpireException{
        JSONObject requestBody = this.getAllDeviceRequestBody(tokenInfo);
        HttpPost post = new HttpPost(BASE_URL);
        post.setEntity(new StringEntity(requestBody.toJSONString(), "UTF-8"));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200){
                if(response.getStatusLine().getStatusCode() == 401){
                    throw new TokenExpireException(response.toString());
                }
                System.out.println(String.format("get all device info failed, result status is %s, response: %s", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity())));
            }else{
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = (JSONObject) JSONObject.parse(resultString);
                this.throwTokenTimeOut(result);
                if(null != result.get("result") && ((JSONArray)result.get("result")).size() > 1){
                    JSONObject allDeviceInfo = ((JSONObject)((JSONArray)result.get("result")).get(1));
                    JSONArray devices = (JSONArray) allDeviceInfo.get("devices");
                    List<String> password = devices.stream().filter(t -> ((JSONObject) t).getString("sn").equals(sn)).map(t -> ((JSONObject) t).getString("passwd")).collect(Collectors.toList());
                    if(password.size() > 0){
                        return password.get(0);
                    }else{
                        System.out.println(String.format("Can't get password for sn:%s, response is %s", sn, resultString));
                        return null;
                    }
                }else{
                    System.out.println(String.format("Can't parse result:%s", resultString));
                }
            }
        } catch (IOException e) {
            System.err.println("get sn password failed");
            e.printStackTrace();
        } finally {
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 设置新风的开关
     * @param on
     * @param sn
     * @param token
     * @param password
     * @return
     * @throws TokenExpireException
     */
    public boolean setFreshAirSwitch(boolean on, String sn, TokenInfo token, String password) throws TokenExpireException{
        JSONObject requestBody = this.setFreshAirRequestBody(on, sn, token, password);
        HttpPost post = new HttpPost(BASE_URL);
        post.setEntity(new StringEntity(requestBody.toJSONString(), "UTF-8"));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200){
                if(response.getStatusLine().getStatusCode() == 401){
                    throw new TokenExpireException(response.toString());
                }
                System.out.println(String.format("set fresh air status failed, result status is %s, response: %s", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity())));
            }else{
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = (JSONObject) JSONObject.parse(resultString);
                this.throwTokenTimeOut(result);
                return true;
            }
        } catch (IOException e) {
            System.err.println("get sn password failed");
            e.printStackTrace();
        }finally {
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    private JSONObject setFreshAirRequestBody(boolean on, String sn, TokenInfo token, String password){
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", 27);
        requestBody.put("method", "call");
        requestBody.put("jsonrpc","2.0");
        JSONArray params = new JSONArray();
        params.add(token.getToken());
        params.add(sn);
        params.add("set");
        JSONObject setInfo = new JSONObject();
        setInfo.put("version", "1");
        setInfo.put("p_w", password);
        setInfo.put("lang", 1);
        setInfo.put("k_close", !on);
        setInfo.put("flavor", "iKECIN");
        setInfo.put("platform", 1);
        params.add(setInfo);
        requestBody.put("params", params);
        return requestBody;
    }

    private JSONObject getAllDeviceRequestBody(TokenInfo token){
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", 93);
        requestBody.put("method", "call");
        requestBody.put("jsonrpc","2.0");
        JSONArray params = new JSONArray();
        params.add(token.getToken());
        params.add("db_agent2");
        params.add("user_get_info");
        JSONObject info = new JSONObject();
        info.put("version", "1");
        info.put("user_id", token.getUserId());
        info.put("lang", 1);
        info.put("flavor", "iKECIN");
        info.put("platform", 1);
        params.add(info);
        requestBody.put("params", params);
        return requestBody;
    }


    private JSONObject getDeviceInfoRequestBody(String sn, int id, String token){
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", id);
        requestBody.put("method", "call");
        requestBody.put("jsonrpc","2.0");
        JSONArray params = new JSONArray();
        params.add(token);
        params.add(sn);
        params.add("get");
        JSONObject info = new JSONObject();
        info.put("version", "1");
        info.put("lang", 1);
        info.put("flavor", "iKECIN");
        info.put("platform", 1);
        params.add(info);
        requestBody.put("params", params);
        return requestBody;
    }


    private void throwTokenTimeOut(JSONObject response) throws TokenExpireException {
        if(null != response.get("error")){
            JSONObject error = (JSONObject)response.get("error");
            if (error.getInteger("code").equals(-32002)){
                throw new TokenExpireException(response.toJSONString());
            }else if (error.getInteger("code").equals(-32000)){
                System.out.println("Device may not online. please check.");
            }else{
                System.out.println("other response error happened, " + response.toJSONString());
            }
        }
    }
}
