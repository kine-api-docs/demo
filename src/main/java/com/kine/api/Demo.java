package com.kine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Demo {

    static String API_ACCESS_KEY = "xxxxx";  // 从交易所创建的API KEY
    static String API_SECRET_KEY = "xxxxx";  // API KEY对应的Secret Key

    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static String HOST = "api.kine.exchange";
    static ObjectMapper objectMapper = new ObjectMapper();
    static OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    public static void main(String[] args) {

        // 1. 调用公开接口， 不需要签名
        // 这里直接调用接口获取最新价格， 实际应用中请监听websocket
        String price = getLatestPrice();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");

        // 2. 调用有签名的POST请求
        // 使用API下单
        placeOrder(price);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");

        // 3. 调用有签名的GET请求
        // 查询财务历史
        queryAccountHistory();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");

        // 4. 调用有签名但是无参数的GET请求
        // 查询账户
        queryAccountBalance();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");


        // 5. 创建Websocket连接，并进行身份认证
        connectWebsocketAndDoAuth();
        System.exit(0);
    }

    private static void connectWebsocketAndDoAuth() {
        // 生成带签名的Auth消息
        String apiUrl = "/ws";

        String parameters = "";
        String ts = Long.toString(System.currentTimeMillis());
        final String payload = "GET\n" +
                HOST + "\n" +
                apiUrl + "\n" +
                "accessKey=" + API_ACCESS_KEY + "\n" +
                ts;

        System.out.println(payload);

        String signature = sign(payload);

        String authString = "{\n" +
                "  \"op\": \"AUTH\",\n" +
                "  \"ts\": " + ts + ",\n " +
                "  \"data\": {\n" +
                "    \"accessKey\": \"" + API_ACCESS_KEY + "\",\n" +
                "    \"ts\": " + ts + ",\n" +
                "    \"signature\": \"" + signature + "\"\n" +
                "  }\n" +
                "}";

        System.out.println(authString);
    }

    private static void queryAccountBalance() {
        String apiUrl = "/account/api/v2/account-positions";

        String parameters = "";
        String ts = Long.toString(System.currentTimeMillis());

        final String payload = "GET\n" +
                HOST + "\n" +
                apiUrl + "\n" +
                parameters + "\n" +
                ts;

        System.out.println(payload);
        String signature = sign(payload);
        System.out.println("Signature: " + signature);
        String fullUrl = getFullUrl(apiUrl);

        System.out.println(fullUrl);

        // 这里是POST请求，body参数，json格式

        Request request = new Request.Builder()
                .url(fullUrl + "?" + parameters)
                .header("KINE-API-ACCESS-KEY", API_ACCESS_KEY)
                .header("KINE-API-TS", ts)
                .header("KINE-API-SIGNATURE", signature)
                .get()
                .build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            String resp = response.body().string();

            System.out.println("Resp: " + resp);
            JsonNode jsonNode = objectMapper.readTree(resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void queryAccountHistory() {
        String apiUrl = "/account/api/account-history";

        String parameters = "currency=kUSD&action=3";
        String ts = Long.toString(System.currentTimeMillis());

        final String payload = "GET\n" +
                HOST + "\n" +
                apiUrl + "\n" +
                parameters + "\n" +
                ts;

        System.out.println(payload);
        String signature = sign(payload);
        System.out.println("Signature: " + signature);
        String fullUrl = getFullUrl(apiUrl);

        System.out.println(fullUrl);

        // 这里是POST请求，body参数，json格式

        Request request = new Request.Builder()
                .url(fullUrl + "?" + parameters)
                .header("KINE-API-ACCESS-KEY", API_ACCESS_KEY)
                .header("KINE-API-TS", ts)
                .header("KINE-API-SIGNATURE", signature)
                .get()
                .build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            String resp = response.body().string();

            System.out.println("Resp: " + resp);
            JsonNode jsonNode = objectMapper.readTree(resp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void placeOrder(String price) {
        String apiUrl = "/trade/api/order/v2/place";

        String parameters = "";
        String ts = Long.toString(System.currentTimeMillis());

        final String payload = "POST\n" +
                HOST + "\n" +
                apiUrl + "\n" +
                parameters + "\n" +
                ts;

        System.out.println(payload);
        String signature = sign(payload);
        System.out.println("Signature: " + signature);
        String fullUrl = getFullUrl(apiUrl);

        System.out.println(fullUrl);

        // 这里是POST请求，body参数，json格式
        String bodyContent = "{\n" +
                "    \"symbol\": \"BTCUSD\",\n" +
                "    \"baseAmount\": \"0.0001\",\n" +
                "    \"direct\": \"SELL\",\n" +
                "    \"type\": 1,\n" +
                "    \"price\": \"" + price + "\"\n" +
                "}";

        RequestBody requestBody = RequestBody.create(JSON, bodyContent);

        Request request = new Request.Builder()
                .url(fullUrl)
                .header("KINE-API-ACCESS-KEY", API_ACCESS_KEY)
                .header("KINE-API-TS", ts)
                .header("KINE-API-SIGNATURE", signature)
                .post(requestBody)
                .build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            String resp = response.body().string();

            System.out.println("Resp: " + resp);
            JsonNode jsonNode = objectMapper.readTree(resp);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String sign(String payload) {
        final Mac hmacSha256;
        // sign:
        try {
            hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secKey = new SecretKeySpec(API_SECRET_KEY.getBytes(Charset.defaultCharset()), "HmacSHA256");
            hmacSha256.init(secKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key: " + e.getMessage());
        }

        final byte[] signBytes = hmacSha256.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        final String sign = Base64.getEncoder().encodeToString(signBytes);
        return sign;
    }

    public static String getLatestPrice() {

        String URL = getFullUrl("/market/api/price/BTCUSD");

        System.out.println(URL);
        Request request = new Request.Builder()
                .url(URL)
                .header("Connection", "close")
                .build();

        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            String resp = response.body().string();

            System.out.println("Resp: " + resp);
            JsonNode jsonNode = objectMapper.readTree(resp);

            return jsonNode.get("data").get("price").asText();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getFullUrl(String apiUrl) {

        return String.format("https://%s%s", HOST, apiUrl);
    }
}
