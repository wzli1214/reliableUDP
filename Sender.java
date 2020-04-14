
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

import java.util.zip.CRC32;
import java.net.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

//Checksum
class CheckSum {
	
	public long generate_checksum(byte[] b) {
		CRC32 crc32 = new CRC32();
		crc32.update(b);
		return crc32.getValue();	
		
	}
	
	public boolean validate_checksum(String message) {
		String[] pieces = message.split("\\|");
        String ack = pieces[0];
        String seq_no = pieces[1];
        String data = ack + "|" + seq_no + "|";
        String checksum = pieces[pieces.length - 1];
        System.out.println("generatedChecksum is " + generate_checksum(data.getBytes()));
        System.out.println("original checksum is " + Long.valueOf(checksum));
        return generate_checksum(data.getBytes()) == Long.valueOf(checksum);
        
	}
	
}

//Basic Sender
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

//Sender
class Sender extends BasicSender {
    
	//sliding window size is 5
    public String[] packetStrs;  
    public int packetsfilled;
    public int seqno;
    public String msg_type;
    

	public Sender(String _dest, int _port, String filename, boolean _debug) throws Exception{
    	
        super(_dest, _port, filename, _debug);
      
        this.packetStrs = new String[5]; 
        this.packetsfilled = 0;
        this.seqno = 0;
        this.msg_type = "";
    }
    
    
    @Override
    //super.fis.read to read the file by bytes
    public void start() throws Exception{
    	
    	//re-initialize
    	this.packetStrs = new String[5]; 
    	this.packetsfilled = 0;
    	this.seqno = 0;
    	this.msg_type ="";
    	
    	while (!msg_type.equals("end")) {
    		
    		//fill the sliding window
    		this.fill_array();
    		
    		this.send_packets_from_to(0, this.packetsfilled);
    		
    		
    		boolean completedWindow = false;
    		
    		//If not fully completed, need to resend some packet
    		while (completedWindow != true) {
    			
    			DatagramPacket[] received_acks = new DatagramPacket[5];
    			
    			for (int i = 0; i < this.packetsfilled; i++) {
    				received_acks[i] = this.receive(500);
    				
    			}
    			
    			if (this.msg_type.equals("end")) {
    				System.out.println("Transfer complete!");
    				break;
    			}
    			
    			
    			//GO-BACK-N
    			int[] ackNos = this.parse_acks(received_acks);
    			int errorCode = this.check_for_error(ackNos);
    			
//    			int checkSumErrorCode = this.check_for_checksum(received_acks);
//    			
//    			System.out.println("Check for check sum, checkSumErrorCode is " + checkSumErrorCode);
    			
    			
    			//errorCode == -1 means no error
    			if (errorCode != -1) {
    				System.out.println("Error");
    				
    				if (errorCode < 10) {
    					System.out.println("Timeout error at index" + errorCode);
    					//resend the packets from this error index
    					this.send_packets_from_to(errorCode, this.packetsfilled);
    				}
    				
    				//or duplicated error
    				else {
    					int errorIdx = errorCode - 10;
    					
    					System.out.println("Duplicated error at index" + errorIdx);
    					
    					if (ackNos[errorIdx] != this.seqno) {
    						this.send_packets_from_to(errorIdx, this.packetsfilled);
    					}
    					
    					//Else, complete the sliding window, the ack is nextSeqNo 
    					//outside the sliding window
    					else {
    						completedWindow = true;
    					}
    				}
    				
    			}
    			
    			else if (this.completed_window(ackNos)) {
    				completedWindow = true;
    			}
    			
    		}
    			
    	}
    	
    	this.fis.close();
        
    }
    
    //Fill the sliding window
    public void fill_array() {
    	//re-initialize
    	this.packetStrs = new String[5]; 
    	this.packetsfilled = 0;
    	
    	for (int i = 0; i < 5; i++) {
    		if (msg_type.equals("end"))
    			break;
    		
    		byte[] chunk = new byte[1450];
    		
    		try {
				int readCode = super.fis.read(chunk);
				System.out.println("Packet index: " + i);
				this.msg_type = "data";
	    		String msg = new String(chunk);
				System.out.println("Read data is:  " + msg);

	    		if (this.seqno == 0) {
	    			this.msg_type = "start";
	    		} else if (readCode == -1) {
	    			this.msg_type = "end";
	    			
	    		}
	    		
	    		String packet = this.make_packetString(msg_type, this.seqno, msg);
				System.out.println("combined packet " + packet);

	    		
	    		this.packetStrs[i] = packet;
	    		this.seqno++;
	    		this.packetsfilled++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    	}
    	
    }
    
    //send all packets in the sliding window
    public void send_packets_from_to(int from, int to) {
    	
    	for (int i = from; i < to; i++) {
    		this.send(this.packetStrs[i]);
    	}
    	
    }
    
    //ackNos[i] == -1 means there is no ackNo for this index
    public int[] parse_acks(DatagramPacket[] received_acks) {
    	int[] ackNos = new int[5];
    	Arrays.fill(ackNos, -1);
    	
    	for (int i = 0; i < 5; i++) {
    		if (received_acks[i] != null) {
    			
    			String strData = this.packetToString(received_acks[i]);
    			
    			//check 没收到数据
    			String[] splitData = strData.split("\\|");
    			System.out.println("Received checksum of packet " + i + "is "+ splitData[2]);

     			
    			try {
    				ackNos[i] = Integer.parseInt(splitData[1]);
    				System.out.println("ackNos[i] is " + ackNos[i]);
    			} catch (NumberFormatException nfe){
    			    nfe.printStackTrace();

    			}
    		
    		}
    		
    	}
    	return ackNos;
    }
    
    //check loss error or duplicated
    //error code -1 means no error
    public int check_for_error(int[] ackNos) {
    	for (int i = 0; i < 4; i++) {
    		if (ackNos[i] == -1)
    			return i;
    		else if(ackNos[i + 1] == -1) {
    			return i + 1;
    		}
    		
    		else {
    			int ackCurr = ackNos[i];
    			int ackNext = ackNos[i + 1];
    			
    			//duplicated
    			if (ackCurr == ackNext) 
    				return 10 + i;
    		}
    	}
    	
    	return -1;
    }
    
    //if return -1, means no error. Otherwise, return the index of packet which has error checksum.
    public int check_for_checksum(DatagramPacket[] received_acks) {
    	for (int i = 0; i < 5; i++) {
    		String packI = this.packetToString(received_acks[i]);
    		
    		if (new CheckSum().validate_checksum(packI)) {
              System.out.println("Received successfully, checksum is correct");
    		}
          
    		else {
              System.out.println("Received successfully but checksum failed at index: " + i);
              return i;
          	}
    		
    	}
    	
    	return -1;
    }
    
    public boolean completed_window(int[] ackNos) {
    	for (int i = 0; i < 5; i++) {
    		if (ackNos[i] == -1)
    			return false;
    	}
    	
    	return true;
    }
    
//    public void handle_response(DatagramPacket response) {
//        //get a string
//        byte[] responseByte = response.getData();
//        String responseStr = new String(responseByte);
//        
//        if (new CheckSum().validate_checksum(responseStr)) {
//            System.out.println("Received successfully, checksum is correct");
//        }
//        
//        else {
//            System.out.println("Received successfully but checksum failed");
//        }
//        
//    }

    public static void main(String[] args) throws Exception {
        
        if (args.length != 3) {
            System.out.println("Please follow the command format: " + 
                "java Sender <filename> <port> <Destination address> ");
        }
        
        String filename = "/Users/weizhaoli/Desktop/lorem-ipsum.txt";
        String dAddress = "localhost";
        
        int dport = 33122;
        
        //real parameter
        filename = args[0];
        dAddress = args[2];
        
        dport = Integer.parseInt(args[1]);
        
        Sender sender = new Sender(dAddress, dport, filename, false); 
        
        sender.start();
        
    }
    
}