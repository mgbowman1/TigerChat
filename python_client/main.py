import sys
import os
from datetime import datetime as dt
from PyQt5.QtWidgets import (QApplication, QLineEdit, QMainWindow, QPushButton,
                             QHBoxLayout, QDockWidget, QLabel, QMessageBox,
                             QListWidget, QTextEdit, QVBoxLayout, QWidget,
                             QInputDialog, QFileDialog, QAction)
from PyQt5.QtCore import Qt
from PyQt5.QtGui import QIcon
from message_worker import MessageWorker
import time


class chatHistory(QTextEdit):
    def __init__(self, parent=None):
        super(chatHistory, self).__init__(parent)
        self.last_msg = dt.today()
        self.setStyleSheet("background-color: #ffcc80; color: black; font-size: 12px")


class chatWindow(QMainWindow):
    def __init__(self, parent=None):
        super(chatWindow, self).__init__(parent)
        favicon_dir = sys.path[0]
        self.fav_path = os.path.join(favicon_dir, './resources/favicon.png')
        self.setWindowIcon(QIcon(self.fav_path))
        self.title = 'TigerChat'
        self.left = 200
        self.top = 100
        self.width = 600
        self.height = 600
        window = self
        window.setObjectName("window")
        self.setStyleSheet("#window { background-image: url(./resources/TigerStripe.svg); background-attachment: fixed;}")
        self.initUI()

    def initUI(self):
        self.statusBar().setStyleSheet("color: white; font-weight: bold; font-size: 14px")
        self.statusBar().showMessage('Connecting...')
        self.login_action = QAction("Login")
        self.login_action.triggered.connect(self.login_window)
        self.menubar = self.menuBar()
        self.options_menu = self.menubar.addMenu("Options")
        self.options_menu.addAction(self.login_action)
        self.setGeometry(self.left, self.top, self.width, self.height)
        self.setWindowTitle(self.title)
        self.central_widget = QWidget()
        self.central_widget.setMinimumWidth(200)
        self.central_widget.setMinimumHeight(200)
        self.setCentralWidget(self.central_widget)

        # Widgets
        self.conversation_partner = QLabel(self)
        self.conversation_partner.setText("Pick a convesation partner")
        self.conversation_partner.setStyleSheet("color: white; font-weight: bold; font-size: 14px")
        self.conversation_history = chatHistory(self)
        self.conversation_history.setReadOnly(True)
        self.text_box = QLineEdit(self)
        self.text_box.resize(260, 40)
        self.text_box.setMinimumWidth(100)
        self.text_box.setMaxLength(2000)
        self.text_box.textEdited.connect(self.resize_editor)
        self.sendButton = QPushButton(self)
        self.sendButton.setText('SEND')
        self.sendButton.clicked.connect(self.send_clicked)
        self.file_button = QPushButton(self)
        self.file_button.setText("UPLOAD FILE")
        self.file_button.clicked.connect(self.file_upload_clicked)

        # Layout
        self.v_box = QVBoxLayout()
        self.v_box.setContentsMargins(15, 15, 15, 15)
        self.v_box.addWidget(self.conversation_partner)
        self.v_box.addWidget(self.conversation_history)
        self.h_box = QHBoxLayout()
        self.h_box.addWidget(self.text_box)
        self.input_layout = QVBoxLayout()
        self.input_layout.addWidget(self.sendButton)
        self.input_layout.addWidget(self.file_button)
        self.h_box.addLayout(self.input_layout)
        self.v_box.addLayout(self.h_box)

        # Conversations Dock
        self.conversations = QDockWidget('', self)
        self.conversations.setFixedWidth(self.width * .3)
        self.dock_box = QWidget()
        self.dock_layout = QVBoxLayout()
        self.dock_layout.setContentsMargins(15, 15, 15, 15)
        self.dock_label = QLabel(self.conversations)
        self.dock_label.setText("Conversations")
        self.dock_label.setStyleSheet("color: white; font-weight: bold; font-size: 14px;")
        self.conversation_list = QListWidget()
        self.conversation_list.addItem('No conversations to show.')
        self.conversation_list.setStyleSheet("background-color: #ffcc80")
        self.dock_layout.addWidget(self.conversation_list)
        self.dock_box.setLayout(self.dock_layout)
        self.conversations.setWidget(self.dock_box)
        self.conversations.setFloating(False)
        self.conversations.setFeatures(QDockWidget.NoDockWidgetFeatures)
        self.addDockWidget(Qt.LeftDockWidgetArea, self.conversations)
        self.central_widget.setLayout(self.v_box)

        # start all
        self.text_box.setFocus()
        self.show()
        self.login_window()
        self.m_thread = MessageWorker(self)
        self.m_thread.start()
        time.sleep(.5)
        self.m_thread.connect_to_server(self.username_string, self.password_string)
        self.m_thread.conn_est.connect(self.connection_established)
        self.m_thread.msg_received.connect(self.message_received)

    def send_clicked(self):
        msg = self.text_box.text()
        if (len(msg) > 0):
            if (len(msg) >= 2000):
                self.alert = QMessageBox()
                self.alert.setWindowTitle("Text too long.")
                self.alert.setIcon(QMessageBox.Warning)
                self.alert.setWindowIcon(QIcon(self.fav_path))
                self.alert.setText("Please input less than 2000 characters.")
                self.alert.exec_()
            else:
                # if mesg within 5 min, no new time stamp
                now = dt.today()
                diff = now - self.conversation_history.last_msg
                if ((diff.total_seconds() * 1000) > 300000):
                    nowfmt = dt.today().strftime('%d, %b %Y at %H:%M')
                    self.conversation_history.append('\n' + nowfmt)
                    self.conversation_history.setAlignment(Qt.AlignCenter)
                self.conversation_history.append('you: ' + msg)
                self.conversation_history.setAlignment(Qt.AlignLeft)
                self.conversation_history.last_msg = now
                self.text_box.clear()
                self.text_box.setFocus()
                self.statusBar().showMessage("Sending message")
                self.m_thread.send_message(msg)
                self.m_thread.msg_sent.connect(self.message_delivered)

    def keyPressEvent(self, QKeyEvent):
        if (QKeyEvent.key() == Qt.Key_Return):
            self.send_clicked()

    def resize_editor(self):
        self.line_length = len(self.text_box.text())
        if (self.line_length > 47 and self.text_box.height() < 80):
            self.text_box.resize(260, 80)

    def connection_established(self):
        self.statusBar().showMessage("Connected")

    def message_received(self, msg):
        sender = msg[0]
        timestamp = msg[1]
        messg = msg[2]

        disp_msg = sender + ': ' + messg
        self.conversation_history.append(timestamp)
        self.conversation_history.append(disp_msg)

    def message_delivered(self):
        self.statusBar().showMessage("Message delivered", 3000)

    def login_window(self):
        self.input_label = "Please enter your username"
        self.username_response = QInputDialog.getText(
                            self, "Login", self.input_label)
        self.username_string = self.username_response[0]
        if (not self.username_response[1] or '|' in self.username_string):
            self.login_error("username")
            return

        self.input_label = "Please enter your password"
        self.password_response = QInputDialog.getText(
                                self, "Password", self.input_label,
                                QLineEdit.Password)
        self.password_string = self.password_response[0]
        if (not self.password_response[1]):
            exit(0)
        if ("|" in self.password_string):
            self.login_error("password")
            return

    def login_error(self, value):
        self.login_button = QPushButton(self)
        self.login_button.setText("Login")
        self.exit_button = QPushButton(self)
        self.exit_button.setText("Exit")

        self.login_error_window = QMessageBox(self)
        self.login_error_window.setWindowTitle("Error")
        self.login_error_window.setIcon(QMessageBox.Warning)
        self.login_error_window.setWindowIcon(QIcon(self.fav_path))
        self.login_error_window.setText("You have entered a wrong " + value)
        self.login_error_window.addButton(self.login_button, QMessageBox.YesRole)
        self.login_error_window.addButton(self.exit_button, QMessageBox.NoRole)
        self.login_error_window.exec_()

        if (self.login_error_window.clickedButton() == self.login_button):
            self.login_window()
        else:
            exit(0)

    def file_upload_clicked(self):
        self.file_name = QFileDialog.getOpenFileName(self, "Choose a file")
        print(self.file_name)


def start():
    app = QApplication(sys.argv)
    app.setStyle('Fusion')
    w = chatWindow()
    sys.exit(app.exec_())


if __name__ == '__main__':
    start()
