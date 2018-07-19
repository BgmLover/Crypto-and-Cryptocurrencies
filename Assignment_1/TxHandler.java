import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
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
    private boolean isValidTx(Transaction tx) {
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
    public Transaction[] handleTxs(Transaction[] possibleTransaction) {
        // IMPLEMENT THIS
        ArrayList<Transaction> valid=new ArrayList<Transaction>();
        for (Transaction tx:possibleTransaction
             ) {
            if(!isValidTx(tx)){
                continue;
            }
            valid.add(tx);
            updatePool(tx);
        }
        Transaction[] result=new Transaction[valid.size()];
        result=valid.toArray(result);
        return result;
}

}
