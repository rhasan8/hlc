package com.optum.ipp.sendemail;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
@Component
@Slf4j
public class SendEmail {
    private static String USER;
    @Value("${spring.mail.username}")
    public void setUserStatic(String user){
        SendEmail.USER = user;
    }
    private static String PWD;
    @Value("${spring.mail.password}")
    public void setPwdStatic(String pwd) {
        SendEmail.PWD = pwd;
    }
    private static String TO;
    @Value("${spring.mail.properties.to}")
    public void setToStatic(String to) {
        SendEmail.TO = to;
    }
    private static String ENV = "";
    private static String IPP_NAMESPACE;
    @Value("${spring.ipp.namespace}")
    public void setNamespaceStatic(String namespace) {
        SendEmail.IPP_NAMESPACE = namespace;
    }
    private static int addemailfrequency;
    @Value("${MAIL_FREQUENCY}")
    public void setMfreqStatic(int mfrq) {
        SendEmail.addemailfrequency = mfrq;
    }

    long now = new Date().getTime();
    Date curTime = new Date(now);
    Date targetTime = new Date();

    public void sendEmail(String msg) {
        String k8url = "";
        if (IPP_NAMESPACE.equalsIgnoreCase("ipp-prod")) {
            k8url = "https://k8s-dash-prod-ctc.optum.com/#/pod?namespace=ipp-prod";
            ENV = "Prod";
        } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-non-prod")) {
            k8url = "https://k8s-dash-nonprod-ctc.optum.com/#/pod?namespace=ipp-non-prod";
            ENV = "Dev";
        } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-stage")) {
            k8url = "https://k8s-dash-prod-elr.optum.com/#/pod?namespace=ipp-stage";
            ENV = "Stage";
        }
        Calendar lastTime = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        long time_at = System.currentTimeMillis();
        boolean email_sent = false;
        log.debug("Started Email process....");
        String host = "mail.uhc.com";
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        Session session = Session.getDefaultInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER, PWD);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom("RapidAI@optum.com");
            String[] recipientList = TO.split(",");
            InternetAddress[] recipientAddress = new InternetAddress[recipientList.length];
            int counter = 0;
            for (String recipient : recipientList) {
                recipientAddress[counter] = new InternetAddress(recipient.trim());
                counter++;
            }
            message.setRecipients(Message.RecipientType.TO, recipientAddress);
            message.setSubject(ENV + " : IPP automation process generated email. Please do not reply to this email.");
            BodyPart messageBodyPart1 = new MimeBodyPart();
            messageBodyPart1.setText(ENV + " : IPP Kubernetes pod's healthcheck detected some issue.\n \n " + msg + "\n\n" + "kindly check the kubernetes logs for further analysis: " + k8url);
            Multipart multipartObject = new MimeMultipart();
            multipartObject.addBodyPart(messageBodyPart1);
            message.setContent(multipartObject);
            SimpleDateFormat sdf
                    = new SimpleDateFormat(
                    "dd-MM-yyyy HH:mm:ss");
            long now1 = new Date().getTime();
            Date curTime = new Date(now1);
            log.debug("targetTime : " + targetTime + "curTime :" + curTime);
            if((targetTime.compareTo(curTime) <= 0)) {
                //log.info("Sending email at :" + curTime + "Pausing for 30 minutes" );
                Transport.send(message);
                email_sent = true;
                targetTime = addMinutesToDate(addemailfrequency, curTime);
                log.debug("message sent successfully at : " + Calendar.getInstance().getTime() + "....");
                String lgm = StringUtils.substringBetween(msg, "[", "]");
                log.warn("disconnected pod's : [" + lgm + "]");
            }
        } catch (MessagingException e) {
            log.error("Email send failed....", e);
        }
    }

    public   Date addMinutesToDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;
        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        log.debug("afterAddingMins : " + afterAddingMins + "....");
        return afterAddingMins;

    }
}
