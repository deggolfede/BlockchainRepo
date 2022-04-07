package application;

import java.nio.file.Paths;
import java.util.Properties;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;


public class AppEnrollAdmin {

    public static void main(String[] args) throws Exception {

        String org = args[0];
        String msp = "";
        String port = "";

        if (org.equals("org1")){
            msp = "Org1";
            port = "7054";
        }
        else{
            msp = "Org2";
            port = "8054";
        }

        // Create a CA client for interacting with the CA.
        Properties props = new Properties();
        props.put("pemFile",
                String.format("../../../test-network/organizations/peerOrganizations/%s.example.com/ca/ca.%s.example.com-cert.pem", org, org));
        props.put("allowAllHostNames", "true");
        HFCAClient caClient = HFCAClient.createNewInstance(String.format("https://localhost:%s", port), props);
        System.out.println("CA Client created!!");
        CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
        caClient.setCryptoSuite(cryptoSuite);

        // Create a wallet for managing identities
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(String.format("wallet/%s", org)));
        System.out.println(String.format("Wallet for %s created", org));

        // Check to see if we've already enrolled the admin user.
        if (wallet.get("admin") != null) {
            System.out.println("An identity for the admin user \"admin\" already exists in the wallet");
            return;
        }

        // Enroll the admin user, and import the new identity into the wallet.
        final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
        enrollmentRequestTLS.addHost("localhost");
        enrollmentRequestTLS.setProfile("tls");
        System.out.println("Tls set!!");
        Enrollment enrollment = caClient.enroll("admin", "adminpw", enrollmentRequestTLS);
        System.out.println("Enrollment succesfull!!");


        Identity user = Identities.newX509Identity(String.format("%sMSP", msp), enrollment);
        wallet.put("admin", user);
        System.out.println("Successfully enrolled user \"admin\" and imported it into the wallet");
    }
}
