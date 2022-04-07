Nel Repository le ho messo i due elementi principali, e cioè una folder chaincoode, che contiene lo smart contract e l'applicazione,
e una folder test-network, che è quella che serve per far partire la rete.

I prerequisiti sono le immagini docker messe a disposizione da fabric che possono essere scaricate tramite il comando:
curl -sSL https://bit.ly/2ysbOFE | bash -s
assieme anche ai fabric samples che contengono alcuni esempi.
Tutto il resto dovrebbe già essere contenuto nelle due folder che ci sono dentro al repository. 

Dentro dentro la folder chaincode le ho inoltre messo il file build.gradle con dentro tutte le dipendenze per poter usare le librerie di Hyperledger Fabric.

STRUTTURA DELLA FOLDER test-network
-test-network
------->bin (contiene i binari di fabric che servono per lanciare le funzioni all'interno degli script per creare i channel, fare il deploy dei chaincode e tirare su la rete)
------->config
------->configtx
------->docker (contiene i file che servono per creare i container docker)
------->organizations
------->scripts
        ------->deployCC.sh (Viene usato per fare il deploy della chiancode)
        ------->createChannel.sh (Viene usato per creare un channel)
        ------->envVar.sh (Contiene delle variabili ambiente utilizzate nei due script precedenti)
------->system-genesis-block

Per replicare l'errore i passi da seguire sono i seguenti:

1. entrare nella cartella test-network e lanciare lo script network.sh con le opzioni up createChannel -ca
./network.sh up createChannel -ca
Viene creato un channel dal nome mychannel e vengono create anche le Certificate Authorities necessarie nella folder organizations ,sempre contenuta in test-network,
all'interno di due nuove folder dal nome peerOrganizations e orderOrganization

2. Una volta creato il canale bisogna eseguire il seguente comando per deployare la chaincode
./network.sh deployCC -l java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"
Il comando fa ad impacchettare la chaincode (che viene chiamata auction-chaincode) e la installa sui peers peer0.org1.example.com e peer0.org2.example.com
La chaincode impacchettata la si può vedere all'interno della folder chaincode nella cartella test-network con il nome di auction-chaincode.tar.gz

STRUTTURA DELLA FOLDER chaincode
-chaincode
------>java
      ------->auction-chaincode
             ------>build
             ------>config
             ------>gradle
             ------>src
                   ----->main
                        ----->java
                             ----->application (Contiene i file dell'applicazione)
                             ----->chaincode (contiene i file per lo smart contract)

            ------>buil.gradle (contiene le dipendenze e i task creati da me)
            ----->gradlew
            ----->gradlew.bat
            ----->settings.gradle


3. Vado nella cartella chaincode e uso il comando per fare l'enroll dell'admin per le due organizzazioni
./gradlew EnrollAdmin --args="org1"
./gradlew EnrollAdmin --args="org2"
Si viene a creare una nuova folder wallet che contiene le identità criptate dei due admin

4. Creo le identità anche per gli utenti
./gradlew RegisterUser --args="org1 seller" -----> Colui che mende il bene a disposizione
./gradlew RegisterUser --args="org1 bidder1"
./gradlew RegisterUser --args="org2 bidder2"


5. Infine l'errore nasce quando invoco per la prima volta il contratto con il comando:
./gradlew CreateAuction --args="org1 seller auction1 playerName playerRole"

Il comando createAuction lancia l'applicativo AppCreateAuction.java che è contenuto all'interno della folder application nella folder chaincode.




