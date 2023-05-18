package com.optum.ipp;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;

@Service
@Slf4j
@EnableScheduling

public class Scheduler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static String IPP_NAMESPACE;
    @Value("${spring.ipp.namespace}")
    public void setNamespaceStatic(String namespace) {
        Scheduler.IPP_NAMESPACE = namespace;
    }
    private static String APP_URL;
    @Value("${spring.ipp.appurl}")
    public void setAppurlStatic(String appurl) {
        Scheduler.APP_URL = appurl;
    }
    @Scheduled(cron = "${process.scedule.cron.expression}")
    public void DriverProcess() throws java.rmi.RemoteException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        //timestamp = new Timestamp(System.currentTimeMillis() - 3600000);
        timestamp = new Timestamp(System.currentTimeMillis() - 300);
        logger.debug("Started healthcheck @ Current timestamp processed:" + timestamp + "  - string" + timestamp.toString());
        URL url;
        try {
            // get URL content
            url = new URL(APP_URL);
            URLConnection conn = url.openConnection();
            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                logger.debug("HealthCheck Info...." + inputLine);
            }
            br.close();
            logger.debug("Healthcheck completed....");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

