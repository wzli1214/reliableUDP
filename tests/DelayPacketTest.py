import time
import random

from BasicTest import *

class DelayPacketTest(BasicTest):
    def handle_packet(self):
        for p in self.forwarder.in_queue:
            self.forwarder.out_queue.append(p)

            if random.choice([True, False]) and p.msg_type == "data":
            	# wait a random time less than a second
            	time.sleep(random.random() * random.random() / 2)

        # empty out the in_queue
        self.forwarder.in_queue = []
