import subprocess
import sys
import socket
import getopt


'''
This is a python wrapper for executing a java program.
You should be able to use it in the test harness by running:
  python TestHarness.py -s JavaSender.py -r Receiver.py
'''
if __name__ == "__main__":
    def usage():
        print "RUDP Wrapper"
        print "Runs a sender written in another language."
        print "-f FILE | --file=FILE The file to transfer; if empty reads from STDIN"
        print "-p PORT | --port=PORT The destination port, defaults to 33122"
        print "-a ADDRESS | --address=ADDRESS The receiver address or hostname, defaults to localhost"
        print "-h | --help Print this usage message"

    try:
        opts, args = getopt.getopt(sys.argv[1:],
                               "f:p:a:", ["file=", "port=", "address="])
    except:
        usage()
        exit()

    port = 33122
    dest = "localhost"
    filename = None

    for o,a in opts:
        if o in ("-f", "--file="):
            filename = a
        elif o in ("-p", "--port="):
            port = int(a)
        elif o in ("-a", "--address="):
            dest = a

    # Change this to your java program's main class
    javaclass = "Sender"
    # You may need to add other arguments such as setting the class path
    # You should do add these before the javaclass variable
    subprocess.call(["java", javaclass, "-f", filename, "-p", str(port), "-a", dest])
    
