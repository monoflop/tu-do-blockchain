package com.philippkutsch.tuchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.network.RemoteNode;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class App {
    private static final Logger logger
            = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        System.out.println("--------------------------------------------------------------------");
        System.out.println("TU-Blockchain Node");
        System.out.println("Blockchain und Smart Contracts | Bachelorarbeit von Philipp Kutsch");
        System.out.println("--------------------------------------------------------------------");

        ArgumentParser parser = ArgumentParsers.newFor("tu-blockchain-node").build()
                .defaultHelp(true)
                .description("Blockchain and smart contract node sample and testing implementation");
        parser.addArgument("-d", "--directory")
                .setDefault(".")
                .help("Specify working directory");
        parser.addArgument("-v", "--verbose")
                .action(Arguments.storeTrue())
                .help("Log everything");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        //Configure logging
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        if (rootLogger != null) {
            if (namespace.getBoolean("verbose")) {
                rootLogger.setLevel(Level.DEBUG);
            } else {
                rootLogger.setLevel(Level.INFO);
            }
        }

        //Try to load config files
        String workingDirectoryPath = namespace.getString("directory");
        File workingDirectory = new File(workingDirectoryPath);
        if(!workingDirectory.exists()) {
            throw new IllegalArgumentException("Working directory not found");
        }
        //System.setProperty("user.dir", directory.getAbsolutePath());

        File configFile = new File(workingDirectory, "config.json");
        Config config;
        if(configFile.exists()) {
            try {
                String content = Files.readString(configFile.toPath());
                config = new Gson().fromJson(content, Config.class);
            }
            catch (IOException | JsonParseException e) {
                e.printStackTrace();
                return;
            }
        }
        else {
            //config = Config.standard();
            throw new IllegalStateException("No configuration");
        }

        //Load rsa keys
        RsaKeys rsaKeys;
        try {
            rsaKeys = RsaKeys.fromFiles(
                    new File(workingDirectory, config.getPublicKeyFilePath()).getPath(),
                    new File(workingDirectory, config.getPrivateKeyFilePath()).getPath());
        }
        catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        }



        /*int listenPort = namespace.getInt("port");
        String name = UUID.randomUUID().toString();
        logger.info("Starting network node " + name +" on port " + listenPort);

        //TODO read config
        List<Peer> knownPeers = new ArrayList<>();
        //Only add peers if not specified otherwise
        if(!namespace.getBoolean("master")) {
            knownPeers.add(new Peer("localhost", 8000));
        }
        Config config = new Config(name, knownPeers);*/

        logger.info("Starting network node " + config.getName() +" on port " + config.getPort());

        Node node = new Node(config, rsaKeys);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        logger.info("Waiting for input (exit, nodes):");
        try {
            for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {
                String[] input = cmd.split(" ");

                if("exit".equals(input[0])) {
                    break;
                }
                else if("nodes".equals(input[0])) {
                    logger.info("Connected nodes:");
                    //Get node list
                    List<RemoteNode> nodes = node.getNetwork().getNodes();
                    for(RemoteNode remoteNode : nodes) {
                        logger.info("Node '" + remoteNode.getName() + "' " + remoteNode.getConnection().getAddress()
                                + ":" + remoteNode.getConnection().getPort() + " listen: " + remoteNode.getListenPort());
                    }

                    logger.info("Pending nodes:");
                    //Get node list
                    List<RemoteNode> pendingNodeList = node.getNetwork().getPendingNodeList();
                    for(RemoteNode remoteNode : pendingNodeList) {
                        logger.info("Node '" + remoteNode.getName() + "' " + remoteNode.getConnection().getAddress()
                                + ":" + remoteNode.getConnection().getPort() + " listen: " + remoteNode.getListenPort());
                    }
                }
                else {
                    logger.warn("Unknown command '" + input[0] + "'");
                }
            }
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            node.shutdown();
        }
        catch (IOException e) {
            logger.error("Shutdown failed", e);
        }
    }
}
