package ch.usi.inf.dslab.byzcast.kvs;

// RCVQINC Message received but has a incomplete quorum
// RCV received
// FORWARDED forwarded
// REPQINC reply quorum incomplete
// REQCOMP
// REPLYED
// NDA
public enum RequestStatus { RCVQINC, RCVD, FORWARDED, EXECUTED, REPQINC, REQCOMP, REPLIED, NDA }
