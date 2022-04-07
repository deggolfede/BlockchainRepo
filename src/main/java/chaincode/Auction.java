package chaincode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Auction {

    @Property()
    private final String auctionID;

    @Property()
    private final BasketballPlayer player;

    @Property()
    private final String seller;

    @Property()
    private final ArrayList<String> orgs;

    @Property()
    private final HashMap<String, HashBid> privateBids;

    @Property()
    private final HashMap<String, FullBid> revealedBids;

    @Property()
    private int price;

    @Property()
    private String winner;

    @Property()
    private String status;



    public String getAuctionID() {
        return auctionID;
    }

    public BasketballPlayer getPlayer() {
        return player;
    }

    public String getSeller() {
        return seller;
    }

    public ArrayList<String> getOrgs() {
        return orgs;
    }

    public HashMap<String, HashBid> getHashBids() { return privateBids; }

    public HashMap<String, FullBid> getRevealedBids() { return revealedBids; }

    public int getPrice() {
        return price;
    }

    public String getWinner() { return winner; }

    public String getStatus() { return status; }

    public void setStatus(final String status){this.status=status;}

    public void setPrice(final int price){this.price=price;}

    public void setWinner(final String winner){this.winner=winner;}




    public Auction(@JsonProperty("auctionID") final String auctionID, @JsonProperty("player") final BasketballPlayer player,
                   @JsonProperty("seller") final String seller, @JsonProperty("orgs") final ArrayList<String> orgs,
                   @JsonProperty("privateBids") final HashMap<String, HashBid> privateBids, @JsonProperty("revealedBids") final HashMap<String, FullBid> revealedBids,
                   @JsonProperty("price") final int price, @JsonProperty("winner") final String winner, @JsonProperty("status") final String status) {
        this.auctionID = auctionID;
        this.player = player;
        this.seller = seller;
        this.orgs = orgs;
        this.privateBids = privateBids;
        this.revealedBids = revealedBids;
        this.price = price;
        this.winner = winner;
        this.status = status;
    }

}
