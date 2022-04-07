package chaincode;

import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;

import com.owlike.genson.Genson;

@Contract(
        name = "auction-chaincode",
        info = @Info(
                title = "Sealed-blind auction",
                description = "The hyperlegendary sealed-blind auction",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))

@Default
public class AuctionContract implements ContractInterface {
    private final Genson genson = new Genson();

    public enum AuctionErrors {
        AUCTION_NOT_FOUND,
        AUCTION_ALREADY_EXISTS,
        CLIENT_NOT_FOUND,
        NO_MATCH_CLIENT_PEER,
        ORG_NOT_FOUND,
        TRANSIENT_NOT_FOUND,
        COLLECTION_NOT_FOUND,
        TRANSIENT_KEY_NOT_FOUND,
        POLICY_NOT_FOUND
    }


    // CreateAuction creates on auction on the public channel. The identity that
    // submits the transaction becomes the seller of the auction
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Auction createAuction(final Context ctx, final String auctionID, final String playerID, final String name, final String role) {
        ChaincodeStub stub = ctx.getStub();
        System.out.println("Chaincode stub obtained !!");

        if (auctionExists(ctx, auctionID)) {
            String errorMessage = String.format("chaincode.Auction %s already exists", auctionID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.AUCTION_ALREADY_EXISTS.toString());
        }
        System.out.println("The Auction doesn't exists, so we can create it!!");

        // get org of submitting client
        String clientID = new String(getSubmittingClientIdentity(ctx), StandardCharsets.UTF_8);
        if (clientID.equals("")) {
            String errorMessage = String.format("Client not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.CLIENT_NOT_FOUND.toString());
        }
        System.out.println(String.format("The client has the following ID : %s", clientID));


        // get org of submitting client
        String clientOrgID = ctx.getClientIdentity().getMSPID();
        if (clientOrgID == null) {
            String errorMessage = String.format("Client Org not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.ORG_NOT_FOUND.toString());
        }
        System.out.println(String.format("The clientOrg has the following ID : %s", clientOrgID));



        // Create auction
        System.out.println("Let's create a new Auction!!");
        HashMap<String, HashBid> bidders = new HashMap<String, HashBid>();
        HashMap<String, FullBid> reveleadBids = new HashMap<String, FullBid>();

        BasketballPlayer player = new BasketballPlayer(playerID, name, role, "", 0);
        Auction auction = new Auction(auctionID, player, clientID, new ArrayList<String>(),
                                      bidders, reveleadBids, 0, "", "open");

        String auctionJSON = genson.serialize(auction);

        // put auction into state
        stub.putStringState(auctionID, auctionJSON);

        /**
        Set the seller of the auction as an endorser
        **/

        setAssetStateBasedEndorsement(ctx, auctionID, clientOrgID);

        return auction;

    }


    // Bid is used to add a user's bid to the auction. The bid is stored in the private
    // data collection on the peer of the bidder's organization. The function returns
    // the transaction ID so that users can identify and query their bid
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String bid(final Context ctx, final String auctionID) {
        ChaincodeStub stub = ctx.getStub();


        Map<String, byte[]> transientMap = stub.getTransient();
        if (transientMap == null) {
            String errorMessage = String.format("Transient not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.TRANSIENT_NOT_FOUND.toString());
        }

        byte[] bidJSON = transientMap.get("bid");
        if (bidJSON == null){
            String errorMessage = String.format("Transient key not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.TRANSIENT_KEY_NOT_FOUND.toString());
        }



        String collection = getCollectionName(ctx);
        if (collection == null) {
            String errorMessage = String.format("Collection not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.COLLECTION_NOT_FOUND.toString());
        }

        // the bidder has to target their peer to store the bid
        if (!verifyClientOrgMatchesPeerOrg(ctx)) {
            String errorMessage = String.format("ClientOrg and PeerOrg do not match!!");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.NO_MATCH_CLIENT_PEER.toString());
        }

        // the transaction ID is used as a unique index for the bid
        String txID = ctx.getStub().getTxId();


        // create a composite key using the transaction ID
        String bidKey = ctx.getStub().createCompositeKey("bidkey", auctionID, txID).toString();


        // put the bid into the organization's implicit data collection
        ctx.getStub().putPrivateData(collection, bidKey, bidJSON);

        return txID;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void submitBid(final Context ctx, final String auctionID, final String txID) {
        ChaincodeStub stub = ctx.getStub();

        // get the MSP ID of the bidder's org
        String clientOrgID = ctx.getClientIdentity().getMSPID();
        if (clientOrgID == null) {
            String errorMessage = String.format("Client Org not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.ORG_NOT_FOUND.toString());
        }

        // get the auction from public state
        Auction auction = genson.deserialize(queryAuction(ctx, auctionID).toString(), Auction.class);
        if (auction == null) {
            String errorMessage = String.format("chaincode Auction not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.AUCTION_NOT_FOUND.toString());

        }

        // the auction needs to be open for users to add their bid
        if (auction.getStatus().equals("open")) {
            System.out.println("Cannot join closed or ended auction!!");
            return;
        }

        // get the implicit collection name of bidder's org
        String collection = getCollectionName(ctx);
        if (collection == null) {
            String errorMessage = String.format("Collection not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.COLLECTION_NOT_FOUND.toString());
        }

        // use the transaction ID passed as a parameter to create composite bid key
        String bidKey = ctx.getStub().createCompositeKey("bidkey", auctionID, txID).toString();

        // get the hash of the bid stored in private data collection
        String bidHash = ctx.getStub().getPrivateDataHash(collection, bidKey).toString();

        // store the hash along with the bidder's organization
        HashBid newHash = new HashBid(bidHash, clientOrgID);
        auction.getHashBids().put(bidKey, newHash);

        // Add the bidding organization to the list of participating organizations if it is not already

        if (!auction.getOrgs().contains(clientOrgID)) {
            auction.getOrgs().add(clientOrgID);

            addAssetStateBasedEndorsement(ctx, auctionID, clientOrgID);
        }

        String newAuctionJSON = genson.serialize(auction);
        stub.putStringState(auctionID, newAuctionJSON);

        return;

    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void revealBid(final Context ctx, final String auctionID, final String txID) {
        ChaincodeStub stub = ctx.getStub();


        Map<String, byte[]> transientMap = stub.getTransient();
        if (transientMap == null) {
            String errorMessage = String.format("Transient not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.TRANSIENT_NOT_FOUND.toString());
        }

        byte[] transientBidJSON = transientMap.get("bid");
        if (transientBidJSON == null) {
            String errorMessage = String.format("Transient key not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.TRANSIENT_KEY_NOT_FOUND.toString());
        }


        String collection = getCollectionName(ctx);
        if (collection == null) {
            String errorMessage = String.format("Collection not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.COLLECTION_NOT_FOUND.toString());
        }

        String bidKey = ctx.getStub().createCompositeKey("bidkey", auctionID, txID).toString();
        byte[] bidHash = ctx.getStub().getPrivateDataHash(collection, bidKey);

        // get the auction from public state
        Auction auction = genson.deserialize(queryAuction(ctx, auctionID).toString(), Auction.class);
        if (auction == null) {
            String errorMessage = String.format("chaincode.Auction not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.AUCTION_NOT_FOUND.toString());

        }

        // Complete a series of three checks before we add the bid to the auction

        // check 1: check that the auction is closed. We cannot reveal a
        // bid to an open auction
        if (!auction.getStatus().equals("closed")) {
            System.out.println("Cannot reveal bid for open or ended auction!!");
            return;
        }


        // check 2: check that hash of revealed bid matches hash of private bid
        // on the public ledger. This checks that the bidder is telling the truth
        // about the value of their bid
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            byte[] calculatedBidJSONHash = hash.digest(transientBidJSON);
            if (!Arrays.equals(calculatedBidJSONHash, bidHash)) {
                System.out.println("The hash of the revealed bid doesn't match hash of private bid\n"
                        + "        // on the public ledger");
                return;
            }
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown for incorrect algorithm: " + e);
        }

        // check 3; check hash of revealed bid matches hash of private bid that was
        // added earlier. This ensures that the bid has not changed since it
        // was added to the auction
        String privateBidHashString = auction.getHashBids().get(bidKey).getHash();
        if (!privateBidHashString.equals(new String(bidHash, StandardCharsets.UTF_8))) {
            System.out.println("The hash of the private bid doesn't match hash in auction");
            return;
        }

        // we can add the FullBid to the auction if all checks have passed
        String clientID = new String(getSubmittingClientIdentity(ctx), StandardCharsets.UTF_8);
        if (clientID.equals("")) {
            String errorMessage = String.format("Client not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.CLIENT_NOT_FOUND.toString());
        }

        FullBid newBid = genson.deserialize(transientBidJSON, FullBid.class);

        // check 4: make sure that the transaction is being submitted is the bidder
        if (!newBid.getBidder().equals(clientID)) {
            System.out.println("Permission denied, client id is not the owner of the bid");
            return;
        }

        auction.getRevealedBids().put(bidKey, newBid);
        String newAuctionJSON = genson.serialize(auction);
        stub.putStringState(auctionID, newAuctionJSON);

        return;
    }


    // CloseAuction can be used by the seller to close the auction. This prevents
    // bids from being added to the auction, and allows users to reveal their bid
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void closeAuction(final Context ctx, final String auctionID) {
        ChaincodeStub stub = ctx.getStub();

        Auction auction = genson.deserialize(queryAuction(ctx, auctionID).toString(), Auction.class);
        if (auction == null) {
            String errorMessage = String.format("chaincode.Auction not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.AUCTION_NOT_FOUND.toString());

        }

        // the auction can only be closed by the seller
        // get ID of submitting client
        String clientID = new String(getSubmittingClientIdentity(ctx), StandardCharsets.UTF_8);
        if (clientID.equals("")) {
            String errorMessage = String.format("Client not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.CLIENT_NOT_FOUND.toString());
        }

        String seller = auction.getSeller();
        if (!seller.equals(clientID)) {
            System.out.println("Error: the auction must be closed only by the seller whom created it!");
            return;
        }

        String status = auction.getStatus();
        if (!status.equals("open")) {
            System.out.println("Error: cannot close auction which is not open!");
            return;
        }


        auction.setStatus("closed");


        String closedAuctionJSON = genson.serialize(auction);
        stub.putStringState(auction.getAuctionID(), closedAuctionJSON);

        return;

    }


    // EndAuction both changes the auction status to closed and calculates the winners
    // of the auction
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void endAuction(final Context ctx, final String auctionID){
        String winner="";
        int price=0;

        ChaincodeStub stub = ctx.getStub();

        // get auction from public state
        Auction auction = genson.deserialize(queryAuction(ctx, auctionID).toString(), Auction.class);
        if (auction == null) {
            String errorMessage = String.format("chaincode.Auction not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.AUCTION_NOT_FOUND.toString());

        }


        // Check that the auction is being ended by the seller
        // get ID of submitting client
        String clientID = new String(getSubmittingClientIdentity(ctx), StandardCharsets.UTF_8);
        if (clientID.equals("")) {
            String errorMessage = String.format("Client not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.CLIENT_NOT_FOUND.toString());
        }


        String seller = auction.getSeller();
        if (!seller.equals(clientID)) {
            System.out.println("Error: the auction must be ended only by the seller whom created it!");
            return;
        }

        String status = auction.getStatus();
        if (!status.equals("closed")) {
            System.out.println("Error: can only end a closed auction!");
            return;
        }

        // get the list of revealed bids
        HashMap<String, FullBid> revealedBidMap = auction.getRevealedBids();
        if (revealedBidMap.size() == 0) {
            System.out.println("Error: No bids have been revealed, cannot end the auction!");
            return;
        }


        // determine the highest bid
        for (Map.Entry<String, FullBid> bid : revealedBidMap.entrySet()) {
            if (bid.getValue().getPrice() > auction.getPrice()) {
                winner = bid.getValue().getBidder();
                price = bid.getValue().getPrice();
            }
        }

        if (auction.getRevealedBids().size() != auction.getHashBids().size()) {
            System.out.println("Error: cannot end auction because not all the bids have been revealed!");
            return;
        }

        auction.setPrice(price);
        auction.setWinner(winner);
        auction.setStatus("ended");

        String endedAuctionJSON = genson.serialize(auction);
        stub.putStringState(auction.getAuctionID(), endedAuctionJSON);

        return;


    }




    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param auctionID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean auctionExists(final Context ctx, final String auctionID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(auctionID);

        return (assetJSON != null && !assetJSON.isEmpty());
    }


    /**
     * Checks that client org id matches peer org id
     *
     * @param ctx the transaction context
     * @return boolean indicating the matching
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean verifyClientOrgMatchesPeerOrg(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        String clientMSPID = ctx.getClientIdentity().getMSPID();
        if(clientMSPID == null){
            System.out.println("Error: failed getting client's MSPID");
            return false;
        }

        String peerMSPID = stub.getMspId();
        if(peerMSPID == null){
            System.out.println("Error: failed getting peer's MSPID");
            return false;
        }

        if(!clientMSPID.equals(peerMSPID)){
            System.out.println("Error: client is not authorized to read or write private data from that org peer!!");
            return false;
        }

        return true;
    }



    /**
     * Checks the existence of the client on the ledger
     *
     * @param ctx the transaction context
     * @return String indicating the clientID
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] getSubmittingClientIdentity (final Context ctx) {
        String b64ID = ctx.getClientIdentity().getId();
        if (b64ID == null){
            b64ID = "";
        }

        return Base64.getDecoder().decode(b64ID);
    }


    /**
     * Checks the existence of the client on the ledger
     *
     * @param ctx the transaction context
     * @return String indicating the collection name
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getCollectionName (final Context ctx) {

        // Get the MSP ID of submitting client identity
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        if(clientMSPID == null){
            return null;
        }

        // Create the collection name
        return "_implicit_org_" + clientMSPID;

    }




    /**
     * Checks the existence of the auction on the ledger
     *
     * @param ctx the transaction context
     * @param auctionID indicating the id of the auction of interest
     * @return chaincode.Auction indicating the auction of interest
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] queryAuction (final Context ctx, final String auctionID) {
        byte[] auctionJSON = ctx.getStub().getState(auctionID);
        if(auctionJSON == null){
            return null;
        }

        return auctionJSON;
    }



    /**
     * Checks the existence of the bid on the ledger
     *
     * @param ctx the transaction context
     * @param auctionID indicating the id of the auction of interest
     * @return chaincode.Auction indicating the auction of interest
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] queryBid (final Context ctx, final String auctionID, final String txID) {
        if(!verifyClientOrgMatchesPeerOrg(ctx)){
            System.out.println("failed to get implicit collection name");
            return null;
        }

        String clientID = new String(getSubmittingClientIdentity(ctx), StandardCharsets.UTF_8);
        String collection = getCollectionName(ctx);

        String bidkey = ctx.getStub().createCompositeKey("bidkey", auctionID, txID).toString();
        byte[] bidJSON = ctx.getStub().getPrivateData(collection, bidkey);

        return bidJSON;
    }


    /**
     * Set the endorsement policy of a new auction
     *
     * @param ctx the transaction context
     * @param auctionID indicating the id of the auction of interest
     * @param orgToEndorse indicating the organization to endorse
     * @return
     */

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void setAssetStateBasedEndorsement(final Context ctx, final String auctionID, final String orgToEndorse){
        ChaincodeStub stub = ctx.getStub();

        StateEP endorsementPolicy = new StateEP(new byte[0]);
        endorsementPolicy.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, orgToEndorse);

        byte[] policy = endorsementPolicy.policy();
        stub.setStateValidationParameter(auctionID, policy);

        return;
    }



    /**
     * Add a new organization as an endorser of the auction
     *
     * @param ctx the transaction context
     * @param auctionID indicating the id of the auction of interest
     * @param orgToEndorse indicating the organization to add
     * @return
     */


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addAssetStateBasedEndorsement(final Context ctx, final String auctionID, final String orgToEndorse){
        ChaincodeStub stub = ctx.getStub();

        byte[] endorsementPolicy = stub.getStateValidationParameter(auctionID);
        if(endorsementPolicy == null){
            String errorMessage = String.format("Endorsement policy not found");
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AuctionErrors.POLICY_NOT_FOUND.toString());
        }

        StateEP newEndorsementPolicy = new StateEP(endorsementPolicy);

        newEndorsementPolicy.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, orgToEndorse);

        byte[] newPolicy = newEndorsementPolicy.policy();

        stub.setStateValidationParameter(auctionID, newPolicy);

        return;
    }

}
