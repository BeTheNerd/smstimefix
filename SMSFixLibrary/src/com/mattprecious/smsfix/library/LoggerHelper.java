package com.mattprecious.smsfix.library;

import java.io.File;
import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.appender.LogCatAppender;
import com.google.code.microlog4android.config.PropertyConfigurator;
import com.google.code.microlog4android.format.PatternFormatter;

public class LoggerHelper {
    private static final String INTENT_LOG_ROLLOVER = "com.mattprecious.smsfix.library.INTENT_LOG_ROLLOVER";
    
    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String LOG_FILE = "/Android/data/com.mattprecious.smsfix/files/smsfix.log";
    private static final String LOG_PATTERN = "%d{ISO8601}-[%P]-%m";
    private static final String ROLLOVER_SUFFIX = ".old";
    
    private static final long MAX_LOG_SIZE = 1 * 1024 * 1024;   // 1MB
    
    private static LoggerHelper instance;

    private Logger logger;
    
    private LoggerHelper(Context context) {
        logger = createLogger(context);
    }
    
    public static LoggerHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LoggerHelper(context);
            checkForRollover(context);
        }
        
        return instance;
    }
    
    private Logger createLogger(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean logToSd = settings.getBoolean("log_to_sd", false);
        String storageState = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            File logFile = new File(ROOT_DIR + LOG_FILE);
            if (!logFile.exists() && logToSd) {
                try {
                    logFile.getParentFile().mkdirs();
                    logFile.createNewFile();
                } catch (IOException e) {
                    logger.error("Could not create file: " + logFile.getAbsolutePath());
                }
            }
        }

        PropertyConfigurator.getConfigurator(context).configure();
        
        logger = LoggerFactory.getLogger();

        // these next steps of the initialization can be removed when a
        // newer version of microlog4android is built
        PatternFormatter formatter = new PatternFormatter();
        formatter.setPattern(LOG_PATTERN);

        FileAppender fileAppender = new FileAppender();
        fileAppender.setFileName(LOG_FILE);
        fileAppender.setFormatter(formatter);

        LogCatAppender logCatAppender = new LogCatAppender();
        logCatAppender.setFormatter(formatter);

        logger.removeAllAppenders();
        logger.addAppender(logCatAppender);
        
        if (logToSd) {
            logger.addAppender(fileAppender);
        }

        logger.info("Logger has been initialized");
        logger.info("Android release: " + Build.VERSION.RELEASE);
        logger.info("Android codename: " + Build.VERSION.CODENAME);
        logger.info("Android incremental" + Build.VERSION.INCREMENTAL);
        logger.info("Android SDK: " + Build.VERSION.SDK_INT);

        return logger;
    }
    
    public void reset(Context context) {
        close();
        logger = createLogger(context);
    }
    
    public void close() {
        try {
            logger.close();
        } catch (IOException e) {
            
        }
    }
    
    public void debug(String message) {
        logger.debug(message);
    }
    
    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }
    
    public void error(String message) {
        logger.error(message);
    }
    
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }
    
    public void fatal(String message) {
        logger.fatal(message);
    }
    
    public void fatal(String message, Throwable t) {
        logger.fatal(message, t);
    }
    
    public void info(String message) {
        logger.info(message);
    }
    
    public void info(String message, Throwable t) {
        logger.info(message, t);
    }
    
    public void warn(String message) {
        logger.warn(message);
    }
    
    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    public void userNote(String note) {
        logger.info("[USERNOTE]-" + note);
    }

    public void clearLog(Context context) {
        File logFile = new File(ROOT_DIR + LOG_FILE);
        File rolloverLogFile = new File(ROOT_DIR + LOG_FILE + ROLLOVER_SUFFIX);
        
        if (logFile.exists()) {
            try {
                if (logger != null) {
                    logger.close();
                }
                
                logFile.delete();
                logger = createLogger(context);
            } catch (IOException e) {
                logger.error("Could not recreate file: " + logFile.getAbsolutePath());
            }
        }
        
        if (rolloverLogFile.exists()) {
            rolloverLogFile.delete();
        }
    }
    
    public static void checkForRollover(Context context) {
        File logFile = new File(ROOT_DIR + LOG_FILE);
        
        if (instance != null) {
            if (logFile.length() > MAX_LOG_SIZE) {
                instance.rollover(context);
            }
            
            AlarmManager alartManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(INTENT_LOG_ROLLOVER);
            PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            
            long nextRunTime = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY;
            
            alartManager.set(AlarmManager.RTC_WAKEUP, nextRunTime, sender);
        }
    }
    
    private void rollover(Context context) {
        try {
            if (logger != null) {
                logger.close();
            }
            
            File logFile = new File(ROOT_DIR + LOG_FILE);
            File rolloverFile = new File(ROOT_DIR + LOG_FILE + ROLLOVER_SUFFIX);
            
            if (rolloverFile.exists()) {
                rolloverFile.delete();
            }
            
            logFile.renameTo(rolloverFile);
        
            logFile.createNewFile();
            
            logger = createLogger(context);
        } catch(IOException e) {
            logger.error("Could not rollover log file");
        }
    }
}
