package com.philippkutsch.tuchain.chain;

import com.philippkutsch.tuchain.RsaKeys;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class SignedTransaction extends Transaction {
    protected final byte[] signature;

    public SignedTransaction(
            long id,
            long timestamp,
            @Nonnull byte[] pubKey,
            @Nonnull Target[] targets,
            @Nonnull byte[] signature) {
        super(id, timestamp, pubKey, targets);
        this.signature = signature;
    }

    @Nonnull
    public static SignedTransaction fromTransaction(
            @Nonnull Transaction transaction,
            @Nonnull byte[] signature) {
        return new SignedTransaction(
                transaction.id,
                transaction.timestamp,
                transaction.pubKey,
                transaction.targets,
                signature);
    }

    @Nonnull
    public Transaction toTransaction() {
        return new Transaction(id, timestamp, pubKey, targets);
    }

    public byte[] getSignature() {
        return signature;
    }

    public boolean isValid() throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, SignatureException {
        //Coinbase
        byte[] targetPubKey;
        if(id == 0) {
            //Coinbase transaction is required to have 'coinbase' as public key and one target
            //with 100 coins. The target public key is the key that verifies the signature.
            if(!Arrays.equals("coinbase".getBytes(StandardCharsets.UTF_8), pubKey)) {
                return false;
            }

            if(targets.length != 1) {
                return false;
            }

            Target target = targets[0];
            if(target.getAmount() != 100) {
                return false;
            }
            targetPubKey = target.getPubKey();
        }
        //Normal transaction
        else {
            targetPubKey = pubKey;
            //TODO check utxo, overspending, etc.
        }



        //Validate signature
        Transaction targetTransaction = toTransaction();
        RsaKeys rsaKeys = new RsaKeys(targetPubKey, null);
        return rsaKeys.verifyData(ChainUtils.encodeToBytes(targetTransaction), signature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SignedTransaction that = (SignedTransaction) o;
        return Arrays.equals(getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(getSignature());
        return result;
    }
}
