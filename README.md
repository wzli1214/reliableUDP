
# Introduction
Author: Weizhao Li

In this project, I built a simple reliable transport protocol known as reliable udp (rudp). My protocol provides in-order, reliable delivery of UDP datagrams, and must do so in the presence of packet loss, delay, corruption, duplication, and re-ordering.

There are a variety of ways to ensure a message is reliably delivered from a sender to a receiver. UC Berkeley EE 122 provided you with a reference implementation of a receiver (in Python) that returns a cumulative ACK whenever it receives a data packet. My job is to implement a sender in Java via Go-Back-N, when sending packets to this receiver, achieves reliable delivery. 

**keywords: Sliding Window, Go-Back-N, Java, Checksum, Reliable UDP**

# The Reliable UDP Protocol 
Our simple protocol has four message types: `start`, `end`, `data`, and `ack`. `start`, `end`, and
`data` messages all follow the same general format:
```
start|<sequence number>|<data>|<checksum>
data|<sequence number>|<data>|<checksum>
end|<sequence number>|<data>|<checksum>
```
To initiate a connection, send a start message. The receiver will use the sequence number provided as the initial sequence number for all packets in that connection. After sending the start message, send additional packets in the same connection using the data message type, adjusting the sequence number appropriately. Unsurprisingly, the last data in a connection should be transmitted with the end message type to signal the receiver that the connection is complete. Your sender should accept acknowledgements from the receiver in the format:

```
ack|<sequence number>|<checksum>
```

An important limitation is the maximum size of your packets. The UDP protocol has an 8 byte header, and the IP protocol underneath it has a header of ~20 bytes. Because we will be using Ethernet networks, which have a maximum frame size of 1500 bytes, this leaves 1472 bytes for your entire packet (message type, sequence number, data, and checksum). 


The angle brackets ("<" and ">") are not part of the protocol. However, you should ensure that there are no extra spaces between your delimiters ("\|" character) and the fields of your packet. For specific formatting details, see the sample code provided.

# The Receiver Specification

We will provide a simple receiver for you; the reference implementation we provide will also be used for grading, so make sure that your sender is compatible with it. The receiver responds to data packets with cumulative acknowledgements. Upon receiving a message of type `start`, `data`, or `end`, the receiver generates an ack message with the sequence number it expects to receive next, which is the lowest sequence number not yet received. In other words, if it expects a packet of sequence number N, the following two scenarios may occur 
1. If it receives a packet with sequence number not equal to N, it will send “ack\|N”. 
2. If it receives a packet with sequence number N, it will check for the highest sequence number (say M) of the in-order packets it has already received and send “ack\|M+1”. For example, if it has already received packets N+1 and N+2 (i.e. M = N+2), but no others past N+2, then it will send “ack\|N+3”. 

Let us illustrate this with an example. Suppose packets 0, 1, and 2 are sent, but packet 1 is lost before reaching the receiver. The receiver will send “ack\|1” upon receiving packet 0, and then “ack\|1” again upon receiving packet 2. As soon as the receiver receives packet 1 (due to retransmission from the sender), it will send “ack\|3” (as it already has received, and upon receiving this acknowledgement the sender can assume all three packets were successfully received.

If the next expected packet is N, the receiver will drop all packets with sequence number greater than N+4; that is, the receiver operates with a window of five packets, and drops all packets that fall outside of that range. When the next unexpected packet is N+1 (due to N arriving), then the receiver will accept packet N+5. 

You can assume that once a packet has been acknowledged by the sender, it has been properly received. The receiver has a default timeout of 10 seconds; it will automatically close any connections for which it does not receive packets for that duration.

# The Sender Specification

The sender should read an input file and transmit it to a specified receiver using UDP sockets. It should split the input file into appropriately sized chunks of data, specify an initial sequence number for the connection, and append a checksum to each packet. The sequence number should increment by one for each additional packet in a connection. Functions for generating and validating packet checksums will be provided for you (see Checksum.py).

Your sender must implement a reliable transport algorithm (such as sliding window). The receiver’s window size is five packets, and it will ignore more than this. Your sender must be able to accept ack packets from the receiver. Any ack packets with an invalid checksum should be ignored.

Your sender should provide reliable service under the following network conditions:

- Loss: arbitrary levels; you should be able to handle periods of 100% packet loss.
- Corruption: arbitrary types and frequency.
- Re-ordering: may arrive in any order, and
- Duplication: you could see a packet any number of times.
- Delay: packets may be delayed indefinitely (but in practice, generally not more than 10s).

# Usage

### Step 1 
In this folder, run the python receiver.

```
python Receiver.py 
```

- -d True is optional if you want to see the details message from the receiver

### Step 2
I alrealdy compiled the java program, if you change the code, you have to re-compile the java file.

Under the folder, ```./reliableUDP/javaVersion/src```, enter

```
java Sender -f filename -p portNumber -a receiverAddress
```

- -f FILE | --file=FILE The file to transfer; if empty reads from STDIN
- -p PORT | --port=PORT The destination port, defaults to 33122
- -a ADDRESS | --address=ADDRESS The receiver address or hostname, defaults to localhost
- -h | --help Print this usage message

Then you can see the file transferred successfully. 

# Test the Accuracy
There are 15 test cases for you. You can write your own test cases.

Under ```reliableUDP``` folder, open JavaSender.py, you have to Change the javaclass to your java program's main class.

Then, under ```reliableUDP``` folder, enter 

```
python TestHarness.py -s JavaSender.py -r Receiver.py
```

TestHarness will run 15 test cases include 

- Loss: arbitrary levels; you should be able to handle periods of 100% packet loss.
- Corruption: arbitrary types and frequency.
- Re-ordering: may arrive in any order, and
- Duplication: you could see a packet any number of times.
- Delay: packets may be delayed indefinitely (but in practice, generally not more than 10s).

Our protocol will pass 14/15 cases.
Current progress: TestHarness could pass these test cases:

- BasicTest.BasicTest(forwarder, “README”) ✅
- BasicTest.BasicTest(forwarder, “README-4x”) ✅
- RandomDropTest.RandomDropTest(forwarder, “README-4x”) ✅

- PrintNoDropTest.PrintNoDropTest(forwarder, “README”) ✅

- PrintDropTest.PrintDropTest(forwarder, “README-4x”, 50) ✅

- RandomCorruptTest.RandomCorruptTest(forwarder, “README”) ✅

- DuplicatePacketTest.DuplicatePacketTest(forwarder, “README”) ✅

- DropAndDuplicatePacketTest.DropAndDuplicatePacketTest(forwarder, “README”) ✅

- DelayPacketTest.DelayPacketTest(forwarder, “README”) ✅

- ReorderedPacketTest.ReorderedPacketTest(forwarder, “README”) ✅

- DuplicatePacketTest.DuplicatePacketTest(forwarder, “README-4x”) ✅

- DropAndDuplicatePacketTest.DropAndDuplicatePacketTest(forwarder, “README-4x”) ✅

- DelayPacketTest.DelayPacketTest(forwarder, “README-4x”) ✅

- ReorderedPacketTest.ReorderedPacketTest(forwarder, “README-4x”) ✅

But failed:
- RandomCorruptTest.RandomCorruptTest(forwarder, "README-4x") ❌

# Reference
- Computer Networking: A Top-down Approach by Keith Ross

- AUTP https://github.com/JKalash/AUTP



> **Acknowledgements:** This project is based on an assignment from EE 121 at UC Berkeley by Soctt Shenker.
