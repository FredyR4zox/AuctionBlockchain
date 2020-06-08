# AuctionBlockchain

### Deployment

The deployment of the project was made using Google Cloud.
To create a Virtual Machine in the Google Cloud that hosts this project, we used the Compute Engine to create a VM, and installed openjdk-11-jdk. Then we made a machine image and launched four different instances of that image. These machines are the bootstrap, the miner, the auctioneer and the client. 
All different types of machines are run from the same jar file, and its parameters are what defines the node type. These machines also have access to predefined wallets so that it is more easy to test the program because now we have the private key of the node that mined the genesis block and that has the initial coins.


There are four types of nodes: bootstrap, miner, auctioneer (seller) and client (buyer):
 * Bootstrap: This type of node simply listens for connections from any nodes and saves the nodes that have contacted it to it's k-buckets. It is also the node that creates and mines the genesis block and keeps the blockchain updated in it's memory. When a node wants to join the network it first contacts this node to retrieve information about other nodes.
 * Miner: This node type is the most common type. It listens for blocks and transactions, aggregates the transactions to form a block and tries to mine that block.
 * Auctioneer: The auctioneer node type has a text interface to create a new auction and converts the winning bid in a transaction when the auction ends. This transactions is signed by the auctioneer.
 * Client: This type of node is the node that listens for auctions and makes bids to those auctions. The user is also notified if an auction in which he made a bid has received a higher bid.


As stated before, the type of node is defined in the arguments when booting up the program. To view the usage and the list of all arguments of the program, simple execute it without arguments.


### Compilation

To compile the program run:
```bash
$ mvn clean install
```

The compiled jar should be on the directory "target" and have the name "AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar".

### Running

The commands to initiate all node types are as follow:
 * Bootstrap (change BootstrapIPAddress to 127.0.0.1 to test on local machine): java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar bootstrap <walletFile> <myIPAddress> <myPort> \end{verbatim}
 * Miner: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar miner <walletFile> <myIPAddress> <myPort> <BootstrapIPAddress> <BootstrapPort> \end{verbatim}
 * Auctioneer: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar auctioneer <walletFile> <myIPAddress> <myPort> <BootstrapIPAddress> <BootstrapPort> \end{verbatim}
 * Client: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar client <walletFile> <myIPAddress> <myPort> <BootstrapIPAddress> <BootstrapPort> \end{verbatim}

### Arguments

Arguments:
 * walletFile: The file in "wallets" directory in which to load the wallet (private and public keys and address). The default wallet file is "defaultWallet.txt" and if the directory or wallet file don't exist they are created.
 * myIPAddress: This is the IP address that the node will announce to other nodes as its IP address. Set to 127.0.0.1 if you want to test the program locally and don't have the ports forwarded on your router. Set to 0.0.0.0 to use your public IP address (the program will search for it) (good fo google cloud with allow-all in firewall settings). The default value is 0.0.0.0 which will get your public IP address.
 * myPort: This is the port to listen on and to announce to other nodes. The default value is 1337.
 * BootstrapIPAddress: The IP address of the bootstrap node. Set to 127.0.0.1 if you have the bootstrap node locally too. The default value is 34.105.188.87 which is the IP address of our Google Cloud VM that hosts the bootstrap node.
 * BootstrapPort: The port of the bootstrap node.

Attention: The arguments are in order an they cannot be set if the previous isn't set, i.e, to set the argument myPort, one must pass also the walletFile and myIPAddress argments.

### Examples

Examples to test locally:
 * Bootstrap: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar bootstrap alice.txt 127.0.0.1 \end{verbatim}
 * Miner: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar miner alice.txt 127.0.0.1 1337 127.0.0.1 \end{verbatim}
 * Auctioneer: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar auctioneer alice.txt 127.0.0.1 1337 127.0.0.1 \end{verbatim}
 * Client: java -jar AuctionBlockchain-1.0-SNAPSHOT-jar-with-dependencies.jar client alice.txt 127.0.0.1 1337 127.0.0.1 \end{verbatim}

