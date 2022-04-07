package application;

import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

public class AppRegisterUser {

    public static void main(String[] args) throws Exception {

        String org = args[0];
        String user = args[1];
        String msp = "";
        String port = "";


        if (org.equals("org1")){
            msp = "Org1MSP";
            port = "7054";
        }
        else{
            msp = "Org2MSP";
            port = "8054";
        }

        // Create a CA client for interacting with the CA.
        Properties props = new Properties();
        props.put("pemFile",
                String.format("../../../test-network/organizations/peerOrganizations/%s.example.com/ca/ca.%s.example.com-cert.pem", org, org));
        props.put("allowAllHostNames", "true");
        HFCAClient caClient = HFCAClient.createNewInstance(String.format("https://localhost:%s", port), props);
        CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
        caClient.setCryptoSuite(cryptoSuite);

        // Create a wallet for managing identities
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(String.format("wallet/%s", org)));

        // Check to see if we've already enrolled the user.
        if (wallet.get(user) != null) {
            System.out.println(String.format("An identity for the user %s already exists in the wallet", user));
            return;
        }

        X509Identity adminIdentity = (X509Identity)wallet.get("admin");
        if (adminIdentity == null) {
            System.out.println("\"admin\" needs to be enrolled and added to the wallet first");
            return;
        }

        User admin = new User() {

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return null;
            }

            @Override
            public String getAccount() {
                return null;
            }

            @Override
            public String getAffiliation() {
                return String.format("%s.department1", org);
            }

            @Override
            public Enrollment getEnrollment() {
                return new Enrollment() {

                    @Override
                    public PrivateKey getKey() {
                        return adminIdentity.getPrivateKey();
                    }

                    @Override
                    public String getCert() {
                        return Identities.toPemString(adminIdentity.getCertificate());
                    }
                };
            }

            @Override
            public String getMspId() {
                if (org.equals("org1")){
                    return "Org1MSP";
                }
                return "Org2MSP";
            }

        };

        // Register the user, enroll the user, and import the new identity into the wallet.
        System.out.println("Creation of registration request...");
        RegistrationRequest registrationRequest = new RegistrationRequest("appUser");
        System.out.println("Ok registration request created!!");
        registrationRequest.setAffiliation(String.format("%s.department1", org));
        registrationRequest.setEnrollmentID(user);
        String enrollmentSecret = caClient.register(registrationRequest, admin);
        System.out.println("Creation of Enrollment...");
        Enrollment enrollment = caClient.enroll(user, enrollmentSecret);
        System.out.println("Ok Enrollment creation!");
        Identity userID = Identities.newX509Identity(msp, enrollment);
        wallet.put(user, userID);
        System.out.println("Successfully enrolled user " + user + " and imported it into the wallet");
    }

}

