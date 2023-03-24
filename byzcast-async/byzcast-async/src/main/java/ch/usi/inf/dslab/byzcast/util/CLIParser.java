package ch.usi.inf.dslab.byzcast.util;

import org.apache.commons.cli.*;

public class CLIParser {
    private Option configPath, globalConfig, logPath, localConfigs, id, group,
            globalPercent, clientCount, duration, msgSize, nonGenuine, singleRequest, rootConfigsPath, msgDestination, lcaConfigPath, executeFunction;
    private Options options;
    private CommandLineParser parser;
    private CommandLine line;
    private String command;
    private Option lcaId;


    private CLIParser() {
        id = Option.builder("i").desc("server id").argName("id").hasArg().numberOfArgs(1).type(Integer.class).build();
        group = Option.builder("g").desc("group id").argName("id").hasArg().numberOfArgs(1).type(Integer.class).build();
        globalPercent = Option.builder("p").desc("percentage of global messages (defaults to 0)").argName("percentage").hasArg().numberOfArgs(1).type(Integer.class).build();
        duration = Option.builder("d").desc("time to execute in seconds (defaults to 120)").argName("seconds").hasArg().numberOfArgs(1).type(Integer.class).build();
        msgSize = Option.builder("s").desc("message size in bytes (defaults to 64)").argName("bytes").hasArg().numberOfArgs(1).type(Integer.class).build();
        msgDestination = Option.builder("md").desc("group(s) destination(s) of the message").argName("group1 ... groupN").hasArg().numberOfArgs(Option.UNLIMITED_VALUES).type(String[].class).build();
        lcaId = Option.builder("lcaId").desc("lca group id").argName("lcaId").hasArg().numberOfArgs(1).type(Integer.class).build();
        lcaConfigPath = Option.builder("lca").desc("lca config folder").argName("folder").hasArg().numberOfArgs(1).type(String.class).build();
        clientCount = Option.builder("c").desc("number of client threads (defaults to 1)").argName("clients").hasArg().numberOfArgs(1).type(Integer.class).build();
        configPath = Option.builder("cp").desc("group config folder").argName("folder").hasArg().numberOfArgs(1).type(String.class).build();
        globalConfig = Option.builder("gc").desc("global group config folder").argName("folder").hasArg().numberOfArgs(1).type(String.class).build();
        localConfigs = Option.builder("lcs").desc("2+ local group config folders").argName("folder1 ... folderN").hasArg().numberOfArgs(Option.UNLIMITED_VALUES).type(String[].class).build();
        nonGenuine = Option.builder("ng").desc("sets the server to the non-genuine configuration").hasArg(false).build();
        executeFunction = Option.builder("fn").desc("sets the server apply a function in responses (defaults to false = concatenado)").hasArg(false).build();
        singleRequest = Option.builder("sr").desc("sets the client to send a single request p/thread").hasArg(false).build();
        rootConfigsPath = Option.builder("rp").desc("root configuration folder").argName("folder").hasArg().numberOfArgs(1).type(String.class).build();
        options = new Options();
        parser = new DefaultParser();
        line = null;
    }

    public static CLIParser getClientParser(String[] args) {
        CLIParser parser = new CLIParser();
        parser.command = "Client Role";
        parser.id.setRequired(true);
        parser.lcaId.setRequired(true);
        parser.lcaConfigPath.setRequired(true);
        parser.msgDestination.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.clientCount);
        parser.options.addOption(parser.duration);
        parser.options.addOption(parser.msgSize);
        parser.options.addOption(parser.lcaId);
        parser.options.addOption(parser.lcaConfigPath);
        parser.options.addOption(parser.msgDestination);
        parser.options.addOption(parser.singleRequest);
        parser.parse(args);
        return parser;
    }

    public static CLIParser getReplicaParser(String[] args) {
        CLIParser parser = new CLIParser();
        parser.command = "Replica options.";
        parser.id.setRequired(true);
        parser.group.setRequired(true);
        parser.rootConfigsPath.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.group);
        parser.options.addOption(parser.nonGenuine);
        parser.options.addOption(parser.rootConfigsPath);
        parser.options.addOption(parser.executeFunction);
        parser.parse(args);
        return parser;
    }


    public int getId() {
        return Integer.parseInt(line.getOptionValue("i"));
    }

    public int getGroup() {
        return Integer.parseInt(line.getOptionValue("g"));
    }
    public int getLcaGroupId() {
        return Integer.parseInt(line.getOptionValue("lcaId"));
    }

    public int getGlobalPercent() {
        String v = line.getOptionValue("p");
        return v == null ? 0 : Integer.parseInt(v);
    }

    public int getClientCount() {
        String v = line.getOptionValue("c");
        return v == null ? 1 : Integer.parseInt(v);
    }

    public int getDuration() {
        String v = line.getOptionValue("d");
        return v == null ? 120 : Integer.parseInt(v);
    }

    public int getMsgSize() {
        String v = line.getOptionValue("s");
        return v == null ? 64 : Integer.parseInt(v);
    }
    
    public String getGlobalConfig() {
        return line.getOptionValue("gc");
    }

    public String getLcaConfig() {
        return line.getOptionValue("lca");
    }

    public String getConfigPath() {
        return line.getOptionValue("cp");
    }

    public String getRootConfigsPath() {
        return line.getOptionValue("rp");
    }

    public String[] getLocalConfigs() {
        return line.getOptionValues("lcs");
    }

    public boolean isNonGenuine() {
        return line.hasOption("ng");
    }
    public boolean executeFunction() {
        return line.hasOption("fn");
    }

    public boolean isSingleRequest() {
        return line.hasOption("sr");
    }

    private void parse(String args[]) {
        try {
            // parse the command line arguments
            line = parser.parse(options, args, false);
        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(command, options);
            System.err.println("Fail to parse command line options.  Reason: " + exp.getMessage());
            System.exit(-1);
        }
    }

    public String[] getMsgDestination() {
        return line.getOptionValues("md");
    }
}
