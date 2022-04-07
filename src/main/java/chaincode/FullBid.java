package chaincode;

import java.util.Objects;

public class FullBid {
    private final int price;
    private final String org;
    private final String bidder;

    public int getPrice() {return price;}

    public String getOrg(){return org;}

    public String getBidder(){return bidder;}



    public FullBid(int price, String org, String bidder){
        this.price=price;
        this.bidder=bidder;
        this.org=org;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        FullBid other = (FullBid) obj;


        return Objects.deepEquals(
                new String[] {getBidder(), getOrg()},
                new String[] {other.getBidder(), other.getOrg()})
                &&
                Objects.deepEquals(
                        new int[] {getPrice()},
                        new int[] {other.getPrice()});

    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [price=" + price + ", org="
                + org + ", bidder=" + bidder + "]";
    }

}
