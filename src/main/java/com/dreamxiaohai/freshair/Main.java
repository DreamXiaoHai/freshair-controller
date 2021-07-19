package com.dreamxiaohai.freshair;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) throws ParseException{
        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help info");
        options.addOption("u", "user", true, "username, phone");
        options.addOption("p", "password", true, "password, encrypt");
        options.addOption("fsn", "fresh-air-sn", true, "fresh air sn");
        options.addOption("psn", "power-sn", true, "power sn");
        options.addOption("pl", "power-limit", true, "power limit");
        options.addOption("fd", "freshair-alive-delay", true, "fresh air switch on delay second when power down");

        CommandLine commandLine = parser.parse(options, args);
        HelpFormatter helpFormatter = new HelpFormatter();
        if(commandLine.hasOption("h")){
            helpFormatter.printHelp("please follow the below usage:", options);
            System.exit(0);
        }
        if(!commandLine.hasOption("u") || !commandLine.hasOption("p") || !commandLine.hasOption("fsn") || !commandLine.hasOption("psn")){
            helpFormatter.printHelp("-u/-p/-fsn/-psn is required, please follow the below usage:", options);
            System.exit(-1);
        }
        String userName = commandLine.getOptionValue("u");
        String password = commandLine.getOptionValue("p");
        String freshAirSN = commandLine.getOptionValue("fsn");
        String powserSN = commandLine.getOptionValue("psn");
        Integer powerLimit = null;
        Integer freshAirAliveDelay = null;
        String powerLimitString = commandLine.getOptionValue("pl");
        if (null != powerLimitString){
            powerLimit = Integer.parseInt(powerLimitString);
        }
        String freshAirAliveDelayString = commandLine.getOptionValue("fd");
        if (null != freshAirAliveDelayString){
            freshAirAliveDelay = Integer.parseInt(freshAirAliveDelayString);
        }
        new Daemon(userName, password, freshAirSN, powserSN, powerLimit, freshAirAliveDelay).daemon();
    }
}
