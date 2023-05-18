package com.optum.ipp.sendemail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
@Component
@Slf4j
public class DbEmail {
    private static String USER;
    @Value("${spring.mail.username}")
    public void setUserStatic(String user){
        DbEmail.USER = user;
    }
    private static String PWD;
    @Value("${spring.mail.password}")
    public void setPwdStatic(String pwd) {
        DbEmail.PWD = pwd;
    }
    private static String TO;
    @Value("${spring.mail.properties.to}")
    public void setToStatic(String to) {
        DbEmail.TO = to;
    }
    private static String ENV = "";
    private static String IPP_NAMESPACE;
    @Value("${spring.ipp.namespace}")
    public void setNamespaceStatic(String namespace) {
        DbEmail.IPP_NAMESPACE = namespace;
    }
    private static int addemailfrequency;
    @Value("${MAIL_FREQUENCY}")
    public void setMfreqStatic(int mfrq) {
        DbEmail.addemailfrequency = mfrq;
    }
    long now = new Date().getTime();
    Date curTime = new Date(now);
    Date targetTime = new Date();
    public void sendEmail(String msg) {
        String vmHost = "";
        if (IPP_NAMESPACE.equalsIgnoreCase("ipp-prod")) {
            vmHost = "ssh ipp_svc@rp000005321";
            ENV = "Prod";
        } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-non-prod")) {
            vmHost = "ssh ipp_svc@rn000005214";
            ENV = "Dev";
        } else if (IPP_NAMESPACE.equalsIgnoreCase("ipp-stage")) {
            vmHost = "ssh ipp_svc@rn000013091";
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
            messageBodyPart1.setText(ENV + " : IPP Kubernetes pod's healthcheck detected some issue with Postgresql on VM.\n \n " + msg + "\n\n" + "kindly check the VM logs for further analysis: " + vmHost);
            Multipart multipartObject = new MimeMultipart();
            multipartObject.addBodyPart(messageBodyPart1);
            //multipartObject.addBodyPart(messageBodyPart2);
            message.setContent(multipartObject);
            long now1 = new Date().getTime();
            Date curTime = new Date(now1);
            log.debug("targetTime : " + targetTime + "curTime :" + curTime);
            if((targetTime.compareTo(curTime) <= 0)) {
                //log.info("Sending email at :" + curTime + "Pausing for 30 minutes" );
                Transport.send(message);
                email_sent = true;
                targetTime = addMinutesToDate(addemailfrequency, curTime);
                log.debug("message sent successfully at : " + Calendar.getInstance().getTime() + "....");
                log.warn(msg);
            }
        } catch (MessagingException e) {
            log.error("Email send failed....", e);
        }
    }

    public   Date addMinutesToDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        log.debug("afterAddingMins : " + afterAddingMins + "....");
        return afterAddingMins;

    }
}
