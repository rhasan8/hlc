package com.optum.ipp.kubeclient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.komamitsu.fluency.Fluency;
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd;
import org.springframework.beans.factory.annotation.Value;

public class FluentLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        private static String FLUENTD_HOST="ipp-fluentd";
        private Fluency fluentLogger;
        private String label = "HealthChecker";

        @Value("${spring.ipp.fluentd.host}")
        public void setFluentdHost (String fluentdHost) {
            FluentLogbackAppender.FLUENTD_HOST = fluentdHost;
        }
        @Override
        public void start() {
            super.start();
            this.fluentLogger = new FluencyBuilderForFluentd().build(FLUENTD_HOST, 24224);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("HealthChecker:", "Starting k8s-HealthChecker");
            try {
                fluentLogger.emit(label, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void append(ILoggingEvent rawData) {
            String msg = rawData.toString();
            Map<String, Object> data = new HashMap<String, Object>(1);
            data.put("HealthChecker:", msg);
            try {
                fluentLogger.emit(label, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void stop() {
            super.stop();
            try {
                fluentLogger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
