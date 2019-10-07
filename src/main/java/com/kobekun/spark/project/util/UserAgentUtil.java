package com.kobekun.spark.project.util;

import com.kobekun.spark.project.domain.UserAgentInfo;
import cz.mallat.uasparser.OnlineUpdater;
import cz.mallat.uasparser.UASparser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class UserAgentUtil {

    public static  UASparser parser = null;

    static {
        try {
            parser = new UASparser(OnlineUpdater.getVendoredInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将官方的UserAgentInfo转换成自己需要的
     * @param ua
     * @return
     */
    public static UserAgentInfo getUserAgentInfo(String ua){

        UserAgentInfo info = null;

        try {
            if(StringUtils.isNotEmpty(ua)){

                cz.mallat.uasparser.UserAgentInfo tmp = parser.parse(ua);

                if(null != tmp){

                    info = new UserAgentInfo();
                    info.setBrowserName(tmp.getUaFamily());
                    info.setBrowserVersion(tmp.getBrowserVersionInfo());
                    info.setOsName(tmp.getOsFamily());
                    info.setOsVersion(tmp.getOsName());
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return info;
    }
    public static void main(String[] args) throws Exception {


//        cz.mallat.uasparser.UserAgentInfo info = parser.parse("Mozilla/4.0 (compatible; MSIE 7.0;Windows NT 5.1; ");
//        System.out.println(info);

        System.out.println(UserAgentUtil.getUserAgentInfo(
                "Mozilla/4.0 (compatible; MSIE 7.0;Windows NT 5.1; "));
    }
}
