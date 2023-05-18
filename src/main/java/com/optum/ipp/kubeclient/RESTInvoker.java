package com.optum.ipp.kubeclient;

import com.optum.ipp.sendemail.SendEmail;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import reactor.netty.http.client.HttpClient;

@Component
@Slf4j
public class RESTInvoker {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static final int bufferSize = 8000;
    private static String ENV = "";
    private static String WORK_ITEM_URL;
    private static String USER;
    private static String TOKENURL;
    private static String DBHOST;
    private static String SW_HOST;
    private static String IPP_TOKEN = "";
    @Value("${spring.db.host}")
    public void setDbhostStatic(String dbhost){
        RESTInvoker.DBHOST = dbhost;
    }
    @Value("${spring.sw.host}")
    public void setSwhostStatic(String swhost){
        RESTInvoker.SW_HOST = swhost;
    }
    @Value("${spring.app.username}")
    public void setNameStatic(String user) {
        RESTInvoker.USER = user;
    }
    private static String PWD;
    @Value("${spring.app.password}")
    public void setPwdStatic(String pwd) {
        RESTInvoker.PWD = pwd;
    }
    @Value("${spring.ipp.tokenurl}")
    public void setToekurlStatic(String tokenurl) {
        RESTInvoker.TOKENURL = tokenurl;
    }
    @Value("${spring.ipp.workitemurl}")
    public void setWorkitemurlStatic(String workitemurl) {
        RESTInvoker.WORK_ITEM_URL = workitemurl;
    }
    private static String IPP_NAMESPACE;
    @Value("${spring.ipp.namespace}")
    public void setNamespaceStatic(String namespace) {
        RESTInvoker.IPP_NAMESPACE = namespace;
    }
    public ArrayList<ArrayList<String>> client() throws JSONException {
        ArrayList<ArrayList<String>> clientList = new ArrayList<ArrayList<String>>();
        SendEmail sne = null;
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", USER);
        formData.add("password", PWD);
        String token = WebClient.create()
                .post()
                .uri(TOKENURL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                //.timeout(Duration.ofMillis(600000))
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("webclient token error=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                    //return Mono.error(new RuntimeException("4xx"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token error=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .block();
        String token1="";
        if (token !=null){token1 = token.substring(1, token.length() - 1);}
        logger.debug("webclient token1=" + token1);
        //String token1 = token.substring(1, token.length() - 1);
        WebClient webClient = WebClient.builder()
                .baseUrl(WORK_ITEM_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        bodyValues.add("inline", "clients(true)");
        bodyValues.add("params", "{}");

        String response1 = webClient.post()
                .uri(WORK_ITEM_URL)
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(bodyValues))
                //.timeout(Duration.ofSeconds(5))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("webclient token=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .block();
        logger.debug("response =" + response1);
        if (response1 != null){
            if (response1.substring(0, 1).equals("[")) {
                JSONArray jsonarray1 = new JSONArray(response1.toString());
                for (int i = 0; i < jsonarray1.length(); i++) {
                    ArrayList<String> tempArrayList = new ArrayList<>();
                    tempArrayList.add(jsonarray1.getJSONArray(i).getJSONObject(0).optString("uuid"));
                    tempArrayList.add(jsonarray1.getJSONArray(i).getString(9));
                    tempArrayList.add(jsonarray1.getJSONArray(i).getString(1));
                    clientList.add(tempArrayList);
//                  clientList.add(jsonarray1.getJSONObject(i).optString("uuid"));
                }
            }
        }
        logger.debug("clientList from Server ...." + clientList.toString());
        return clientList;
    }

    public ArrayList<ArrayList<String>> agents() throws JSONException {
        ArrayList<ArrayList<String>> clientList2 = new ArrayList<ArrayList<String>>();
        SendEmail sne = null;
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", USER);
        formData.add("password", PWD);
        String token = WebClient.create()
                .post()
                .uri(TOKENURL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                //.timeout(Duration.ofMillis(600000))
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("webclient token error=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                    //return Mono.error(new RuntimeException("4xx"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token error=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .block();
        String token1="";
        if (token !=null){token1 = token.substring(1, token.length() - 1);}
        logger.debug("webclient token1=" + token1);
        //String token1 = token.substring(1, token.length() - 1);
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection().compress(true)))
                .baseUrl(WORK_ITEM_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        MultiValueMap<String, String> bodyValues2 = new LinkedMultiValueMap<>();
        bodyValues2.add("path", "/users/bjakkar/hc_main.mini");
        bodyValues2.add("params", "{}");
        ArrayList<String> response2 = webClient.post()
                .uri(WORK_ITEM_URL)
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(bodyValues2))
                //.timeout(Duration.ofSeconds(5))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("webclient token=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call2 Failed with Auth retry : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call to run hc_main.mini script Failed with Auth retry : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(new ParameterizedTypeReference<ArrayList<String>>() {})
                .block();
        logger.debug("response =" + response2);
        if (response2 != null){
//            System.out.println("sdsdsd"+response2 );
            clientList2.removeAll(clientList2);
            for(int i=0;i<response2.size();i += 3){
                ArrayList<String> tempArrayList = new ArrayList<>();
                tempArrayList.add(response2.get(i));
                tempArrayList.add(response2.get(i+1));
                tempArrayList.add(response2.get(i+2));
                clientList2.add(tempArrayList);
            }
            //
//            if (response2.substring(0, 1).equals("[")) {
//                clientList2.removeAll(clientList2);
//                clientList2.add(response2.substring(1,response2.length()-1));
//            }
        } else{
            ArrayList<String> tempError = new ArrayList<>();
            tempError.add("Error running hc_main.mini script on the IPP workbench");
            tempError.add("null");
            tempError.add("null");
            clientList2.add(tempError);
        }
        logger.debug("clientList from Server ...." + clientList2.toString());
        //
        return clientList2;
    }
    public String SeaweedStatus() throws JSONException {
    	if (SW_HOST == null || (IPP_NAMESPACE.equalsIgnoreCase("ipp-stage") || IPP_NAMESPACE.equalsIgnoreCase("ipp-prod"))) return "N/A";
        SendEmail sne = null;
        String SeaweedStatus ="";
        WebClient webClient = WebClient.builder()
                .baseUrl(SW_HOST)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection().compress(true)))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        MultiValueMap<String, String> bodyValues3 = new LinkedMultiValueMap<>();
        try {
            String response3 = webClient.post()
                    .uri(":8080/ui/index.html")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(bodyValues3))
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, response -> {
                        logger.info("webclient token=" + "4xx error");
                        StringBuilder msg = new StringBuilder();
                        ;
                        msg.append("IPP WorkItem Rest Service Call3 Failed with Auth retry : HTTP error code : " + "4xx error");
                        sne.sendEmail(msg.toString());
                        return Mono.empty();
                    })
                    .onStatus(HttpStatus::is5xxServerError, response -> {
                        logger.info("webclient token=" + "5xx error");
                        StringBuilder msg = new StringBuilder();
                        ;
                        msg.append("IPP WorkItem Rest Service Call2 Failed with Auth retry : HTTP error code : " + "5xx error");
                        sne.sendEmail(msg.toString());
                        return Mono.empty();
                    })
                    .bodyToMono(String.class)
                    .onErrorMap(t -> {
                        SeaweedStatus.concat("Error connecting to SeaweedFS Server");
                        return new Throwable();
                    })
                    .block();
            logger.debug("response =" + response3);
            if (response3 != null){
                String HTMLSTring = response3;
                Document html = Jsoup.parse(HTMLSTring);
                String title = html.title();
                String td = html.body().getElementsByTag("tbody").text();
                String usages = "";
                if(td != null){
                    String[] Seaweed_info = td.split(" ");
                    if(Seaweed_info != null && Seaweed_info.length > 0){
                        usages = Seaweed_info[5];
                        if(Seaweed_info[1] != null){
                            usages += " of "+Seaweed_info[1]+Seaweed_info[2];
                        }
                    }
                }
                logger.debug("SeaweedFS Status ...." + title+":Usage="+ usages);
                return SeaweedStatus.concat(usages);
            } else {
                return SeaweedStatus.concat("Error connecting to SeaweedFS Server");
            }
        } catch(Exception e) {
            return SeaweedStatus;
        }
        //logger.debug("SeaweedFS Status ...." + response3.toString());
        //return SeaweedStatus;
    }

    public ArrayList<Object>ClientsDetail() throws JSONException {
        ArrayList<Object> clientList = new ArrayList<Object>();
        SendEmail sne = null;
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", USER);
        formData.add("password", PWD);
        String token = WebClient.create()
                .post()
                .uri(TOKENURL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                //.timeout(Duration.ofMillis(600000))
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("ClientsDetail webclient token error=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                    //return Mono.error(new RuntimeException("4xx"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token error=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    msg.append("IPP Token Rest Service Call Failed : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .block();
        String token1="";
        if (token !=null){
            token1 = token.substring(1, token.length() - 1);
            IPP_TOKEN = token1;
        };
        logger.debug("ClientsDetail webclient token1=" + token1);
        //String token1 = token.substring(1, token.length() - 1);
        WebClient webClient = WebClient.builder()
                .baseUrl(WORK_ITEM_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        bodyValues.add("inline", "clients()");
        bodyValues.add("params", "{}");

        String response1 = webClient.post()
                .uri(WORK_ITEM_URL)
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(bodyValues))
                //.timeout(Duration.ofSeconds(5))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    logger.info("ClientsDetail webclient token=" + "4xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "4xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    logger.info("webclient token=" + "5xx error");
                    StringBuilder msg = new StringBuilder();
                    ;
                    msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "5xx error");
                    sne.sendEmail(msg.toString());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .block();

        ArrayList<Object> Orchies = new ArrayList<Object>();
        ArrayList<String> Compies = new ArrayList<String>(5);
        ArrayList<String> InactivePods = new ArrayList<String>();
        int NoOfOrchies = 0;
        if (response1 != null){
            JSONObject data = new JSONObject(response1);
            if(data.isNull("map")){
                return clientList;
            }
            JSONArray arrayData = data.getJSONArray("map");
            NoOfOrchies = arrayData.length();
            HashMap<String, ArrayList> ConnCompiesObj = new HashMap<String, ArrayList>();
            List<JSONArray> sortedArrayData = new ArrayList<>();
            for(int i = 0; i < arrayData.length(); i++){
                JSONArray t = arrayData.getJSONArray(i);
                sortedArrayData.add(t);
            };
            Collections.sort(sortedArrayData, new Comparator<JSONArray>(){
                @Override
                public int compare(JSONArray a, JSONArray b) {
                    try{
                        String s1 = (String)a.get(0);
                        String s3 = (String)b.get(0);
                        return s1.compareTo(s3);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    return  0;
                }
            });
            for(int i = 0; i < sortedArrayData.size(); i++) {
                ArrayList<Object> Orch_ArrayList = new ArrayList<Object>();
                String OrchyPodName = sortedArrayData.get(i).getString(0);
                Orch_ArrayList.add(OrchyPodName);
                Orch_ArrayList.add(0);
                Orch_ArrayList.add(0);

                JSONArray ConnectedCompies = sortedArrayData.get(i).getJSONArray(1);
                for(int j = 0; j < ConnectedCompies.length(); j++){
                    String CompyPodName = ConnectedCompies.getJSONArray(j).getString(9);
                    String Status = ConnectedCompies.getJSONArray(j).getString(1);
                    ArrayList<Object> CompyDetails = new ArrayList<Object>();
                    CompyDetails.add(Status);
                    Orch_ArrayList.set(1, (int)Orch_ArrayList.get(1)+1);
                    if(Status.equalsIgnoreCase("ACTIVE")) Orch_ArrayList.set(2, (int)Orch_ArrayList.get(2)+1);
                    else{
                        if(!InactivePods.contains(CompyPodName)) InactivePods.add(CompyPodName);
                    }
                    boolean warning = false;
                    if (ConnectedCompies.getJSONArray(j).getInt(2) != ConnectedCompies.getJSONArray(j).getInt(3)) warning = true;
                    CompyDetails.add(warning);
                    if(ConnCompiesObj.containsKey(CompyPodName)){
                        ArrayList TempArrayList =  ConnCompiesObj.get(CompyPodName);
                        TempArrayList.set(i, CompyDetails);
                    }else{
                        Compies.add(CompyPodName);
                        ArrayList<Object> TempArrayList = new ArrayList<Object>(Arrays.asList(new Object[NoOfOrchies]));
                        TempArrayList.set(i, CompyDetails);
                        ConnCompiesObj.put(CompyPodName,TempArrayList);
                    }
                }
                Orchies.add(Orch_ArrayList);
            }
            clientList.add(ConnCompiesObj);
        }
        Compies.sort(Comparator.naturalOrder());
        clientList.add(0, Compies);
        clientList.add(0, Orchies);
        clientList.add(InactivePods);
        logger.debug("ClientsDetail from Server ...." + clientList.toString());
        return clientList;
    };

    public HashMap<String, Object> DBStorageUsage() throws JSONException {
        HashMap<String, Object> response = new HashMap<String, Object>();
        if (IPP_TOKEN == null || IPP_TOKEN == "" ){
            response.put("db_usage_error", "Error while getting db usage info");
            return response;
        }
        WebClient webClient = WebClient.builder()
                .baseUrl(WORK_ITEM_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
        MultiValueMap<String, String> bodyValues3 = new LinkedMultiValueMap<>();
        bodyValues3.add("inline", "status::dbspace()");
        bodyValues3.add("params", "{}");
        try {
            String response3 = webClient.post()
                    .uri(WORK_ITEM_URL)
                    .header("Authorization", "Bearer " + IPP_TOKEN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(bodyValues3))
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, res -> {
                        logger.info("DBStorageUsage webclient token=" + "4xx error");
                        StringBuilder msg = new StringBuilder();
                        msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "4xx error");
                        response.put("db_usage_error", "Error while getting db usage info");
                        return Mono.empty();
                    })
                    .onStatus(HttpStatus::is5xxServerError, res -> {
                        logger.info("webclient token=" + "5xx error");
                        StringBuilder msg = new StringBuilder();
                        msg.append("IPP WorkItem Rest Service Call Failed with Auth retry : HTTP error code : " + "5xx error");
                        response.put("db_usage_error", "Error while getting db usage info");
                        return Mono.empty();
                    })
                    .bodyToMono(String.class)
                    .block();
            logger.debug("response =" + response3);
            if (response3 != null){
                JSONArray data = new JSONArray(response3);
                for(int i = 0; i < data.length(); i++) {
                    JSONArray each = data.getJSONArray(i);
                    if(each.get(each.length()-1).equals("/var")) {
                        String usage = each.get(each.length()-2).toString();
                        usage = usage.substring(0, usage.length() - 1);
                        String total = each.get(1).toString();
                        total = ((Integer.parseInt(total)/1000)/1000)+"G";
                        response.put("db_storage_usage", Integer.parseInt(usage));
                        response.put("db_storage_total", total);
                    };
                    if(each.get(each.length()-1).equals("/app1")) {
                        String usage = each.get(each.length()-2).toString();
                        usage = usage.substring(0, usage.length() - 1);
                        String total = each.get(1).toString();
                        total = ((Integer.parseInt(total)/1000)/1000)+"G";
                        response.put("bk_storage_usage", Integer.parseInt(usage));
                        response.put("bk_storage_total", total);
                    };
                }
                return response;
            } else {
                response.put("db_usage_error", "Error while getting db usage info");
                return response;
            }
        } catch(Exception e) {
            response.put("db_usage_error", "Error while getting db usage info");
            return response;
        }
    };
}