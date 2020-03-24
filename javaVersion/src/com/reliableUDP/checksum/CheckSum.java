package com.reliableUDP.checksum;

import java.util.zip.CRC32;

public class CheckSum {
	
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
