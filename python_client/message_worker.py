from PyQt5.QtCore import QThread, pyqtSignal
from DataSocket import DataSocket as socket
from TTP_packet import TTP_packet


class MessageWorker(QThread):

    conn_est = pyqtSignal(str)
    msg_sent = pyqtSignal(str)

    def __init__(self):
        QThread.__init__(self)

    def connect(self):
        ttp = TTP_packet(flag=5, username='test', password='test')
        self.socket.add_send(ttp)

    def receive(self, ttp):
        pass

    def run(self):
        self.socket = socket(self)
        self.socket.start()
        self.connect()
