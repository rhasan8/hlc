package com.optum.ipp.kubeclient;

import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class IppHealthCheck {
    private ArrayList<String>  listActivePods;
    private ArrayList<Object>  listInActivePods;
    private ArrayList<ArrayList<String>>  listHcResp;
    private String overallst;
    private String pg;
    private String nst;
    private String dst;
    private Integer corchy;
    private Integer ck8;
    private boolean autokill;
    private boolean sendemail;
    private ZonedDateTime timestamp;
    private HashMap<String, Object> refreshAtDiff;

    public ArrayList<String>  getListActivePods() {
        return listActivePods;
    }

    public void setListActivePods(ArrayList<String>  listActivePods) {
        this.listActivePods = listActivePods;
    }

    public ArrayList<Object>  getListInActivePods() {
        return listInActivePods;
    }

    public void setListInActivePods(ArrayList<Object> listInActivePods) {
        this.listInActivePods = listInActivePods;
    }

    public ArrayList<ArrayList<String>>  getListHcResp() {
        return listHcResp;
    }

    public void setListHcResp(ArrayList<ArrayList<String>>  listHcResp) {
        this.listHcResp = listHcResp;
    }

    public String getOverallst() {
        return overallst;
    }

    public void setOverallst(String overallst) {
        this.overallst = overallst;
    }

    public String getPg() {
        return pg;
    }

    public void setPg(String pg) {
        this.pg = pg;
    }

    public String getNst() {
        return nst;
    }

    public void setNst(String nst) {
        this.nst = nst;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public Integer getCorchy() {
        return corchy;
    }

    public void setCorchy(Integer corchy) {
        this.corchy = corchy;
    }

    public Integer getCk8() {
        return ck8;
    }

    public void setCk8(Integer ck8) {
        this.ck8 = ck8;
    }

    public boolean isAutokill() {
        return autokill;
    }

    public void setAutokill(boolean autokill) {
        this.autokill = autokill;
    }

    public boolean isSendemail() {
        return sendemail;
    }

    public void setSendemail(boolean sendemail) {
        this.sendemail = sendemail;
    }
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public HashMap<String, Object> getRefreshAtDiff() {
        return refreshAtDiff;
    }

    public void setRefreshAtDiff(HashMap<String, Object> refreshAtDiff) {
        this.refreshAtDiff = refreshAtDiff;
    }
    @Override
    public String toString() {
        return "IppHealthCheck{" +
                "listActivePods='" + listActivePods + '\'' +
                ", listInActivePods='" + listInActivePods + '\'' +
                ", listHcResp='" + listHcResp + '\'' +
                ", overallst='" + overallst + '\'' +
                ", pg='" + pg + '\'' +
                ", nst='" + nst + '\'' +
                ", dst='" + dst + '\'' +
                ", corchy=" + corchy +
                ", ck8=" + ck8 +
                ", autokill=" + autokill +
                ", sendemail=" + sendemail +
                ", timestamp=" + timestamp +
                ", refreshAtDiff=" + refreshAtDiff +
                '}';
    }
}
