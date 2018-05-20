package com.acme.auction.ws;

import com.acme.auction.model.AuctionRequest;
import com.acme.auction.service.AuctionService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest API. localhost:8080/auction/*
 */
@RequestMapping("/auction")
@RestController
public class AuctionController {

    @Autowired
    AuctionService auctionService;

    @ApiOperation("Deploy auction contract")
    @RequestMapping(value = "/deploy", method = RequestMethod.POST)
    public String deploy(@RequestBody AuctionRequest auctionRequest) throws Exception {
        return auctionService.deploy(auctionRequest);
    }

    @ApiOperation("Start a new auction")
    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public String startAuction(@RequestBody AuctionRequest auctionRequest) throws Exception {
        return auctionService.startAuction(auctionRequest);
    }

    @ApiOperation("Bid")
    @RequestMapping(value = "/bid", method = RequestMethod.POST)
    public String bid(@RequestBody AuctionRequest auctionRequest) throws Exception {
        return auctionService.bid(auctionRequest);
    }

    @ApiOperation("End auction")
    @RequestMapping(value = "/end", method = RequestMethod.POST)
    public String endAuction(@RequestBody AuctionRequest auctionRequest) throws Exception {
        return auctionService.endAuction(auctionRequest);
    }


}
