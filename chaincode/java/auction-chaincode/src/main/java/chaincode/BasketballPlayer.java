package chaincode;

import java.util.Objects;

public class BasketballPlayer {


    private String assetID;


    private String name;


    private String role;


    private String owner;


    private int appraisedValue;

    public String getAssetID() {
        return assetID;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getOwner() {
        return owner;
    }

    public int getAppraisedValue() {
        return appraisedValue;
    }


    public BasketballPlayer(String assetID, String name,
                           String role, String owner,
                           int appraisedValue) {
        this.assetID = assetID;
        this.name = name;
        this.role = role;
        this.owner = owner;
        this.appraisedValue = appraisedValue;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        BasketballPlayer other = (BasketballPlayer) obj;

        return Objects.deepEquals(
                new String[] {getAssetID(), getName(), getRole(), getOwner()},
                new String[] {other.getAssetID(), other.getName(), getRole(), other.getOwner()})
                &&
                Objects.deepEquals(
                        new int[] {getAppraisedValue()},
                        new int[] {other.getAppraisedValue()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssetID(), getName(), getRole(), getOwner(), getAppraisedValue());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [assetID=" + assetID + ", name="
                + name + ", role=" + role + ", owner=" + owner + ", appraisedValue=" + appraisedValue + "]";
    }

}
