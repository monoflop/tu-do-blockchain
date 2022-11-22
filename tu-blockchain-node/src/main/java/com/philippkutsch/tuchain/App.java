package com.philippkutsch.tuchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.philippkutsch.tuchain.chain.*;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.PingMessage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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
        parser.addArgument("-g", "--genesis")
                .action(Arguments.storeTrue())
                .help("Generate genesis block only");
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
            logger.error("Failed to load rsa keys", e);
            return;
        }

        //Load blockchain
        File blockchainFile = new File(workingDirectory, config.getBlockchainFilePath());
        Blockchain blockchain = null;
        if(blockchainFile.exists()) {
            try {
                String blockchainJson = Files.readString(blockchainFile.toPath());
                blockchain = ChainUtils.decodeFromString(blockchainJson, Blockchain.class);
            }
            catch (IOException | JsonParseException e) {
                logger.error("Failed to read blockchain from file", e);
                return;
            }
        }
        else {
            if(!namespace.getBoolean("genesis")) {
                throw new IllegalStateException("No blockchain found. A blockchain with an genesis block is required");
            }
        }

        //Generate genesis block and save to blockchain
        if (namespace.getBoolean("genesis")) {
            logger.info("Generating genesis block");
            try {
                //Generate
                ListeningExecutorService service = MoreExecutors.
                        listeningDecorator(Executors.newCachedThreadPool());

                ListenableFuture<HashedBlock> hashedBlockFuture = service.submit(new Miner(
                        new Block(1, new byte[]{}, new BlockBody(new SignedTransaction[]{})),
                        new HashPerformanceAnalyser()
                ));
                HashedBlock hashedBlock = hashedBlockFuture.get();
                logger.info("Generated genesis block " + ChainUtils.encodeToString(hashedBlock));

                //Export to blockchain
                List<HashedBlock> blockList = new ArrayList<>();
                blockList.add(hashedBlock);
                Blockchain exportChain = new Blockchain(blockList);
                Files.writeString(blockchainFile.toPath(), ChainUtils.encodeToString(exportChain));

                //Shutdown
                service.shutdown();
            }
            catch (IOException | InterruptedException | ExecutionException e) {
                logger.error("Failed to generate / save genesis block", e);
            }
            return;
        }

        //Build and start node
        logger.info("Starting network node " + config.getName() +" on port " + config.getPort());
        Node node;
        try {
            //noinspection ConstantConditions
            node = new Node(config, rsaKeys, blockchain);
        }
        catch (IOException e) {
            logger.error("Failed to create node", e);
            return;
        }

        //Listen for input
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        logger.info("Waiting for input (exit, nodes, broadcast, save):");
        try {
            for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {
                String[] input = cmd.split(" ");

                if("exit".equals(input[0])) {
                    break;
                }
                else if("nodes".equals(input[0])) {
                    logger.info("Connected nodes:");
                    //Get node list
                    List<RemoteNode> connectedNodeList = node.getNetwork().getConnectedNodes();
                    for(RemoteNode connectedNode : connectedNodeList) {
                        logger.info("Node '" + connectedNode.getConnectedNode().getName() + "' " + connectedNode.getConnectedNode().getHost()
                                + ":" + connectedNode.getConnectedNode().getPort());
                    }
                }
                else if("broadcast".equals(input[0])) {
                    node.getNetwork().broadcast(new PingMessage().encode());
                }
                else if("save".equals(input[0])) {
                    try {
                        Files.writeString(blockchainFile.toPath(), ChainUtils.encodeToString(node.blockchain));
                        logger.info("Chain saved!");
                    }
                    catch (IOException e) {
                        logger.error("Failed to save blockchain", e);
                    }
                }
                else {
                    logger.warn("Unknown command '" + input[0] + "'");
                }
            }
            reader.close();
        }
        catch (IOException e) {
            logger.error("Waiting for input failed", e);
        }

        try {
            node.shutdown();
        }
        catch (IOException e) {
            logger.error("Shutdown failed", e);
        }
    }
}
