package com.philippkutsch.tuchain.contract;

import com.philippkutsch.tuchain.chain.*;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContractVmTest {
    @Test
    public void findSendingPubKey_shouldFindKey_existingTransaction() {
        final byte[] publicKey = "PubKey".getBytes(StandardCharsets.UTF_8);

        //Fund transaction
        Transaction first = new Transaction(System.currentTimeMillis(), new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(200, publicKey)
        });
        //Contract transaction
        Transaction second = new Transaction(System.currentTimeMillis(), new Transaction.SignedInput[]{
                new Transaction.SignedInput(first.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(200, new byte[0])
        });

        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0], new BlockBody(new Transaction[]{first}, new Contract[0]), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(0, new byte[0], new BlockBody(new Transaction[]{second}, new Contract[0]), 0, 0, new byte[0]));
        Blockchain blockchain = new Blockchain(initialBlockchain);

        byte[] foundPublicKey = ContractVm.findSendingPubKey(blockchain, second);
        assert Arrays.equals(foundPublicKey, publicKey);
    }

    @Test(expected = RuntimeException.class)
    public void findSendingPubKey_shouldNotFindKey_nonExistingTransaction() {
        //Contract transaction
        Transaction second = new Transaction(System.currentTimeMillis(), new Transaction.SignedInput[]{
                new Transaction.SignedInput(new byte[0], 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(200, new byte[0])
        });

        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0], new BlockBody(new Transaction[]{second}, new Contract[0]), 0, 0, new byte[0]));
        Blockchain blockchain = new Blockchain(initialBlockchain);

        ContractVm.findSendingPubKey(blockchain, second);
    }

    @Test
    public void run_deposite_shouldReturnNothing_insideDeadline() throws ContractException {
        long contractTimestamp = System.currentTimeMillis();

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        200,
                        new byte[0],
                        "Test",
                        "Test"), new byte[0]);

        Transaction transaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(200, new byte[0])
        });
        Blockchain blockchain = new Blockchain();
        assert ContractVm.run(contract, transaction, 0, blockchain).size() == 0;
    }

    @Test
    public void run_deposite_shouldReturnTransaction_outsideDeadline() throws ContractException {
        final long contractTimestamp = System.currentTimeMillis();
        final long transactionTimestamp = contractTimestamp + 1000;
        final byte[] targetPublicKey = "PubKey".getBytes(StandardCharsets.UTF_8);
        final int investmentAmount = 200;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        200,
                        new byte[0],
                        "Test",
                        "Test"), new byte[0]);

        //Source transaction is accessed to get target public key
        Transaction sourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investmentAmount, targetPublicKey)
        });

        //Deposit transaction
        Transaction transaction = new Transaction(transactionTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(sourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investmentAmount, contract.getContractId())
        });

        //Other state
        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0], new BlockBody(new Transaction[]{sourceTransaction}, new Contract[0]), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(0, new byte[0], new BlockBody(new Transaction[]{transaction}, new Contract[0]), 0, 0, new byte[0]));
        Blockchain blockchain = new Blockchain(initialBlockchain);

        //Test
        List<Transaction> contractOutput = ContractVm.run(contract, transaction, 0, blockchain);
        assert contractOutput.size() == 1;

        Transaction outputTransaction = contractOutput.get(0);
        assert outputTransaction.getInputs().length == 1;
        assert outputTransaction.getInputs().length == 1;

        Transaction.SignedInput input = outputTransaction.getInputs()[0];
        assert Arrays.equals(input.getTxId(), transaction.getTransactionId());
        assert input.getvOut() == 0;
        assert Arrays.equals(input.getSignature(), new byte[0]);

        Transaction.Output output = outputTransaction.getOutputs()[0];
        assert Arrays.equals(output.getPubKey(), targetPublicKey);
        assert output.getAmount() == investmentAmount;
    }

    @Test(expected = ProjectRunningException.class)
    public void run_withdraw_shouldThrowProjectRunningException_insideDeadline() throws ContractException {
        final long contractTimestamp = System.currentTimeMillis();
        final long transactionTimestamp = contractTimestamp - 1000;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        200,
                        new byte[0],
                        "Test",
                        "Test"), new byte[0]);

        //Deposit transaction
        Transaction transaction = new Transaction(transactionTimestamp, new Transaction.SignedInput[0], new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });

        Blockchain blockchain = new Blockchain();
        ContractVm.run(contract, transaction, 0, blockchain);
    }

    @Test
    public void run_withdraw_shouldReturnOwnerTransaction() throws ContractException {
        final byte[] ownerPubKey = "PubKeyOwner".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor1 = "PubKeyInvestor1".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor2 = "PubKeyInvestor2".getBytes(StandardCharsets.UTF_8);
        final int investorOneInvestmentAmount = 200;
        final int investorTwoInvestmentAmount = 500;
        final int contractTarget = 400;
        final long contractTimestamp = System.currentTimeMillis();
        final long investmentTimestamp = contractTimestamp - 1000;
        final long withdrawTimestamp = contractTimestamp + 1000;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        contractTarget,
                        ownerPubKey,
                        "Test",
                        "Test"), new byte[0]);

        //Valid Investments
        Transaction investorOneInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)
        });
        Transaction investorTwoInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, publicKeyInvestor2)
        });
        Transaction investorOneInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, contract.getContractId())
        });
        Transaction investorTwoInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorTwoInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, contract.getContractId())
        });

        //Investment after expiry
        Transaction investorOneLateInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)});
        Transaction investorOneLateInvestment = new Transaction(contractTimestamp + 1000, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneLateInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(100, contract.getContractId())
        });

        //Withdraw owner transaction
        Transaction withdrawSourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(0, ownerPubKey)
        });

        //Blockchain
        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestmentFund, investorTwoInvestmentFund, withdrawSourceTransaction, investorOneLateInvestmentFund}, new Contract[]{contract}), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(1, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestment, investorTwoInvestment, investorOneLateInvestment}, new Contract[0]), 0, 0, new byte[0]));

        Blockchain blockchain = new Blockchain(initialBlockchain);

        //Execute withdraw
        Transaction withdrawTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(withdrawSourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });

        List<Transaction> contractTransactions = ContractVm.run(contract, withdrawTransaction, 0, blockchain);
        assert contractTransactions.size() == 1;
        Transaction out = contractTransactions.get(0);
        assert out.getInputs().length == 2;
        assert out.getOutputs().length == 1;
        assert out.getOutputs()[0].getAmount() == investorOneInvestmentAmount + investorTwoInvestmentAmount;
        assert Arrays.equals(out.getOutputs()[0].getPubKey(), ownerPubKey);
    }

    @Test(expected = NotAInvestorException.class)
    public void run_withdraw_shouldThrowNotAInvestorException_failedProjectNoOwnerReturn() throws ContractException {
        final byte[] ownerPubKey = "PubKeyOwner".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor1 = "PubKeyInvestor1".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor2 = "PubKeyInvestor2".getBytes(StandardCharsets.UTF_8);
        final int investorOneInvestmentAmount = 200;
        final int investorTwoInvestmentAmount = 500;
        final int contractTarget = 1000;
        final long contractTimestamp = System.currentTimeMillis();
        final long investmentTimestamp = contractTimestamp - 1000;
        final long withdrawTimestamp = contractTimestamp + 1000;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        contractTarget,
                        ownerPubKey,
                        "Test",
                        "Test"), new byte[0]);

        //Valid Investments
        Transaction investorOneInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)
        });
        Transaction investorTwoInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, publicKeyInvestor2)
        });
        Transaction investorOneInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, contract.getContractId())
        });
        Transaction investorTwoInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorTwoInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, contract.getContractId())
        });

        //Investment after expiry
        Transaction investorOneLateInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)});
        Transaction investorOneLateInvestment = new Transaction(contractTimestamp + 1000, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneLateInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(100, contract.getContractId())
        });

        //Withdraw owner transaction
        Transaction withdrawSourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(0, ownerPubKey)
        });

        //Blockchain
        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestmentFund, investorTwoInvestmentFund, withdrawSourceTransaction, investorOneLateInvestmentFund}, new Contract[]{contract}), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(1, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestment, investorTwoInvestment, investorOneLateInvestment}, new Contract[0]), 0, 0, new byte[0]));

        Blockchain blockchain = new Blockchain(initialBlockchain);

        //Execute withdraw
        Transaction withdrawTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(withdrawSourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });

        List<Transaction> contractTransactions = ContractVm.run(contract, withdrawTransaction, 0, blockchain);
        assert contractTransactions.size() == 0;
    }

    @Test
    public void run_withdraw_shouldReturnInvestorTransaction() throws ContractException {
        final byte[] ownerPubKey = "PubKeyOwner".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor1 = "PubKeyInvestor1".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor2 = "PubKeyInvestor2".getBytes(StandardCharsets.UTF_8);
        final int investorOneInvestmentAmount = 200;
        final int investorTwoInvestmentAmount = 500;
        final int contractTarget = 1000;
        final long contractTimestamp = System.currentTimeMillis();
        final long investmentTimestamp = contractTimestamp - 1000;
        final long withdrawTimestamp = contractTimestamp + 1000;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        contractTarget,
                        ownerPubKey,
                        "Test",
                        "Test"), new byte[0]);

        //Valid Investments
        Transaction investorOneInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)
        });
        Transaction investorTwoInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, publicKeyInvestor2)
        });
        Transaction investorOneInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, contract.getContractId())
        });
        Transaction investorTwoInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorTwoInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, contract.getContractId())
        });

        //Investment after expiry
        Transaction investorOneLateInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)});
        Transaction investorOneLateInvestment = new Transaction(contractTimestamp + 1000, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneLateInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(100, contract.getContractId())
        });

        //Withdraw investor transaction
        Transaction withdrawSourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(0, publicKeyInvestor1)
        });

        //Blockchain
        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestmentFund, investorTwoInvestmentFund, withdrawSourceTransaction, investorOneLateInvestmentFund}, new Contract[]{contract}), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(1, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestment, investorTwoInvestment, investorOneLateInvestment}, new Contract[0]), 0, 0, new byte[0]));

        Blockchain blockchain = new Blockchain(initialBlockchain);

        //Execute withdraw
        Transaction withdrawTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(withdrawSourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });

        List<Transaction> contractTransactions = ContractVm.run(contract, withdrawTransaction, 0, blockchain);
        assert contractTransactions.size() == 1;
        Transaction out = contractTransactions.get(0);
        assert out.getInputs().length == 1;
        assert out.getOutputs().length == 1;
        assert out.getOutputs()[0].getAmount() == investorOneInvestmentAmount;
        assert Arrays.equals(out.getOutputs()[0].getPubKey(), publicKeyInvestor1);
    }

    @Test(expected = AlreadyWithdrawnException.class)
    public void run_withdraw_shouldThrowAlreadyWithdrawnException_investor() throws ContractException {
        final byte[] ownerPubKey = "PubKeyOwner".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor1 = "PubKeyInvestor1".getBytes(StandardCharsets.UTF_8);
        final byte[] publicKeyInvestor2 = "PubKeyInvestor2".getBytes(StandardCharsets.UTF_8);
        final int investorOneInvestmentAmount = 200;
        final int investorTwoInvestmentAmount = 500;
        final int contractTarget = 1000;
        final long contractTimestamp = System.currentTimeMillis();
        final long investmentTimestamp = contractTimestamp - 1000;
        final long withdrawTimestamp = contractTimestamp + 1000;

        Contract contract = new Contract(
                new SignAbleContract(
                        contractTimestamp,
                        contractTimestamp,
                        contractTarget,
                        ownerPubKey,
                        "Test",
                        "Test"), new byte[0]);

        //Valid Investments
        Transaction investorOneInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)
        });
        Transaction investorTwoInvestmentFund = new Transaction(investmentTimestamp - 1000, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, publicKeyInvestor2)
        });
        Transaction investorOneInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorOneInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, contract.getContractId())
        });
        Transaction investorTwoInvestment = new Transaction(investmentTimestamp, new Transaction.SignedInput[]{
                new Transaction.SignedInput(investorTwoInvestmentFund.getTransactionId(), 0, new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorTwoInvestmentAmount, contract.getContractId())
        });

        //Withdraw investor transaction
        Transaction withdrawSourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(0, publicKeyInvestor1)
        });
        Transaction withdrawTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(withdrawSourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });
        Transaction contractTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(investorOneInvestment.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(investorOneInvestmentAmount, publicKeyInvestor1)
        });

        //Second withdraw
        Transaction secondWithdrawSourceTransaction = new Transaction(contractTimestamp, new Transaction.SignedInput[]{}, new Transaction.Output[]{
                new Transaction.Output(0, publicKeyInvestor1)
        });
        Transaction secondWithdrawTransaction = new Transaction(withdrawTimestamp, new Transaction.SignedInput[]{
                new Transaction.Input(secondWithdrawSourceTransaction.getTransactionId(), 0).toSignedInput(new byte[0])
        }, new Transaction.Output[]{
                new Transaction.Output(0, contract.getContractId())
        });

        //Blockchain
        List<HashedBlock> initialBlockchain = new ArrayList<>();
        initialBlockchain.add(new HashedBlock(0, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestmentFund, investorTwoInvestmentFund, withdrawSourceTransaction}, new Contract[]{contract}), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(1, new byte[0],
                new BlockBody(new Transaction[]{investorOneInvestment, investorTwoInvestment}, new Contract[0]), 0, 0, new byte[0]));
        initialBlockchain.add(new HashedBlock(2, new byte[0],
                new BlockBody(new Transaction[]{withdrawTransaction, contractTransaction}, new Contract[0]), 0, 0, new byte[0]));
        Blockchain blockchain = new Blockchain(initialBlockchain);

        //Execute withdraw again
        ContractVm.run(contract, secondWithdrawTransaction, 0, blockchain);
    }
}
