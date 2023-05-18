package com.optum.ipp.kubeclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optum.ipp.sendemail.DbEmail;
import com.optum.ipp.sendemail.SendEmail;
import com.optum.ipp.sendemail.StorageEmail;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@EnableScheduling
@SpringBootApplication
@RestController
public class Controller {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    ApplicationContext applicationContext;
    private final SendEmail sendEmail;
    private final StorageEmail storageEmail;
    private final PingDb pingDb;
    private final DbEmail dbEmail;
    private final KubeJavaClient kubeJavaClient;
    private final RESTInvoker restInvoker;
    String dbstatus = "";
    int usedStorage = 0;
    int client = 0;
    int kubeclients = 0;
    String NasStorage="";
    String NasTotalStorage="";
    String SwStorage="";
    ZonedDateTime refreshedAt;
    Map<Object, Object> map;
    ArrayList<String> listActivePods = new ArrayList<String>();
    ArrayList<Object> listInActivePods = new ArrayList<Object>();
    ArrayList<ArrayList<String>> listHcResp = new ArrayList<ArrayList<String>>();
    ArrayList<Object> ClientsDetails = new ArrayList<Object>();
    Integer K8OrchiesNo = 0;
    ArrayList<String> error_msg = new ArrayList<>();
    private static String ENV = "";
    private static String K8Url = "";
    HashMap<String, Object> db_storage = new HashMap<>();
    Date LastDbStorageFatchedAt;

    @Value("${spring.ipp.env}")
    public void setNamespaceStatic(String _env) {
        Controller.ENV = _env;
    }

    @Value("${spring.ipp.k8url}")
    public void setK8Url(String _k8url) { Controller.K8Url = _k8url;}

    @Autowired
    public Controller(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.restInvoker = this.applicationContext.getBean(RESTInvoker.class);
        this.kubeJavaClient = this.applicationContext.getBean(KubeJavaClient.class);
        this.sendEmail = this.applicationContext.getBean(SendEmail.class);
        this.storageEmail = this.applicationContext.getBean(StorageEmail.class);
        this.pingDb = this.applicationContext.getBean(PingDb.class);
        this.dbEmail = this.applicationContext.getBean(DbEmail.class);
    }

    private ZonedDateTime LocalTimeStamp(TimeZone timeZone){
        String usersTimezone = "America/Chicago";
        if(timeZone != null){
            usersTimezone = timeZone.getID();
        };
        ZonedDateTime CurrentTimestamp = ZonedDateTime.now(ZoneId.of(usersTimezone));
        return CurrentTimestamp;
    }

    private String fetchMatrices(String name, TimeZone timeZone, Boolean enableEmails) throws IOException, ApiException, JSONException{
        refreshedAt = LocalTimeStamp(timeZone);
        dbstatus = pingDb.pingdatabase();
        if (!"ok".equals(dbstatus) && enableEmails) {
            dbEmail.sendEmail(dbstatus.toString());
        }
        String used = null;
        StringBuilder lgmsg = new StringBuilder();
        String ippstorage = kubeJavaClient.checkStorage();
        String Seaweedtorage = restInvoker.SeaweedStatus();
        if(Seaweedtorage != null && Seaweedtorage != "" ){
            SwStorage=Seaweedtorage;
        }else{
            error_msg.add("Error in SeaweedFS server");
            SwStorage = "not ok";
        }
        String[] nas_info = ippstorage.split(" ");
        nas_info = Arrays.stream(nas_info).filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);
        if(nas_info != null && nas_info.length >0 ){
            used = nas_info[nas_info.length-2];
            used = used.substring(0, used.length()-1);
            NasTotalStorage = nas_info[1];
        }else{
            logger.debug("Nas usage details not found "+ ippstorage);
            NasTotalStorage = "0 G";
        }

        logger.debug("Substring used = " + used);
        //int usedStorage =0;
        if (null != used && used.length() > 1) {
            NasStorage=used.trim();
            usedStorage = Integer.parseInt(used.trim());
        } else {
            NasStorage="0";
            usedStorage = 0;
        }
        String st = "IPP-storage utilization at :" + usedStorage + "%";
        if (usedStorage >= 95 && enableEmails) {
            storageEmail.sendEmail("ipp-storage utilization at over 95%");
            lgmsg.append("ipp-storage utilization over 95 %,");
        } else {
            lgmsg.append("ipp-storage utilization at :" + usedStorage + "%" + " :: " + "SeaweedFS utilization at :" + Seaweedtorage);
        }
        ArrayList<ArrayList<String>> clientl = restInvoker.client();
        ArrayList<String> kubeclientsl = kubeJavaClient.returnPodList();
        K8OrchiesNo = kubeJavaClient.getK8OrchyList().size();
        ArrayList<Object> client_details = restInvoker.ClientsDetail();
        if(client_details.size() != 0){
            ClientsDetails = client_details;
        };
        long time_diff_ms = 4510731;
        Date CurrentDate = new Date();
        if(LastDbStorageFatchedAt != null) time_diff_ms = CurrentDate.getTime() - LastDbStorageFatchedAt.getTime();
        if(time_diff_ms/(60 * 60 * 1000) >= 1) {
            db_storage = restInvoker.DBStorageUsage();
            LastDbStorageFatchedAt = CurrentDate;
        };
        listInActivePods.clear();
        listInActivePods.add(ClientsDetails.get(3));
        listHcResp.clear();
        listActivePods.clear();
            //logger.info("mapvalue=" + entry.getValue());
        kubeclients = kubeclientsl.size();
        client = clientl.size();
        StringBuilder msg = new StringBuilder();
        msg.append("# of agent's connected to orchestration server:  ").append(client);
        msg.append("\n");
        msg.append("# of computation agents connected to Kubernetes: ").append(kubeclients);
        msg.append("\n");
        lgmsg.append("# of agent's connected to orchestration server:" + client + " ::").append(" # of computation agents connected to Kubernetes:" + kubeclients + " ::").append(" Postgres status:" + dbstatus + " ::").append("Agent status:" + ClientsDetails.get(2));
        MDC.remove("storage_utilization");
        MDC.remove("os_agents");
        MDC.remove("ca_agents");
        MDC.put("storage_utilization", String.valueOf(usedStorage));
        MDC.put("os_agents", String.valueOf(client));
        MDC.put("ca_agents", String.valueOf(kubeclients));
        if (client == kubeclients) {
            logger.info(String.valueOf(lgmsg));
        }
        if (client != kubeclients) {
            // compare the list and display the diffrences
            clientl.forEach((value) -> {
                logger.debug("clientl: " + value);
            });
            List<String> agentl = new ArrayList<>();
            kubeclientsl.forEach((value) -> {
                agentl.add(value.substring(0, 36));
                logger.debug("kubeclientsl: " + value);
            });
            if (clientl.size() > agentl.size()) {
                clientl.removeAll(agentl);
                msg.append("\"podId\":").append(listInActivePods);
                lgmsg.append(" :: disconnected pods:[" + clientl + "]");
            } else {
                clientl.removeAll(clientl);
                msg.append("\"podId\":").append(listInActivePods);
                lgmsg.append(" :: disconnected pods:[" + agentl + "]");
            }
            logger.info(String.valueOf(lgmsg));
            if(enableEmails){
                sendEmail.sendEmail(msg.toString());
            };
            return String.format("  %s", name);
        } else {
            name = "  IPP-Workbench Healthcheck looks good, ".concat(("").concat(lgmsg.toString()));
        }
        //
        ReportController rc = new ReportController();
        return String.format(" %s", name);
    };


    private Boolean checkOverallStatus(){
        error_msg.clear();
        boolean overallStatus = false;
        boolean anyNonScaleDisc = false;
        HashMap<Integer, Integer> multiCompiesDisc = new HashMap<>();
        HashMap<String, Boolean> allNonScaleDisc = new HashMap<>();
        allNonScaleDisc.put("st-", true);
        allNonScaleDisc.put("ds-", true);
        allNonScaleDisc.put("q-", true);
        if(ClientsDetails.size() > 0){
            Map<String, Object> Conn = new ObjectMapper().convertValue(ClientsDetails.get(2), HashMap.class);
            for (Map.Entry<String, Object> entry : Conn.entrySet()){
                if(entry.getValue() != null){
                    ArrayList <Object> ValueArrayList = new ObjectMapper().convertValue(entry.getValue(), ArrayList.class);
                    for (int i = 0; i < ValueArrayList.size(); i++ ){
                        ArrayList <Object> Value = new ObjectMapper().convertValue(ValueArrayList.get(i), ArrayList.class);
                        if(Value != null && Value.get(0) == "INACTIVE"){
                            if(entry.getKey().contains("st-") || entry.getKey().contains("ds-") || entry.getKey().contains("q-")){
                                for(Map.Entry<String, Boolean> each : allNonScaleDisc.entrySet()){
                                    if(entry.getKey().contains(each.getKey())){
                                        String _msg = "One or more non scalable pods inactive";
                                        if(!error_msg.contains(_msg)) error_msg.add(_msg);
                                        allNonScaleDisc.put(each.getKey(), false);
                                    }
                                }
                                anyNonScaleDisc = true;
                            }
                            multiCompiesDisc.put(i, multiCompiesDisc.get(i)+1 );
                        }
                    }
                }
            }
        }
        boolean hasMultiDiscCompies = false;
        for ( Integer value: multiCompiesDisc.values()) {
            if ( value > 1){
                String _msg = "One on more orchies have multiple compies disconnected";
                if(error_msg.contains(_msg)) error_msg.add(_msg);
                hasMultiDiscCompies = true;
            }
        };
        ArrayList<Object> OrchiesList = new ArrayList<>();
        if(ClientsDetails.size() > 0) OrchiesList = new ObjectMapper().convertValue(ClientsDetails.get(0), ArrayList.class);
        if(!dbstatus.equals("ok")) error_msg.add("Postgres DB have some issue");
        if(kubeclients != client) error_msg.add("No. of pods in client() is not same as no. of pods in k8");
        if(K8OrchiesNo != OrchiesList.size()) error_msg.add("No. of orchies in client() is not same as no. of orchies in K8");
        if(usedStorage >= 95) error_msg.add("Nas storage usage reached above 95%");
        if((kubeclients==client) && (dbstatus.equals("ok")) && usedStorage <=95 &&
                !anyNonScaleDisc && !allNonScaleDisc.containsValue(false) && !hasMultiDiscCompies && K8OrchiesNo == OrchiesList.size()){
            overallStatus = true;
        };
        if((db_storage.get("db_storage_usage") != null && (Integer)db_storage.get("db_storage_usage") >= 85) || db_storage.get("db_usage_error") != null) {
            overallStatus = false;
            String _msg = "DB storage usage is more than 85%";
            if(db_storage.get("db_usage_error") != null) _msg = (String)db_storage.get("db_usage_error");
            error_msg.add(_msg);
        };
        return overallStatus;
    };

    private HashMap<String, Object> refreshedAtDiff(ZonedDateTime refreshedAt){
        HashMap<String, Object> res = new HashMap<String, Object>();
        if(refreshedAt != null) {
            Date d2 = new Date();
            Date d1 = new Date(refreshedAt.toInstant().toEpochMilli());
            long diff = d2.getTime() - d1.getTime();
            res.put("days", (int) diff / (1000 * 60 * 60 * 24));
            res.put("hours", diff / (60 * 60 * 1000));
            res.put("minutes", diff / (60 * 1000) % 60);
            res.put("seconds", diff / 1000 % 60);
            return res;
        }else{
            res.put("hours", 0);
            res.put("minutes", 0);
            res.put("seconds", 0);
            return res;
        }
    }
    @GetMapping("/checkPodHealth")
    public String checkPodHealth(@RequestParam(value = "IPP", defaultValue = "Dev : IPP Kubernetes pod's health check detected some issue.kindly check the kubernetes logs for further analysis.") String name, TimeZone timezone) throws IOException, ApiException, JSONException {
        return fetchMatrices(name, timezone, true);
    };
    @org.springframework.stereotype.Controller
    class ReportController {
        //Controller return data in HTML UI
        @RequestMapping("/")
        public String showForm(@RequestParam(value = "ref", required = false) String ref, TimeZone timeZone, Model model) throws IOException, ApiException, JSONException{
            if(ref != null && ref.equals("1")) fetchMatrices("", timeZone, false);
            IppHealthCheck ippHealthCheck = new IppHealthCheck();
            model.addAttribute("ippHealthCheck", ippHealthCheck);
            if (checkOverallStatus()){
                ippHealthCheck.setOverallst("ok");
            } else {
                ippHealthCheck.setOverallst("not ok");
            }
            ippHealthCheck.setPg(dbstatus);
            ippHealthCheck.setCk8(kubeclients);
            ippHealthCheck.setCorchy(client);
            ippHealthCheck.setNst(NasStorage+"%");
            ippHealthCheck.setDst(SwStorage);
            ippHealthCheck.setTimestamp(refreshedAt);
            ippHealthCheck.setRefreshAtDiff(refreshedAtDiff(refreshedAt));
            model.addAttribute("k8url", K8Url);
            model.addAttribute("env", ENV);
            model.addAttribute("listActivePods", listActivePods);
            if (listInActivePods.size() < 1){
                //listInActivePods.add("none");
                model.addAttribute("listInActivePods", listInActivePods);
            } else {
                model.addAttribute("listInActivePods", listInActivePods);
            }
            model.addAttribute("listHcResp", listHcResp);
            model.addAttribute("clientsDetails", ClientsDetails);
            model.addAttribute("k8OrchiesNo", K8OrchiesNo);
            model.addAttribute("error_msg", error_msg);
            return "getHealthCheck_form";
        }

        //Controller return data in JSON format
        @RequestMapping("/json")
        @ResponseBody()
        public Object reportJSON(@RequestHeader("Accept") String accept,@RequestParam(value = "ref", required = false) String ref) throws IOException, ApiException, JSONException {
            if(ref != null && ref.equals("1")) fetchMatrices("",null, false); //To reFetch the data from UI
            HashMap<String, Object> response = new HashMap<>();
            if (checkOverallStatus()){
                response.put("overall", "ok");
            } else {
                response.put("overall", "not ok");
            }
            String nas_usages = NasStorage+"%";
            if(NasTotalStorage != null){
                nas_usages += " of "+NasTotalStorage;
            }
            response.put("database", dbstatus);
            response.put("running_pods", kubeclients);
            response.put("orchies", K8OrchiesNo);
            response.put("nas", nas_usages);
            response.put("nas_usage", NasStorage);
            response.put("other", SwStorage);
            response.put("last_refreshed", refreshedAtDiff(refreshedAt));
            response.put("clientDetails", ClientsDetails);
            response.put("error_msg", error_msg);
            response.put("k8url", K8Url);
            response.put("db_storage", db_storage);
            if(accept.equals("application/json")){
                ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.writeValueAsString(response);
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }else{
                return "Invalid Request Accept parameter";
            }
            return "Invalid Request Accept parameter";
        }

        @GetMapping("/healthCheckReport")
        public String renderUI(Model model) {
            return "redirect:/";
        }
//        @PostMapping("/healthCheckReport")
//        public String submitForm(@ModelAttribute("ippHealthCheck") IppHealthCheck ippHealthCheck) {
//            System.out.println(ippHealthCheck);
//            return "auto_success";
//        }
    }
}


