package com.dreamxiaohai.freshair;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Daemon {
    private int powerLimit = 30;
    private int freshAirAliveDelay = 1 * 60; //默认当电源功率下降后一分钟恢复新风开关

    private String userName;
    private String password;
    private String freshAirSN;
    private String powerSN;
    private IkeService ikeService;
    private boolean running;
    private int tokenFreshFailedTimes = 0;
    private TokenInfo token;

    private static final DateFormat df =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Daemon(String userName, String password, String freshAirSN, String powerSN, Integer powerLimit, Integer freshAirAliveDelay){
        this.userName = userName;
        this.password = password;
        this.freshAirSN = freshAirSN;
        this.powerSN = powerSN;
        this.ikeService = IkeService.getInstance();
        this.running = true;
        if(null != powerLimit){
            this.powerLimit = powerLimit;
        }
        if (null != freshAirAliveDelay){
            this.freshAirAliveDelay = freshAirAliveDelay;
        }
    }

    public void daemon(){
        this.token = this.ikeService.getToken(userName, password);
        if (null == this.token){
            System.out.println("First get token failed, please check userName and password.");
            System.exit(1);
        }
        String freshAirPassword = "";
        try {
            freshAirPassword = this.ikeService.getDevicePassword(this.freshAirSN, this.token);
            if (null == password){
                System.out.println("Can't get freshAir password, please check you have freshAir right.");
                System.exit(2);
            }
        } catch (TokenExpireException e) {
            e.printStackTrace();
            System.exit(2);
        }
        int runLogCount = 0;
        int continuousFreshTokenTimes = 0;

        printLog("Begin to daemon fresh air...");
        while(this.running){
            try {
                int power = this.ikeService.getPower(this.powerSN, this.token);
                if (power > this.powerLimit){
                    if (ikeService.isFreshAirOn(this.freshAirSN, this.token)){
                        printLog(String.format("Notice: power > %d, fresh air close util power down.", this.powerLimit));
//                        ikeService.fakeDownPower();
                        freshAirPassword = this.ikeService.getDevicePassword(this.freshAirSN, this.token);
                        ikeService.setFreshAirSwitch(false, this.freshAirSN, this.token, freshAirPassword);
                        this.waitUtilPowerDown();
                        freshAirPassword = this.ikeService.getDevicePassword(this.freshAirSN, this.token);
                        printLog(String.format("Notice: power down a few time, fresh air will switch on."));
                        ikeService.setFreshAirSwitch(true, this.freshAirSN, this.token, freshAirPassword);
                    }
                }
                continuousFreshTokenTimes = 0;
                if (runLogCount++ > 720){
                    //一小时打印一次
                    printLog("Daemon still running fresh air...");
                    runLogCount = 0;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (TokenExpireException e) {
                if(continuousFreshTokenTimes++ > 10){
                    printLog(String.format("Fresh token still failed %s times. please check.", continuousFreshTokenTimes));
                }
                this.freshToken();
            }
        }
    }

    private void freshToken(){
        this.token = this.ikeService.getToken(userName, password);
        if (null == token){
            System.out.println("fresh token failed, please check");
            if(this.tokenFreshFailedTimes++ > 3){
                System.out.println("warning------------------------, fresh token failed too many times, will sleep 5 min.");
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.tokenFreshFailedTimes = 0;
            }
        }else{
            this.tokenFreshFailedTimes = 0;
        }
    }

    private void waitUtilPowerDown(){
        long beginPowerDownTimestamp = -1;
        while(this.running){
            try {
                int power = this.ikeService.getPower(this.powerSN, this.token);
                if (power > this.powerLimit){
                    beginPowerDownTimestamp = -1;
                }else{
                    if (beginPowerDownTimestamp != -1){
                        if (System.currentTimeMillis() - beginPowerDownTimestamp > this.freshAirAliveDelay * 1000L){
                            break;
                        }
                    }else{
                        beginPowerDownTimestamp = System.currentTimeMillis();
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }catch (TokenExpireException e) {
                this.freshToken();
            }
        }
    }

    public void stop(){
        this.running = false;
    }


    private void printLog(String msg){
        System.out.println(df.format(new Date()) + " " +  msg);
    }
}
