pragma solidity ^0.4.0;
contract Auction{
uint256 public bidx;
address public bidderx;
uint256 public deadx;
uint256 public blockx;
uint256 public timeLimitx;
uint256 public timestampx;
string public auctionx;
string public endauctionx;
string public endkeyx;
	struct auction {
	    string name;
	    address owner;
		uint256 deadline;
		uint256 highestBid;
		address highestBidder;
		bytes32 bidHash;
	}
	mapping(bytes => auction) Auctions;

	function startAuction(string auctionName, uint256 timeLimit) public {
auctionx=auctionName;
		auction a = Auctions[bytes(auctionName)];
		a.name = auctionName;
 		a.deadline = block.number + timeLimit;
		a.owner = msg.sender;

 		deadx = a.deadline;
 		blockx = block.number;
		timeLimitx = timeLimit;


	}

	function bid(string auctionName, string key) payable public{
	bidx=msg.value;
	    require(msg.value >0);
		auction a = Auctions[bytes(auctionName)];
		if (a.highestBid > msg.value || block.number >= a.deadline) {
			msg.sender.transfer(msg.value);

		}

		a.highestBidder.transfer(a.highestBid);
		a.highestBidder = msg.sender;
		a.highestBid = msg.value;
		a.bidHash  = keccak256(key);
        bidderx = a.highestBidder;
	}

	function endAuction(string auctionName, string key) payable public {
	endauctionx =auctionName;
	endkeyx= key;
		auction a = Auctions[bytes(auctionName)];
		if (block.number >= a.deadline && keccak256(key) == a.bidHash) {
			a.owner.transfer(a.highestBid);
			clean(auctionName);
		}
	}
	function clean(string auctionName) private{
		auction a = Auctions[bytes(auctionName)];
		a.highestBid = 0;
		a.highestBidder =0;
		a.deadline = 0;
		a.owner = 0;
		a.bidHash = 0;
	}
}