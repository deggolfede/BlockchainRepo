package application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.gateway.*;

import chaincode.BasketballPlayer;
import chaincode.Auction;

public class AppCreateAuction {
    // helper function for getting connected to the gateway
    public static Gateway connect(final String org, final String user) throws Exception{

        // Load a file system based wallet for managing identities.
        Path walletPath = Paths.get(String.format("wallet/%s", org));
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..", "..", "test-network", "organizations", "peerOrganizations", String.format("%s.example.com",org), String.format("connection-%s.yaml", org));
        System.out.println("OK CCP loaded");
        System.out.println(networkConfigPath);
        System.out.println(walletPath);

        Gateway.Builder builder = Gateway.createBuilder();
        System.out.println("OK Builder created!!");
        builder.identity(wallet, user).networkConfig(networkConfigPath).discovery(true);
        System.out.println("OK identity found!!");
        return builder.connect();
    }

    public static void main(String[] args) throws Exception {

        String org = args[0].toLowerCase();
        String user = args[1];
        String auctionID = args[2];
        String playerName = args[3];
        String playerRole = args[4];

        // connect to the network and invoke the smart contract
        try (Gateway gateway = connect(org, user)) {

            // get the network and contract
            System.out.println("Get the network channel...");
            Network network = gateway.getNetwork("mychannel");
            System.out.println("OK");
            System.out.println("Get the contract...");
            Contract contract = network.getContract("auction-chaincode");
            System.out.println("OK");
            System.out.println(contract);

            BasketballPlayer player = new BasketballPlayer("player1", playerName, playerRole, "", 0);

            System.out.println("Submit Transaction: Propose new Auction for a new player.");
            contract.submitTransaction("createAuction", auctionID, player.getAssetID(), player.getName(), player.getRole());
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
