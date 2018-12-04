from PyQt5.QtCore import QThread, pyqtSignal
from DataSocket import DataSocket as socket
import TTP_packet as ttp
import utility


class MessageWorker(QThread):

    conn_est = pyqtSignal()
    msg_sent = pyqtSignal()
    msg_received = pyqtSignal(object)
    convo_id = '044d1778-f760-11e8-b05c-2c600c8aa83e'

    def __init__(self, parent):
        QThread.__init__(self, parent)

    def connect_to_server(self, user, passw):
        syn = ttp.TTP_packet(flag=5, username=user, password=passw)
        self.loggedin = False
        self.socket.add_send(syn)

    def send_message(self, msg):
        msg_packet = ttp.TTP_packet(flag=0, sender_id=self.identification, conversation_id=self.convo_id, message=msg)
        self.socket.add_send(msg_packet)
        try:
            self.msg_sent.emit()
        except Exception as e:
            print(e)

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
            self.conn_est.emit()
        elif (ttp.FLAG_TYPE[ttppacket.flag] == 'CCV'):
            self.convo_id = ttppacket.data.decode()
        elif (ttp.FLAG_TYPE[ttppacket.flag] == 'MSG'):
            ttppacket.get_message()
            self.msg_received.emit((ttppacket.sender_id, ttppacket.timestamp, ttppacket.message))
        # print(ttppacket.data.decode())

    def run(self):
        self.socket = socket(self)
        self.socket.start()
