package com.cigc.limit.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by sofn
 * 2018/5/14 15:08
 */
public class FileUtils {
    private static Log logger = LogFactory.getLog(FileUtils.class);

    public static void write(String file, String conent){
        File fileDir = new File(file).getParentFile();
        if (!fileDir.exists()){
            fileDir.mkdirs();
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true)));
            out.write(conent + "\r\n");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
            }
        }
    }
}
