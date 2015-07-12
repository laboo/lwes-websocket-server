package com.github.laboo.lwes.websocketserver;

/**
 * Created by mlibucha on 6/20/15.
 */
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

// Thanks: http://stackoverflow.com/questions/16910955/programmatically-configure-logback-appender
public class Log {
    private static Logger logger = null;
    public static int DEFAULT_ROLLOVER_MBS = 5;
    public static String DEFAULT_PATTERN = "%d [%thread] %-5level %logger{35} - %msg%n";

    public static synchronized Logger getLogger() {
        return getLogger(DEFAULT_ROLLOVER_MBS, DEFAULT_PATTERN);
    }

    public static synchronized Logger getLogger(int mbs, String pattern) {
        if (logger == null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            RollingFileAppender rfAppender = new RollingFileAppender();
            rfAppender.setContext(loggerContext);
            rfAppender.setFile("lwesws.log");
            FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setParent(rfAppender);
            rollingPolicy.setFileNamePattern("lwesws.%i.log.zip");
            rollingPolicy.start();

            SizeBasedTriggeringPolicy triggeringPolicy = new ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy();
            triggeringPolicy.setMaxFileSize(mbs + "MB");
            triggeringPolicy.start();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern(pattern);
            encoder.start();

            rfAppender.setEncoder(encoder);
            rfAppender.setRollingPolicy(rollingPolicy);
            rfAppender.setTriggeringPolicy(triggeringPolicy);

            rfAppender.start();
            logger = loggerContext.getLogger("Main");
            logger.addAppender(rfAppender);
        }
        return logger;
    }

}