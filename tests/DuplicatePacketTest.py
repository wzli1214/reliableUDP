import random

from BasicTest import *

class DuplicatePacketTest(BasicTest):
    def handle_packet(self):
        for p in self.forwarder.in_queue:
            # ignore all non data packets
            if p.msg_type != "data":
                self.forwarder.out_queue.append(p)
                continue
            
            self.forwarder.out_queue.append(p)

            if random.choice([True, True]):
            	# for i in range(0, random.randint(1, 2)):
            	self.forwarder.out_queue.append(p)
            	self.forwarder.out_queue.append(p)

        # empty out the in_queue
        self.forwarder.in_queue = []
