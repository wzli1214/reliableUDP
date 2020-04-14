import random

from BasicTest import *

"""
This prints out every packet sent but drops nothing.
This is helpful for basic debugging.
"""

class PrintNoDropTest(BasicTest):

    def handle_packet(self):
        for p in self.forwarder.in_queue:
            # Print out each packet (both directions)
            print p
            self.forwarder.out_queue.append(p)

        # empty out the in_queue
        self.forwarder.in_queue = []
