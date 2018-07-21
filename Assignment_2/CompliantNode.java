import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class CompliantNode implements Node {

    private final Double graph;
    private final Double malicious;
    private final Double txDistribution;
    private final Integer numRounds;
    private Set<Transaction> pendingTransactions;
    private boolean[] followees;
    private HashMap<Integer,Integer>lastNum;
    private Set<Integer> blackList;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.graph = p_graph;
        this.malicious = p_malicious;
        this.txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.lastNum=new HashMap<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        this.blackList = new HashSet<>(followees.length);
        for(int i=0;i<followees.length;i++){
            if(followees[i]){
                lastNum.put(i,0);
            }
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return new HashSet<>(pendingTransactions);
    }

    public void receiveFromFollowees(final Set<Candidate> candidates) {
        final Set<Integer> senders = candidates.stream().map(candidate -> candidate.sender).collect(toSet());
        for (int i = 0; i < this.followees.length; i++) {
            if (this.followees[i] && !senders.contains(i))
                this.blackList.add(i);
        }
        HashMap<Integer,Integer> current=new HashMap<>();
        for(Candidate c:candidates){
            if(!current.containsKey(c.sender)){
                current.put(c.sender,0);
            }
            current.put(c.sender,current.get(c.sender)+1);
        }
        for(int i:current.keySet()){
            if(lastNum.get(i)>current.get(i)){
                this.blackList.add(i);
                continue;
            }
            else {
                lastNum.put(i,current.get(i));
            }
        }

        this.pendingTransactions = candidates.stream()
                .filter(candidate -> !this.blackList.contains(candidate.sender))
                .map(candidate -> candidate.tx)
                .collect(toSet());
    }
}
