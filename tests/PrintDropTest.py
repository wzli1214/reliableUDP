import random

from BasicTest import *

"""
This tests a single packet drop. If more than 10 packets total are sent, then it
immediately exits. It also prints out every packet sent.
This is helpful for basic debugging.
"""

class PrintDropTest(BasicTest):
    def __init__(self, forwarder, input_file, max_pkts):
        super(PrintDropTest, self).__init__(forwarder, input_file)
        self.num_pkts = 0
        self.max_pkts = max_pkts

    def handle_packet(self):
        for p in self.forwarder.in_queue:
            # Print out each packet (both directions)
            print p
            if p.msg_type == "data" or p.msg_type == "end":
                self.num_pkts = self.num_pkts + 1
                if self.num_pkts > self.max_pkts:
                    print ("FAILURE Tried to send more than max packets!")
                    exit(-1)
                if self.num_pkts == 2:
                    print "DROPPING 2nd DATA PACKET!"
                else:
                    self.forwarder.out_queue.append(p)
            else:
                # send all ACK packets correctly
                self.forwarder.out_queue.append(p)

        # empty out the in_queue
        self.forwarder.in_queue = []
