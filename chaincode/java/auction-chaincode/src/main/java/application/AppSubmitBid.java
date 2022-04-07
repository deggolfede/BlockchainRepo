package application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import chaincode.Auction;
import com.owlike.genson.Genson;

import org.hyperledger.fabric.gateway.*;

public class AppSubmitBid {

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
        String bidID = args[3];

        // connect to the network and invoke the smart contract
        try (Gateway gateway = connect(org, user)) {

            // get the network and contract
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("auction-contract");


            /**
             * Qua devo mettercelo il setEndorsingPeer o è diverso dal setEndorsingOrganizations?
             */

            System.out.println("Submit Transaction: add bids to the auction.");
            contract.submitTransaction("submitBid", auctionID, bidID);
            System.out.println("\n");

            System.out.println("Evaluate Transaction: query the auction that was just created");
            String auctionJSON = contract.evaluateTransaction("queryAuction", auctionID).toString();
            System.out.println("*** Result: Auction: " + auctionJSON);
            System.out.println("\n");

        }
        catch(Exception e){
            System.err.println(e);
        }

    }

}
