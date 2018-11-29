import sys
import os
from datetime import datetime as dt
from PyQt5.QtWidgets import (QApplication, QLineEdit, QMainWindow, QPushButton,
                             QHBoxLayout, QDockWidget, QLabel, QMessageBox,
                             QListWidget, QTextEdit, QVBoxLayout, QWidget)
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QIcon
from message_thread import MessageWorker


class chatHistory(QTextEdit):
    def __init__(self, parent=None):
        super(chatHistory, self).__init__(parent)
        self.last_msg = dt.today()


class chatWindow(QMainWindow):
    def __init__(self, parent=None):
        super(chatWindow, self).__init__(parent)
        script_dir = sys.path[0]
        self.img_path = os.path.join(script_dir, './resources/tiger.png')
        self.setWindowIcon(QIcon(self.img_path))
        self.title = 'TigerChat'
        self.left = 200
        self.top = 100
        self.width = 600
        self.height = 600
        window = self
        window.setObjectName("window")
        self.setStyleSheet("#window { background-image: url(Tiger Pattern.png); background-attachment: fixed;}")
        self.initUI()

    def initUI(self):
        self.statusBar().showMessage('Connecting...')
        self.setGeometry(self.left, self.top, self.width, self.height)
        self.setWindowTitle(self.title)
        self.centralWidget = QWidget()
        self.centralWidget.setMinimumWidth(200)
        self.centralWidget.setMinimumHeight(200)
        self.setCentralWidget(self.centralWidget)

        # Widgets
        self.conversation_partner = QLabel(self)
        self.conversation_partner.setText("Pick a convesation partner")
        self.conversationHistory = chatHistory(self)
        self.conversationHistory.setReadOnly(True)
        self.textbox = QLineEdit(self)
        self.textbox.resize(260, 40)
        self.textbox.setMinimumWidth(100)
        self.textbox.setMaxLength(2000)
        self.sendBtn = QPushButton(self)
        self.sendBtn.setText('SEND')
        self.sendBtn.clicked.connect(self.send_clicked)

        # Layout
        self.v_box = QVBoxLayout()
        self.v_box.setContentsMargins(15, 15, 15, 15)
        self.v_box.addWidget(self.conversation_partner)
        self.v_box.addWidget(self.conversationHistory)
        self.h_box = QHBoxLayout()
        self.h_box.addWidget(self.textbox)
        self.h_box.addWidget(self.sendBtn)
        self.v_box.addLayout(self.h_box)

        # Conversations Dock
        self.conversations = QDockWidget('Conversations', self)
        self.conversation_list = QListWidget()
        self.conversation_list.addItem('No conversations to show.')
        self.conversations.setWidget(self.conversation_list)
        self.conversations.setFloating(False)
        self.conversations.setFeatures(QDockWidget.NoDockWidgetFeatures)
        self.addDockWidget(Qt.LeftDockWidgetArea, self.conversations)
        self.centralWidget.setLayout(self.v_box)

        # start all
        self.textbox.setFocus()
        self.show()
        # self.m_thread = MessageWorker()
        # self.m_thread.start()
        # self.m_thread.conn_est.connect(self.connection_established)

    def send_clicked(self):
        msg = self.textbox.text()
        if (len(msg) > 0):
            if (len(msg) >= 2000):
                self.alert = QMessageBox()
                self.alert.setWindowTitle("Text too long.")
                self.alert.setIcon(QMessageBox.Warning)
                self.alert.setWindowIcon(QIcon(self.img_path))
                self.alert.setText("Please input less than 2000 characters.")
                self.alert.exec_()
            else:
                # if mesg within 5 min, no new time stamp
                now = dt.today()
                diff = now - self.conversationHistory.last_msg
                if ((diff.total_seconds() * 1000) > 30000):
                    nowfmt = dt.today().strftime('%d, %b %Y at %H:%M')
                    self.conversationHistory.append('\n' + nowfmt)
                    self.conversationHistory.setAlignment(Qt.AlignCenter)
                self.conversationHistory.append('you: ' + msg)
                self.conversationHistory.setAlignment(Qt.AlignLeft)
                self.conversationHistory.last_msg = now
                self.textbox.clear()
                self.textbox.setFocus()
                self.statusBar().showMessage("Sending message")
                self.m_thread.sendMessage(msg)
                self.m_thread.msg_sent.connect(self.message_delivered)

    def keyPressEvent(self, QKeyEvent):
        if (QKeyEvent.key() == Qt.Key_Return):
            self.send_clicked()

    def connection_established(self):
        self.statusBar().showMessage("Connected.")

    def message_delivered(self):
        self.statusBar().showMessage("Message delivered.", 3000)


def start():
    app = QApplication(sys.argv)
    app.setStyle('Fusion')
    w = chatWindow()
    sys.exit(app.exec_())


if __name__ == '__main__':
    start()
