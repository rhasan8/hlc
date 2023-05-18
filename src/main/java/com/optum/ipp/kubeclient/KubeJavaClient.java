package com.optum.ipp.kubeclient;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KubeJavaClient {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static String IPP_NAMESPACE;
    private static String ENV = "";
    private static String NAS_STORAGE;
    private static String K8_CONFIG_PATH;
    @Value("${spring.ipp.storage}")
    public void setNASStorage(String storage){
        KubeJavaClient.NAS_STORAGE = storage;
    }
    @Value("${spring.ipp.k8_config_path}")
    public void setK8_CONFIG_PATH(String configPath){ KubeJavaClient.K8_CONFIG_PATH = configPath;}


    @Value("${spring.ipp.namespace}")
    public void setNamespaceStatic(String namespace) {
        KubeJavaClient.IPP_NAMESPACE = namespace;
    }
    ArrayList<String> OrchyList = new ArrayList<>();
    public ArrayList<String> returnPodList() throws IOException, ApiException {
        ApiClient client = null;
        OrchyList.clear();
        client = Config.fromConfig(K8_CONFIG_PATH);

//        if (ENV.equalsIgnoreCase("local")) {
//            if (IPP_NAMESPACE.equalsIgnoreCase("ipp-non-prod")) {
//                client = Config.fromConfig("/Users/bjakkar//k8-pod-healthcheck/local_config_Backup");
//            } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-stage")) {
//                client = Config.fromConfig("/Users/bjakkar//k8-pod-healthcheck/local_stage_config");
//            } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-prod")) {
//                client = Config.fromConfig("/Users/bjakkar//k8-pod-healthcheck/local_prod_config");
//            }
//        } else {
//            client = Config.fromConfig("/storage/k8Config");
//            //client = Config.fromConfig("/Users/bjakkar/k8-pod-healthcheck/local_stage_config");
//
//        }
        client.setVerifyingSsl(false);
        client.setDebugging(false);
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList pods = api.listNamespacedPod(IPP_NAMESPACE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                10,
                false);
        ArrayList<String> PodList = new ArrayList();
        int i = 0;
        for (V1Pod item : pods.getItems()) {
            int t = -1;// compare(item.getMetadata().getCreationTimestamp().toDateTime());
            if ("Running".equals(item.getStatus().getPhase().toString()) && (((("c-".equals(item.getMetadata().getName().substring(0, 2).toString())) || ("py-".equals(item.getMetadata().getName().substring(0, 3).toString()))) || ("ds-".equals(item.getMetadata().getName().substring(0, 3).toString()))) || ("q-".equals(item.getMetadata().getName().substring(0, 2).toString())) || ("kt-".equals(item.getMetadata().getName().substring(0, 3).toString())) || ("sig-".equals(item.getMetadata().getName().substring(0, 4).toString())) || ("st-".equals(item.getMetadata().getName().substring(0, 3).toString()))) && (t == -1)) {
                PodList.add(item.getMetadata().getUid()+"::"+item.getMetadata().getName());
                logger.debug("PODS=" + item.getMetadata().getUid() +"Pod Name="+item.getMetadata().getName() + ", Age=" + item.getMetadata().getCreationTimestamp());
                i++;
            }
            if("orch-".equals(item.getMetadata().getName().substring(0, 5).toString())){
                OrchyList.add(item.getMetadata().getName());
            }
        }
        checkStorage();
        return PodList;
    }

    public ArrayList<String> getK8OrchyList(){
        return OrchyList;
    }
    public String checkStorage() {
        String s = "";
        String s1 = "";
        Process p;
        try {
//            if (IPP_NAMESPACE.equalsIgnoreCase("ipp-prod")){
            p = Runtime.getRuntime().exec("df -h "+NAS_STORAGE);
//            } else {
//                p = Runtime.getRuntime().exec("df /storage");
//            }
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null)
                s1 = s;
            logger.debug("ipp-storage df /storage command output: " + s);
            p.waitFor();
            logger.debug("ipp-storage df /storage command exit code: " + p.exitValue());
            p.destroy();
        } catch (InterruptedException | IOException e) {
            logger.warn("Interrupted!", e);
            //return s1;
            Thread.currentThread().interrupt();
        }

        logger.debug("ipp-storage df /storage command return output: " + s1);
        return s1;
    }

    public int compare(DateTime t1) {
        //Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
        DateTime timestamp = new DateTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        logger.debug("pod creation timestamp=" + t1 + "current timestamp + min =" + timestamp);
        if (t1.compareTo(timestamp) < 0) {
            //pod is older than a min old
            return -1;
        } else {
            //pod is less than a min
            return 1;
        }
    }

    public void sendPostRequest() {
        String url = "http://nomad.service.uhgwm110-025040.ap.ctc.development.mesh.uhg.com/v1/jobs";

        // Set the headers
        HttpHeaders headers = new HttpHeaders();

        // Create the HTTP entity with empty body and headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Send the POST request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // Process the response
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.debug("POST request successful. Response: " + response.getBody());

            // Process the response data
            String jsonResponse = response.getBody();

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(jsonResponse);

                for (JsonNode node : root) {
                    String name = node.get("Name").asText();

                    if (name.equals("kafka-ui") || name.equals("grafana") || name.equals("py")) {
                        JsonNode jobSummary = node.get("JobSummary");
                        JsonNode summary = jobSummary.get("Summary").get(name);

                        int queued = summary.get("Queued").asInt();
                        int complete = summary.get("Complete").asInt();
                        int failed = summary.get("Failed").asInt();
                        int running = summary.get("Running").asInt();
                        int starting = summary.get("Starting").asInt();
                        int lost = summary.get("Lost").asInt();
                        int unknown = summary.get("Unknown").asInt();

                        System.out.println("Name: " + name);
                        System.out.println("Queued: " + queued);
                        System.out.println("Complete: " + complete);
                        System.out.println("Failed: " + failed);
                        System.out.println("Running: " + running);
                        System.out.println("Starting: " + starting);
                        System.out.println("Lost: " + lost);
                        System.out.println("Unknown: " + unknown);
                        System.out.println("--------------------------------");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logger.debug("POST request failed. Status code: " + response.getStatusCodeValue());
        }
    }
}

