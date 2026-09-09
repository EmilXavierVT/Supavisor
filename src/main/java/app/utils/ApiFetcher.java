package app.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiFetcher {

    public static JsonNode getApiDataWithMapper(String url, ObjectMapper objectMapper){

        try {
            JsonNode node = objectMapper.readTree(new URI(url).toURL());
//            System.out.println(node.toPrettyString());
            return node;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    public  static String getApiData(String url) {
        HttpClient client = HttpClient.newHttpClient();
        String result = null;

        try {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(new URI(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .GET()
                    .build();
            HttpResponse<String> response =  client.send(request, HttpResponse.BodyHandlers.ofString());



            if(response.statusCode() == 200) {

                String bodytext = response.body();
                System.out.println(bodytext);

                result = bodytext;

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }


}
