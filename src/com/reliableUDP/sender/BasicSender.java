package com.reliableUDP.sender;

import java.net.*;
import com.reliableUDP.checksum.CheckSum;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


abstract class BasicSender {
	protected boolean debug;
    protected String dest;
    protected int dport;
    protected DatagramSocket sock;
    protected FileInputStream fis;

    
    public BasicSender(String _dest, int _port, String filename, boolean _debug) throws SocketException, UnknownHostException {
        this.debug = _debug;
        this.dest = _dest;
        this.dport = _port;
        
        this.sock = new DatagramSocket();
        this.sock.setSoTimeout(0); //blocking， never timeout
      
        this.fis = null;
        try {
			this.fis = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
    }
    
    // Waits until packet is received to return.
    
    //return a DatagramPacket
	public DatagramPacket receive(int timeout) {
        
        try{
        	this.sock.setSoTimeout(timeout);
            byte[] data = new byte[4096];
            DatagramPacket reply = new DatagramPacket(data, data.length);
            this.sock.receive(reply);
            
            // String res = new String(reply.getData());
            return reply;
            
        } catch(Exception e) {
            e.printStackTrace();
        } 
        
        byte[] dataEmpt = new byte[4096];
    	DatagramPacket empt = new DatagramPacket(dataEmpt, dataEmpt.length);
    	return empt;
        
    }
    
    // Sends a packet to the destination address.
    //make a datagrampacket, and send
    public void send(String message) {
        byte[] byteMsg = message.getBytes();
        
        InetAddress dAddress;
		try {
			dAddress = InetAddress.getByName(this.dest);
			DatagramPacket packetToSend = new DatagramPacket(byteMsg,
		            byteMsg.length, dAddress, this.dport);
			try {
				this.sock.send(packetToSend);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        
        
    }
    
    // Prepares a packet, make a concatenated String
    public String make_packetString(String msg_type, int seqno, String data) {
        String body = String.format("%s|%d|%s|", msg_type, seqno, data);
        CheckSum checksumUse = new CheckSum(); 
        long checksum = checksumUse.generate_checksum(body.getBytes());
        String packet = String.format("%s%s", body, checksum);
        return packet;
    }
 
    //format of ack message is different from others
    public String[] split_packet(String message) {
        String[] pieces = message.split("|");
        //String message types = pieces[0];
        //String seq_no = pieces[1];
        
        return pieces;
    }
    
    public String packetToString(DatagramPacket pck) {
    	try {
			return new String(pck.getData(), 0, pck.getLength(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return "";
    	
    }

    
    //Main sending loop
    public abstract void start() throws Exception; 
        
    
}