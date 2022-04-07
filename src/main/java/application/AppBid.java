package application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import chaincode.FullBid;
import org.hyperledger.fabric.gateway.*;
import com.owlike.genson.Genson;

public class AppBid {

    // helper function for getting connected to the gateway
    public static Gateway connect(final String org, final String user) throws Exception{
        // Load a file system based wallet for managing identities.
        Path walletPath = Paths.get(String.format("wallet/%s", org));
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);
        // load a CCP
        Path networkConfigPath = Paths.get("..", "..", "test-network", "organizations", "peerOrganizations", String.format("%s.example.com",org), String.format("connection-%s.yaml", org));

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, user).networkConfig(networkConfigPath).discovery(true);
        return builder.connect();
    }


    public static void main(String[] args) throws Exception {
        String org = args[0].toLowerCase();
        String user = args[1];
        String auctionID = args[2];
        String price = args[3];
        String orgMSP = org + "MSP";


        // connect to the network and invoke the smart contract
        try (Gateway gateway = connect(org, user)) {

            Genson genson = new Genson();

            // get the network and contract
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("auction-contract");

            System.out.println("Evaluate Transaction: get your client ID");
            System.out.println("\n");
            String bidder = new String(contract.evaluateTransaction("getSubmittingClientIdentity"), StandardCharsets.UTF_8);

            String bidData = String.format("{'price':'%s', 'org':'%s', 'bidder':%s'}", price, orgMSP, bidder);
            Transaction t = contract.createTransaction("bid");
            String bidID = t.getTransactionId();
            Map<String, byte[]> transientData = new HashMap<String, byte[]>();
            transientData.put("bid", bidData.getBytes(StandardCharsets.UTF_8));
            t.setTransient(transientData);

            System.out.println("Submit Transaction: Create the bid that is stored in the organization's private data collection");
            System.out.println("\n");
            t.submit(auctionID);

            System.out.println("*** Result ***SAVE THIS VALUE*** BidID: " + bidID);
            System.out.println("\n");

            System.out.println("Evaluate Transaction: read the bid that was just created");
            System.out.println("\n");
            FullBid bid = genson.deserialize(contract.evaluateTransaction("queryBid", auctionID, bidID).toString(), FullBid.class);
            System.out.println("*** Result:  Bid: " + bid.getPrice() + " " + bid.getOrg() + " " + bid.getBidder() + "\n");



        }
        catch(Exception e){
            System.err.println(e);
        }

    }
}
