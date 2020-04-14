from BasicTest import *

class ReorderedPacketTest(BasicTest):
    def handle_packet(self):
        lengthy = len(self.forwarder.in_queue)
        if(lengthy > 1): # queue will always have at least 2 at some point because of start and end messages
            # send in reverse order
            x = lengthy
            while x > 0:
                self.forwarder.out_queue.append(self.forwarder.in_queue[x-1]) # insert the end of in_queue to the front of out_queeu
                x-=1

            # empty out the in_queue
            self.forwarder.in_queue = []
