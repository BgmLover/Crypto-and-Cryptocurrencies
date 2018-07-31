import java.util.ArrayList;
import java.util.HashMap;


public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private TransactionPool transactionPool;
    private HashMap<ByteArrayWrapper, TreeNode> treeNodeHashMap;
    private TreeNode maxHeightBlockNode;

    private class TreeNode {
        Block block;
        ArrayList<TreeNode> children;
        UTXOPool utxoPool;
        int height;

        /**
         * Add a new Block to the BlockChain. This also updates the global
         * TreeNode Map.
         * @param block
         * @param utxoPool Unclaimed tx outputs, init'd to the block's coinbase outputs
         */
        public TreeNode(Block block, UTXOPool utxoPool) {
            this.block = block;
            this.children = new ArrayList<TreeNode>();
            this.utxoPool = utxoPool;

            // Genesis block
            if (block.getPrevBlockHash() == null) {
                this.height = 1;
                maxHeightBlockNode = this;
            }
            else {
                TreeNode parent = treeNodeHashMap.get(block.getPrevBlockHash());
                this.height = parent.height + 1;
                parent.children.add(this);
            }
        }

        public UTXOPool getUTXOPool() {
            return utxoPool;
        }
    }

    private void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();
        Transaction.Output out = coinbase.getOutput(0);
        UTXO utxo = new UTXO(coinbase.getHash(), 0);
        utxoPool.addUTXO(utxo, out);
    }


    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        transactionPool = new TransactionPool();
        treeNodeHashMap = new HashMap<ByteArrayWrapper, TreeNode>();

        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(genesisBlock, utxoPool);

        TreeNode newNode = new TreeNode(genesisBlock, utxoPool);
        treeNodeHashMap.put(new ByteArrayWrapper(genesisBlock.getHash()), newNode);
        maxHeightBlockNode = newNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightBlockNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return new UTXOPool(maxHeightBlockNode.getUTXOPool());
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added; false if it is not or if it is a 'genesis'
     * block (presents a null hash).
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if (block.getHash() == null)
            return false;

        if (block.getPrevBlockHash() == null)
            return false;

        TreeNode parentNode = treeNodeHashMap.get(block.getPrevBlockHash());
        if (parentNode == null)
            return false;

        if (!(parentNode.height + 1 > maxHeightBlockNode.height - CUT_OFF_AGE))
            return false;

        TxHandler txHandler = new TxHandler(new UTXOPool(parentNode.getUTXOPool()));

        ArrayList<Transaction> blockTxs = block.getTransactions();

        Transaction[] validTxs = txHandler.handleTxs(blockTxs.toArray(new Transaction[blockTxs.size()]));
        if (validTxs.length != blockTxs.size())
            return false;

        UTXOPool utxoPool = txHandler.getUTXOPool();
        addCoinbaseToUTXOPool(block, utxoPool);
        TreeNode newNode = new TreeNode(block, utxoPool);
        treeNodeHashMap.put(new ByteArrayWrapper(block.getHash()), newNode);

        if (newNode.height > maxHeightBlockNode.height)
            maxHeightBlockNode = newNode;
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }
}