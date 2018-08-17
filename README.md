# Auction Contract Service

This project demonstrates the integration of spring/java/webservices/web3j with the ethereum world.
It primarily illustrates connection to local/private nodes(geth) using admin api.
The accounts need not be unlocked in beforehand. They get unlocked as and when functionality is requested.

## Contract
At the heart of this is a solidity contract for a decentralised auction. The address that starts the auction is assumed to be
the owner. Every bidder, passes along with the bid, a key which places the bid in a kind of escrow till the bidder(or anyone)
ends the auction with the same key which also can signal that items of the bid were successfully removed.

The contract is exposed completely exposed via webservices.
You can start as early as deploying the contract from the webservice. Note:This app supports only one contract deploy per vm,
as the contract address is cached after the first deploy.
After the contract is deployed an auction can be started and biding  via webservice.

## Environment
I have used Intellij and java 1.8 to build thr app.

## Run
Run the AuctionApplication class which starts the embedded tomcat server at localhost:8080

## Usage

All available application endpoints are documented using [Swagger](http://swagger.io/).

You can view the Swagger UI at http://localhost:8080/swagger-ui.html. From here you
can perform all  requests easily to facilitate deployment of, transacting
with the auction contract. Example requests have been documented at resources/testScript.txt


For more info on web3j refer to :https://docs.web3j.io/getting_started.html
