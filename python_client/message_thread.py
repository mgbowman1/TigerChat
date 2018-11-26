from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtNetwork import QUdpSocket, QHostAddress


class MessageWorker(QThread):

    conn_est = pyqtSignal()
    msg_sent = pyqtSignal()

    def __init__(self):
        QThread.__init__(self)
        self.server_addr = QHostAddress('75.65.192.207')
        self.server_port = 11000
        self.message = 'hi'
        self.data = bytes(4) + self.message.encode()

    def __del__(self):
        self.wait()

    def sendMessage(self, msg):
        self.data = bytes(4) + msg.encode()
        self.client_socket.writeDatagram(self.data, self.server_addr, self.server_port)
        self.client_socket.waitForReadyRead()
        self.return_msg = self.client_socket.readDatagram(2048)
        print(str(self.return_msg[0].decode()))
        self.msg_sent.emit()

    def run(self):
        self.client_socket = QUdpSocket()
        self.client_socket.bind(26001)

        self.client_socket.writeDatagram(self.data, self.server_addr, self.server_port)
        self.client_socket.waitForReadyRead()
        if (self.client_socket.hasPendingDatagrams()):
            self.connection_socket = self.client_socket.readDatagram(2048)
            self.server_port = self.connection_socket[0]
            self.server_port = int(self.server_port[4:].decode())
            print('port: ' + str(self.server_port))
            self.conn_est.emit()
