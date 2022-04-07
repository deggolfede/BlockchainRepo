package chaincode;

import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;


import java.nio.charset.StandardCharsets;
import java.util.*;

public class StateEP implements StateBasedEndorsement{

    private HashMap<String, RoleType> orgs;
    private byte[] endorsementPolicy;

    public HashMap<String, RoleType> getOrgs(){return this.orgs;}

    public StringBuilder createPolicy(StringBuilder newPolicy){
        for (HashMap.Entry<String, RoleType> entry : this.getOrgs().entrySet()) {
            newPolicy.append(entry.getKey()).append(".").append(entry.getValue().toString().toLowerCase()).append("', ");
        }
        newPolicy.delete(newPolicy.capacity()-2, newPolicy.capacity()-1);
        newPolicy.append(")");
        System.out.println(newPolicy.toString());
        return newPolicy;
    }

    @Override
    public byte[] policy() {
        StringBuilder newPolicy = new StringBuilder();
        if(this.endorsementPolicy.length == 0) {
            newPolicy.append("AND('");
            newPolicy = createPolicy(newPolicy);
        }
        else{
            newPolicy.append(this.endorsementPolicy.toString());
            newPolicy.deleteCharAt(newPolicy.capacity()-1);
            newPolicy.append(", ");
            newPolicy = createPolicy(newPolicy);
        }

        return newPolicy.toString().getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public void addOrgs(RoleType roleType, String... organizations) {
        RoleType role = null;
        switch (roleType){
            case RoleTypeMember:
                role = RoleType.RoleTypeMember;
                break;

            case RoleTypePeer:
                role = RoleType.RoleTypePeer;
                break;

            default:
                break;
        }

        for(String org : organizations){
            this.getOrgs().put(org, role);
        }

        return;
    }

    @Override
    public void delOrgs(String... organizations) {
        for(String org : organizations){
            this.getOrgs().remove(org);
        }

        return;

    }

    @Override
    public List<String> listOrgs() {
        List<String> orgsNames = new ArrayList<>();
        for(Map.Entry<String, RoleType> org : this.getOrgs().entrySet()) {
            orgsNames.add(org.getKey());
        }
        return orgsNames;
    }



    public StateEP(byte[] policy){
        this.endorsementPolicy = policy;
        this.orgs = new HashMap<String, RoleType>();
    }

}
