from PyQt5.QtCore import QThread, pyqtSignal
from DataSocket import DataSocket as socket
import TTP_packet as ttp
import utility
import traceback
import time


class MessageWorker(QThread):

    conn_est = pyqtSignal(str)
    msg_sent = pyqtSignal(str)

    def __init__(self):
        QThread.__init__(self)

    def connect(self):
        syn = ttp.TTP_packet(flag=5, username='test', password='test')
        self.loggedin = False
        self.socket.add_send(syn)

    def receive(self, ttppacket):
        if (ttp.FLAG_TYPE[ttppacket.flag] == 'CON' and not self.loggedin):
            self.identification = ttppacket.data.decode()
            reply = ttp.TTP_packet(flag=1, message=self.identification)
            self.socket.add_send(reply)
            self.loggedin = True
        elif (ttp.FLAG_TYPE[ttppacket.flag] == 'CON' and self.loggedin):
            self.socket._send_protocol()
            port = utility.byte_to_int(ttppacket.data, 0)
            self.socket.socket.setPeerPort(port)
            self.socket.serverport = port

            try:
                self.socket.add_send(ttp.TTP_packet(sender_id=self.identification, conversation_id='044d1778-f760-11e8-b05c-2c600c8aa83e', message='a'*513))
                time.sleep(.5)
                convo = ttp.TTP_packet(flag=3, conversation_id='044d1778-f760-11e8-b05c-2c600c8aa83e', message_block_number=0)
            except Exception as e:
                traceback.print_exc()
            self.socket.add_send(convo)
        print(ttppacket.data.decode())

    def run(self):
        self.socket = socket(self)
        self.socket.start()
        self.connect()
