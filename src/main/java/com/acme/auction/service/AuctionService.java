package com.acme.auction.service;

import com.acme.auction.model.AuctionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * his class contains all the logic to interact with the ethereum client.
 */
@Service
public class AuctionService {

    @Autowired
    Admin web3j;

    static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);

    private static final int SLEEP_DURATION = 15000;
    private static final int ATTEMPTS = 40;
    public static String contractAddress =null;

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    /**
     * Deploy auction contract.
     * Only one instance of contract can be deployed per jvm instance
     *
     * @param request
     * @return
     * @throws Exception
     */
    public String deploy(AuctionRequest request) throws Exception {

        log.info("Deploy contract for:"+request.getAddress());

        if(contractAddress!=null){
            return contractAddress;
        }

        boolean accountUnlocked = unlockAccount(request.getAddress(), request.getPassword());
        if(accountUnlocked){

            String transactionHash  = createContract(request.getAddress());

            TransactionReceipt transactionReceipt =
                    waitForTransactionReceipt(transactionHash);

            contractAddress = transactionReceipt.getContractAddress();

            log.info("Contract deployed at:"+contractAddress);

        }

        return contractAddress;

    }

    /**
     * Initialise auction
     * Auctions are mapped by their name and are active for the time specified in this request.
     *
     * @param request
     * @return
     * @throws Exception
     */
    public String startAuction(AuctionRequest request) throws Exception {

        log.info("Start auction:"+request.getAuctionName()+" for:"+request.getAddress());
        String transactionHash = null;

        boolean accountUnlocked = unlockAccount(request.getAddress(), request.getPassword());
        if(accountUnlocked){


            Function function = new Function(
                    "startAuction",
                    Arrays.<Type>asList(new Utf8String(request.getAuctionName()), new Uint256(request.getAuctionTime())),
                    Collections.<TypeReference<?>>emptyList());

            transactionHash = callSmartContractFunction(function, request.getAddress(), contractAddress);
            log.info("Start auction transaction:"+transactionHash);

        }

        return transactionHash;

    }

    /**
     * Send a bid request.
     * The key used in this request will be used to end auction if bidder wins.
     *
     * @param request
     * @return
     * @throws Exception
     */
    public String bid(AuctionRequest request) throws Exception {

        log.info(request.getAddress()+ " bid:"+request.getBid());
        String transactionHash = null;

        boolean accountUnlocked = unlockAccount(request.getAddress(), request.getPassword());
        if(accountUnlocked){

            Function function = new Function(
                    "bid",
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(request.getAuctionName()),
                            new org.web3j.abi.datatypes.Utf8String(request.getKey())),Collections.<TypeReference<?>>emptyList());

            transactionHash =  callSmartContractFunction(function, request.getAddress(), contractAddress,
                    Convert.toWei(request.getBid().toString(), org.web3j.utils.Convert.Unit.ETHER).toBigInteger());
            log.info("bid transaction:"+transactionHash);

        }

        return transactionHash;

    }

    /**
     * End auction.
     * Auction can only be ended if time to live has expired and correct key is sent
     * @param request
     * @return
     * @throws Exception
     */
    public String endAuction(AuctionRequest request) throws Exception {

        log.info(request.getAddress()+ " attempted to end auction");
        String transactionHash = null;

        boolean accountUnlocked = unlockAccount(request.getAddress(), request.getPassword());
        if(accountUnlocked){


            Function function = new Function(
                    "endAuction",
                    Arrays.<Type>asList(new Utf8String(request.getAuctionName()), new Utf8String(request.getKey())),
                    Collections.<TypeReference<?>>emptyList());

            transactionHash = callSmartContractFunction(function, request.getAddress(), contractAddress, BigInteger.ZERO);
            log.info("end auction transaction:"+transactionHash);

        }

        return transactionHash;

    }


    private String callSmartContractFunction(
            Function function, String address, String contractAddress) throws Exception {

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger nonce = getNonce(address);

        Transaction transaction = Transaction.createFunctionCallTransaction(address, nonce,
                GAS_PRICE, GAS_LIMIT, contractAddress, encodedFunction);

        org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =
                web3j.ethSendTransaction(transaction).send();

        String transactionHash = transactionResponse.getTransactionHash();
        return transactionHash;

    }

    private String callSmartContractFunction(
            Function function, String address, String contractAddress, BigInteger value) throws Exception {

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger nonce = getNonce(address);

        Transaction transaction = Transaction.createFunctionCallTransaction(address, nonce,
                GAS_PRICE, GAS_LIMIT, contractAddress, value, encodedFunction);

        org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =
                web3j.ethSendTransaction(transaction).send();

        String transactionHash = transactionResponse.getTransactionHash();
        return transactionHash;

    }

    private boolean unlockAccount(String address, String password) throws Exception {
        PersonalUnlockAccount personalUnlockAccount =
                web3j.personalUnlockAccount(address, password)
                        .sendAsync().get();
        return personalUnlockAccount.accountUnlocked();
    }

    private String createContract(String address) throws Exception {
        BigInteger nonce = getNonce(address);

        Transaction transaction = Transaction.createContractTransaction(
                address,
                nonce,
                GAS_PRICE,
                GAS_LIMIT,
                BigInteger.ZERO,
                getAuctionSolidityBinary());

        org.web3j.protocol.core.methods.response.EthSendTransaction
                transactionResponse = web3j.ethSendTransaction(transaction)
                .sendAsync().get();

        return transactionResponse.getTransactionHash();
    }


    BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).sendAsync().get();

        return ethGetTransactionCount.getTransactionCount();
    }

    static String getAuctionSolidityBinary() throws Exception {
        return load("src/main/resources/solidity/build/Auction.bin");
    }

    static String load(String filePath) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/solidity/build/Auction.bin"));
        return new String(bytes);
    }

    TransactionReceipt waitForTransactionReceipt(
            String transactionHash) throws Exception {

        Optional<TransactionReceipt> transactionReceiptOptional =
                getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);

        if (!transactionReceiptOptional.isPresent()) {
            log.info("Transaction receipt not generated after " + ATTEMPTS + " attempts");
        }

        return transactionReceiptOptional.get();
    }

    private Optional<TransactionReceipt> getTransactionReceipt(
            String transactionHash, int sleepDuration, int attempts) throws Exception {

        Optional<TransactionReceipt> receiptOptional =
                sendTransactionReceiptRequest(transactionHash);
        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.isPresent()) {
                Thread.sleep(sleepDuration);
                receiptOptional = sendTransactionReceiptRequest(transactionHash);
            } else {
                break;
            }
        }

        return receiptOptional;
    }

    private Optional<TransactionReceipt> sendTransactionReceiptRequest(
            String transactionHash) throws Exception {
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

        return transactionReceipt.getTransactionReceipt();
    }
}
