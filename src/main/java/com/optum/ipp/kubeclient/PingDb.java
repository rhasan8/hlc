package com.optum.ipp.kubeclient;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
@Component
@Slf4j
public class PingDb {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static String DB_HOST;
    @Value("${spring.db.host}")
    public void setPgDbhostStatic(String pgdbhost){
        PingDb.DB_HOST = pgdbhost;
    }
    private static String DATABASE;
    @Value("${spring.db.database}")
    public void setDatabaseStatic(String database){
        PingDb.DATABASE = database;
    }
    private static String USER;
    @Value("${spring.db.user}")
    public void setUserStatic(String user){
        PingDb.USER = user;
    }
    private static String PWD;
    @Value("${spring.db.pwd}")
    public void setPwdStatic(String pwd){ PingDb.PWD = pwd; }
    public String pingdatabase() {
        String DbStatus="";
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
            .getConnection("jdbc:postgresql://"+DB_HOST+":5432/"+ DATABASE+"?targetServerType=primary", USER, PWD);
            if (c != null) {
                DbStatus="ok";
                logger.debug("Connecting to database successfully.");
                try {
                    c.close();
                    DbStatus="ok";
                    logger.debug("Closing database connection successfully.");
                } catch (Exception e) {
                    DbStatus="Error closing database connection.";
                    e.printStackTrace();
                    logger.error(e.getClass().getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            DbStatus="Error Connecting to databse.";
            logger.error(e.getClass().getName() + ": " + e.getMessage());
        }
        return DbStatus;
    }
}
