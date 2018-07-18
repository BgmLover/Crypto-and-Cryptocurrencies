import java.lang.reflect.Array;
import java.util.*;

public class MaxFeeTxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool=new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        HashSet<UTXO> usedUTXO = new HashSet<UTXO>();
        double inputValue=0;
        double outputValue=0;
        int index=0;
        for (Transaction.Input in:tx.getInputs()){
            UTXO utxo = new UTXO(in.prevTxHash,in.outputIndex);
            if(!this.utxoPool.contains(utxo)){
                return false;
            }
            if(usedUTXO.contains(utxo)){
                return false;
            }
            usedUTXO.add(utxo);
            if(!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address,tx.getRawDataToSign(index),in.signature)){
                return false;
            }
            inputValue+=utxoPool.getTxOutput(utxo).value;
            index++;
        }
        for(Transaction.Output out:tx.getOutputs()){
            if(out.value<0){
                return false;                       //value must >=0
            }
            outputValue+=out.value;
        }
        if(outputValue>inputValue){
            return false;                          //input>=output
        }
        return true;
    }

    private void updatePool(Transaction tx){
        for(Transaction.Input in:tx.getInputs()){
            UTXO utxo = new UTXO(in.prevTxHash,in.outputIndex);
            this.utxoPool.removeUTXO(utxo);
        }
        byte[] txHash = tx.getHash();
        for(int index=0;index<tx.getOutputs().size();index++){
            UTXO utxo = new UTXO(txHash,index);
            this.utxoPool.addUTXO(utxo,tx.getOutput(index));
        }
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<TransactionWithFee> acceptedTx = new ArrayList<TransactionWithFee>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                TransactionWithFee txWithFee = new TransactionWithFee(tx);
                acceptedTx.add(txWithFee);
                removeConsumedCoinsFromPool(tx);
                addCreatedCoinsToPool(tx);
            }
        }

        Collections.sort(acceptedTx);
        Transaction[] result = new Transaction[acceptedTx.size()];
        for (int i = 0; i < acceptedTx.size(); i++) {
            result[i] = acceptedTx.get(acceptedTx.size() - i - 1).tx;
        }

        return result;
    }

    class TransactionWithFee implements Comparable<TransactionWithFee> {
        public Transaction tx;
        private double fee;

        public TransactionWithFee(Transaction tx) {
            this.tx = tx;
            this.fee = calcTxFee(tx);
        }

        @Override
        public int compareTo(TransactionWithFee otherTx) {
            double diff = fee - otherTx.fee;
            if (diff > 0) {
                return 1;
            } else if (diff < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private double calcTxFee(Transaction tx) {
        double inputSum = calculateInputSum(tx);
        double outputSum = calculateOutputSum(tx);

        return inputSum - outputSum;
    }

    private double calculateOutputSum(Transaction tx) {
        double outputSum = 0;
        List<Transaction.Output> outputs = tx.getOutputs();
        for (int j = 0; j < outputs.size(); j++) {
            Transaction.Output output = outputs.get(j);
            outputSum += output.value;
        }
        return outputSum;
    }

    private double calculateInputSum(Transaction tx) {
        List<Transaction.Input> inputs = tx.getInputs();
        double inputSum = 0;
        for (int j = 0; j < inputs.size(); j++) {
            Transaction.Input input = inputs.get(j);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output correspondingOutput = utxoPool.getTxOutput(utxo);
            inputSum += correspondingOutput.value;
        }
        return inputSum;
    }

    private void addCreatedCoinsToPool(Transaction tx) {
        List<Transaction.Output> outputs = tx.getOutputs();
        for (int j = 0; j < outputs.size(); j++) {
            Transaction.Output output = outputs.get(j);
            UTXO utxo = new UTXO(tx.getHash(), j);
            utxoPool.addUTXO(utxo, output);
        }
    }

    private void removeConsumedCoinsFromPool(Transaction tx) {
        List<Transaction.Input> inputs = tx.getInputs();
        for (int j = 0; j < inputs.size(); j++) {
            Transaction.Input input = inputs.get(j);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            utxoPool.removeUTXO(utxo);
        }
    }

}
