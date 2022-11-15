package com.philippkutsch.tuchain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.philippkutsch.tuchain.blockchain.Blockchain;
import com.philippkutsch.tuchain.blockchain.HashPerformanceAnalyser;
import com.philippkutsch.tuchain.blockchain.HashedBlock;
import com.philippkutsch.tuchain.blockchain.RsaKeys;
import com.philippkutsch.tuchain.config.Config;
import com.philippkutsch.tuchain.config.Node;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.Network;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.user.messages.BalanceMessage;
import com.philippkutsch.tuchain.network.protocol.user.messages.RequestBlockMessage;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class App {
    private static final Logger logger
            = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("--------------------------------------------------------------------");
        System.out.println("TU-Blockchain");
        System.out.println("Blockchain und Smart Contracts | Bachelorarbeit von Philipp Kutsch");
        System.out.println("--------------------------------------------------------------------");

        ArgumentParser parser = ArgumentParsers.newFor("tu-blockchain").build()
                .defaultHelp(true)
                .description("Blockchain and smart contract sample and testing implementation");
        parser.addArgument("mode")
                .required(true)
                .choices("node", "test", "rsa-test")
                .setDefault("test")
                .help("Specify operation mode");
        parser.addArgument("-g", "--genesis")
                .help("Generate a genesis block of the given body");
        parser.addArgument("-c", "--config")
                .help("Configuration file of all network nodes");
        //parser.addArgument("-p", "--public")
        //        .help("Import public rsa key");
        //parser.addArgument("-k", "--key")
        //        .help("Import private rsa key");
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
            if(namespace.getBoolean("verbose")) {
                rootLogger.setLevel(Level.DEBUG);
            }
            else {
                rootLogger.setLevel(Level.INFO);
            }
        }

        String mode = namespace.getString("mode");
        if(mode.equals("node")) {
            logger.info("Running in node mode");

            //Check if we have a config
            String configFilePath = namespace.getString("config");
            if(configFilePath == null) {
                throw new IllegalStateException("Config file is required");
            }

            //Load and parse config
            Path filePath = Path.of(configFilePath);
            Config config;
            try {
                String content = Files.readString(filePath);
                config = new Gson().fromJson(content, Config.class);
                if(config == null) {
                    throw new IllegalStateException("Failed to load config file");
                }
            }
            catch (IOException | JsonParseException e) {
                e.printStackTrace();
                return;
            }



            //Check if we have a public and private key
            /*String publicKeyPath = namespace.getString("public");
            String privateKeyPath = namespace.getString("key");
            boolean hasKeys = publicKeyPath != null && privateKeyPath != null;
            RsaKeys rsaKeys = null;
            if(hasKeys) {
                try {
                    rsaKeys = RsaKeys.fromFiles(publicKeyPath, privateKeyPath);
                }
                catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }*/

            //Create network
            Network network = new Network(config);

            //RemoteNode testingNode1 = network.registerNode(UUID.randomUUID().toString(), rsaKeys);
            //RemoteNode testingNode2 = network.registerNode(rsaKeys);

            //Register strg-c hook
            Runtime.getRuntime().addShutdownHook(new Thread(network::shutdown));

            //network.sendUserMessage(testingNode1.getNodeId(),
            //        new PublishTransactionMessage("1", "2", 100, "signature").build());

            //Read commands from cmd
            //Commands enable communication with emulated network
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));
            logger.info("Waiting for input (exit, balance, block, transaction):");
            try {
                for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {
                    String[] input = cmd.split(" ");


                    if("exit".equals(input[0])) {
                        break;
                    }
                    //Get balance of keypair
                    /*else if("balance".equals(input[0])) {
                        if(!hasKeys) {
                            logger.warn("No key pair provided");
                            continue;
                        }

                        network.sendUserMessage(testingNode1.getNodeId(),
                                new BalanceMessage(Base64.getEncoder().encodeToString(rsaKeys.getPublicKeyBytes())).encode());
                    }
                    else if("block".equals(input[0])) {
                        if(input.length != 2) {
                            logger.warn("Usage: block [id]");
                            continue;
                        }

                        long blockId;
                        try {
                            blockId = Long.parseLong(input[1]);
                        }
                        catch (NumberFormatException e) {
                            logger.warn("Expected number as argument", e);
                            continue;
                        }

                        network.sendUserMessage(testingNode1.getNodeId(),
                                new RequestBlockMessage(blockId).encode());
                    }*/
                    else {
                        logger.warn("Unknown command '" + input[0] + "'");
                    }
                }
                reader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            network.shutdown();

        }
        else if(mode.equals("rsa-test")) {
            //Require public and private key
            String publicKeyPath = namespace.getString("public");
            String privateKeyPath = namespace.getString("key");
            if(publicKeyPath == null || privateKeyPath == null) {
                logger.error("Public and private key required for testing mode");
                return;
            }

            RsaKeys rsaTesting;
            try {
                rsaTesting = RsaKeys.fromFiles(publicKeyPath, privateKeyPath);
            }
            catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                logger.error("Failed to import keys", e);
                return;
            }
            logger.info("Keys imported successfully");

            //Test sign
            try {
                byte[] data = "Hallo welt :)".getBytes(StandardCharsets.UTF_8);
                byte[] signature = rsaTesting.signData(data);
                boolean valid = rsaTesting.verifyData(data, signature);

                logger.info("data " + new String(Base64.getEncoder().encode(data)));
                logger.info("signature " + new String(Base64.getEncoder().encode(signature)));
                logger.info("valid: " + valid);
            }
            catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                logger.error("Signature failed", e);
                return;
            }



            //Read commands from cmd
            //Commands enable communication with emulated network
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));
            logger.info("Waiting for input:");
            try {
                for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {
                    if("exit".equals(cmd)) {
                        break;
                    }
                    else {
                        logger.warn("Unknown command '" + cmd + "'");
                    }
                }
                reader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(mode.equals("test")) {
            logger.info("Running in test mode");

            String genesis = namespace.getString("genesis");
            if(genesis != null) {
                logger.info("Generating genesis block with body: " + genesis);

                Blockchain blockchain = new Blockchain();
                byte[] data = genesis.getBytes(StandardCharsets.UTF_8);

                LocalDateTime startedTime = LocalDateTime.now();
                logger.info("Generating block...");
                CompletableFuture<HashedBlock> genesisFuture = blockchain.generateGenesisBlock(data, new HashPerformanceAnalyser());
                HashedBlock genesisBlock = genesisFuture.get();
                LocalDateTime endTime = LocalDateTime.now();

                logger.info("Generated genesis block in " + startedTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
                logger.info(genesisBlock.toString());
                blockchain.shutdown();
            }
        }
    }
}

