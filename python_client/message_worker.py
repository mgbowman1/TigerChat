from PyQt5.QtCore import QThread, pyqtSignal
from DataSocket import DataSocket as socket
import TTP_packet as ttp
import utility


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
            port = utility.byte_to_int(ttppacket.data, 0)
            self.socket.socket.setPeerPort(port)
            self.socket.serverport = port

            convo = ttp.TTP_packet(flag=7, username_arr=['3bb2e85e-f75c-11e8-b05c-2c600c8aa83e'])
            self.socket.add_send(convo)
        print(ttppacket.data.decode())

    def run(self):
        self.socket = socket(self)
        self.socket.start()
        self.connect()
