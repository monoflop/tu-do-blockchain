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
import com.philippkutsch.tuchain.modules.BlockchainSyncModule;
import com.philippkutsch.tuchain.modules.PingModule;
import com.philippkutsch.tuchain.modules.mining.HashPerformanceAnalyser;
import com.philippkutsch.tuchain.modules.mining.Miner;
import com.philippkutsch.tuchain.network.RemoteNode;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

//TODO GSON serialization allows null fields and has no really reliable serialization / deserialization order...
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

        Subparsers subparsers = parser.addSubparsers();
        subparsers.addParser("node")
                .help("Start full network node")
                .addArgument("node")
                .action(Arguments.storeTrue());

        subparsers.addParser("gen-wallet")
                .help("Generate a new wallet file")
                .addArgument("-o", "--out")
                .dest("walletOut")
                .type(Arguments.fileType())
                .setDefault("myWallet.wallet")
                .help("Target wallet file");

        subparsers.addParser("gen-genesis")
                .help("Generate a new genesis block and save it to a new chain")
                .addArgument("-o", "--out")
                .dest("genesisOut")
                .type(Arguments.fileType())
                .setDefault("blockchain.json")
                .help("Target blockchain file");

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

        //Generate wallet
        if(namespace.getString("walletOut") != null) {
            File targetWalletFile = new File(namespace.getString("directory"), namespace.getString("walletOut"));
            logger.info("Generating new wallet: " + targetWalletFile.getPath());

            try {
                //Generate wallet
                Wallet newWallet = Wallet.generate();

                //Save wallet
                newWallet.save(targetWalletFile);

                logger.info("Wallet generated successfully!");
            }
            catch (IOException | InterruptedException |
                    NoSuchAlgorithmException | InvalidKeySpecException e) {
                logger.error("Failed to generate wallet", e);
                return;
            }
            return;
        }

        //Generate genesis block and save to blockchain
        if (namespace.getString("genesisOut") != null) {
            File targetBlockchain = new File(namespace.getString("directory"), namespace.getString("genesisOut"));
            logger.info("Generating genesis block: " + targetBlockchain.getPath());

            logger.info("");
            try {
                //Generate
                ListeningExecutorService service = MoreExecutors.
                        listeningDecorator(Executors.newCachedThreadPool());

                ListenableFuture<HashedBlock> hashedBlockFuture = service.submit(new Miner(
                        24,0,
                        Block.generateGenesisBlock(),
                        new HashPerformanceAnalyser()
                ));
                HashedBlock hashedBlock = hashedBlockFuture.get();
                logger.info("Generated genesis block " + ChainUtils.encodeToString(hashedBlock));

                //Export to blockchain
                List<HashedBlock> blockList = new ArrayList<>();
                blockList.add(hashedBlock);
                Blockchain exportChain = new Blockchain(blockList);
                Files.writeString(targetBlockchain.toPath(), ChainUtils.encodeToString(exportChain));

                //Shutdown
                service.shutdown();
            }
            catch (IOException | InterruptedException | ExecutionException e) {
                logger.error("Failed to generate / save genesis block", e);
            }
            return;
        }

        //Try to load config files
        String workingDirectoryPath = namespace.getString("directory");
        File workingDirectory = new File(workingDirectoryPath);
        if(!workingDirectory.exists()) {
            throw new IllegalArgumentException("Working directory not found");
        }

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

        //Load wallet and get rsa keys
        RsaKeys rsaKeys;
        try {
            Wallet wallet = Wallet.load(new File(workingDirectory, config.getWalletFilePath()));
            rsaKeys = new RsaKeys(wallet.getPublicKey(), wallet.getPrivateKey());
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

        //Build and start node
        logger.info("Starting network node " + config.getName() +" on port " + config.getPort());
        TestingNode testingNode;
        try {
            //noinspection ConstantConditions
            testingNode = new TestingNode(config, rsaKeys, blockchain);
        }
        catch (IOException e) {
            logger.error("Failed to create node", e);
            return;
        }

        //Listen for input
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));

        //TODO: figure out how to use arg parser
        logger.info("""
                Waiting for input:
                exit                   Shutdown node and exit
                nodes                  List all connected nodes
                connect [ip] [port]    Try to connect to target node
                ping                   Ping all connected nodes
                save                   Save blockchain to disk
                blockchain             View blockchain
                minerkey               Show miner public key
                balance-key [pubKey]   Get UTXO of pubKey
                balance [wallet]       Get UTXO of wallet
                transactions [pubKey]  Get all transactions that public key is involved in
                tx [txId] [vOut] [amount] [target pubKey] [wallet] Create a transaction
                """);

        try {
            for (String cmd = reader.readLine(); cmd != null; cmd = reader.readLine()) {
                String[] input = cmd.split(" ");
                if("exit".equals(input[0])) {
                    break;
                }
                else if("nodes".equals(input[0])) {
                    logger.info("Connected nodes:");
                    //Get node list
                    List<RemoteNode> connectedNodeList = testingNode.getNetwork().getConnectedNodes();
                    for(RemoteNode connectedNode : connectedNodeList) {
                        logger.info("Node '" + connectedNode.getConnectedNode().getName() + "' " + connectedNode.getConnectedNode().getHost()
                                + ":" + connectedNode.getConnectedNode().getPort());
                    }
                }
                else if("connect".equals(input[0])) {
                    //TODO add connect
                }
                else if("ping".equals(input[0])) {
                    PingModule pingModule = testingNode.requireModule(PingModule.class);
                    List<RemoteNode> connectedNodes = testingNode.getNetwork().getConnectedNodes();

                    if(connectedNodes.isEmpty()) {
                        logger.info("No nodes connected");
                        continue;
                    }

                    logger.info("Pinging all connected nodes");
                    for(RemoteNode remoteNode : connectedNodes) {
                        boolean response = pingModule.pingNode(remoteNode);
                        if(response) {
                            logger.info("[.OK.] Node " + remoteNode.getKey());
                        }
                        else {
                            logger.info("[FAIL] Node " + remoteNode.getKey());
                        }
                    }
                }
                else if("blockchain".equals(input[0])) {
                    List<HashedBlock> blockList = testingNode.getBlockchain().getBlockchain();
                    logger.info("Blockchain: " + blockList.size() + " blocks long");
                }
                else if("save".equals(input[0])) {
                    try {
                        Files.writeString(blockchainFile.toPath(), ChainUtils.encodeToString(testingNode.blockchain));
                        logger.info("Chain saved!");
                    }
                    catch (IOException e) {
                        logger.error("Failed to save blockchain", e);
                    }
                }
                else if("minerkey".equals(input[0])) {
                    logger.info("Public:\n" + ChainUtils.bytesToBase64(rsaKeys.getPublicKeyBytes())
                            + "\n\nPrivate:\n" + ChainUtils.bytesToBase64(rsaKeys.getPrivateKeyBytes()));
                }
                else if("transactions".equals(input[0])) {
                    if (input.length != 2) {
                        logger.info("Usage: balance [pubKey]");
                        continue;
                    }

                    byte[] pubKey = ChainUtils.bytesFromBase64(input[1]);
                    try {
                        new RsaKeys(pubKey, null);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        logger.error("Invalid public key", e);
                        continue;
                    }


                    //TODO Find all transactions
                }
                else if("balance-key".equals(input[0])) {
                    if(input.length != 2) {
                        logger.info("Usage: balance [pubKey]");
                        continue;
                    }

                    RsaKeys pubKey;
                    try {
                        pubKey = new RsaKeys(ChainUtils.bytesFromBase64(input[1]), null);
                    }
                    catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        logger.error("Invalid public key", e);
                        continue;
                    }

                    List<UnspentTransactionOutput> uTXOList =
                            blockchain.findUTXO(pubKey.getPublicKeyBytes());

                    logger.info("Available balance");

                    int sum = 0;
                    for(UnspentTransactionOutput uTXO : uTXOList) {
                        logger.info(uTXO.toString());
                        sum += uTXO.getAmount();
                    }
                    logger.info("Total available amount: " + sum);
                }
                else if("balance".equals(input[0])) {
                    if(input.length != 2) {
                        logger.info("Usage: balance [pubKey]");
                        continue;
                    }

                    RsaKeys pubKey;
                    try {
                        Wallet wallet = Wallet.load(new File(workingDirectory, input[1]));
                        pubKey = new RsaKeys(wallet.getPublicKey(), null);
                    }
                    catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                        logger.error("Invalid public key / wallet", e);
                        continue;
                    }

                    List<UnspentTransactionOutput> uTXOList =
                            blockchain.findUTXO(pubKey.getPublicKeyBytes());

                    logger.info("Available balance");

                    int sum = 0;
                    for(UnspentTransactionOutput uTXO : uTXOList) {
                        logger.info(uTXO.toString());
                        sum += uTXO.getAmount();
                    }
                    logger.info("Total available amount: " + sum);
                }
                else if("tx".equals(input[0])) {
                    if(input.length != 6) {
                        logger.info("Usage: tx [txId] [vOut] [amount] [target pubKey] [wallet]");
                        continue;
                    }

                    //Arguments
                    byte[] txId = ChainUtils.bytesFromBase64(input[1]);
                    int vOut = Integer.parseInt(input[2]);
                    int amount = Integer.parseInt(input[3]);
                    byte[] targetPubKey = ChainUtils.bytesFromBase64(input[4]);

                    RsaKeys keyPair;
                    try {
                        new RsaKeys(targetPubKey, null);
                        Wallet wallet = Wallet.load(new File(workingDirectory, input[5]));
                        keyPair = new RsaKeys(wallet.getPublicKey(), wallet.getPrivateKey());
                    }
                    catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                        logger.error("Invalid keys", e);
                        continue;
                    }

                    //Search input transaction
                    Optional<Transaction> targetTransactionOptional = testingNode.getBlockchain().findTransaction(txId);
                    if(targetTransactionOptional.isEmpty()) {
                        logger.error("Transaction " + input[1] + " not found");
                        continue;
                    }
                    Transaction targetTransaction = targetTransactionOptional.get();

                    //Get output
                    if(vOut > targetTransaction.getOutputs().length - 1) {
                        logger.error("Transaction target vOut " + input[2] +" not found");
                        continue;
                    }

                    int availableAmount = targetTransaction.getOutputs()[vOut].getAmount();
                    if(availableAmount < amount) {
                        logger.error("Transaction has not enough funds " + input[3]);
                        continue;
                    }

                    //Create transaction
                    Transaction.Input transactionInput = new Transaction.Input(txId, vOut);
                    Transaction.Input[] inputs = {transactionInput};
                    Transaction.Output transactionOutput = new Transaction.Output(amount, targetPubKey);
                    Transaction.Output[] outputs = {transactionOutput};
                    if(availableAmount > amount) {
                        //Add transaction back to our self
                        outputs = new Transaction.Output[]{transactionOutput, new Transaction.Output(availableAmount-amount, keyPair.getPublicKeyBytes())};
                    }

                    SignAbleTransaction signAbleTransaction = new SignAbleTransaction(System.currentTimeMillis(), inputs, outputs);
                    byte[] rawTransactionBytes = ChainUtils.encodeToBytes(signAbleTransaction);

                    byte[] signature;
                    try {
                        signature = keyPair.signData(rawTransactionBytes);
                    }
                    catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                        logger.error("Failed to sign transaction", e);
                        continue;
                    }

                    Transaction.SignedInput signedInput = transactionInput.toSignedInput(signature);
                    Transaction.SignedInput[] signedInputs = { signedInput };
                    Transaction transaction = new Transaction(signAbleTransaction.getTimestamp(), signedInputs, outputs);

                    //Submit
                    BlockchainSyncModule syncModule = testingNode.getModule(BlockchainSyncModule.class);
                    if(syncModule == null) {
                        logger.error("Module not found");
                        continue;
                    }

                    boolean accepted = syncModule.addNewTransaction(transaction);
                    if(accepted) {
                        logger.info("Transaction " + ChainUtils.bytesToBase64(transaction.getTransactionId()) + " accepted");
                        logger.info(ChainUtils.encodeToString(transaction));
                    }
                    else {
                        logger.error("Transaction rejected");
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
            testingNode.shutdown();
        }
        catch (IOException e) {
            logger.error("Shutdown failed", e);
        }
    }
}
