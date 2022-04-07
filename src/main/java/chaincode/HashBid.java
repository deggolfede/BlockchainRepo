package chaincode;

import java.util.Objects;

public class HashBid {

    private final String hash;
    private final String org;

    public String getHash(){return this.hash;}
    public String getOrg() {return this.org;}



    public HashBid(String hash, String org){
        this.hash=hash;
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

        HashBid other = (HashBid) obj;


        return Objects.deepEquals(
                new String[] {getHash(), getOrg()},
                new String[] {other.getHash(), other.getOrg()});

    }


        @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [hash=" + hash + ", org="
                + org +"]";
    }

}
