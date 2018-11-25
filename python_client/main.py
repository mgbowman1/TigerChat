import sys
from datetime import datetime as dt
from PyQt5.QtWidgets import (QApplication, QLineEdit, QLabel, QMainWindow,
                             QPushButton, QHBoxLayout, QDockWidget,
                             QListWidget, QTextEdit, QVBoxLayout, QWidget)
from PyQt5.QtCore import Qt


class chatHistory(QTextEdit):
    def __init__(self, parent=None):
        super(chatHistory, self).__init__(parent)
        last_msg = dt.today()


class chatWindow(QMainWindow):
    def __init__(self, parent=None):
        super(chatWindow, self).__init__(parent)
        self.title = 'TigerChat'
        self.left = 200
        self.top = 100
        self.width = 600
        self.height = 600
        self.initUI()

    def initUI(self):
        self.statusBar().showMessage('Ready')
        self.setGeometry(self.left, self.top, self.width, self.height)
        self.setWindowTitle(self.title)
        self.centralWidget = QWidget()
        self.setCentralWidget(self.centralWidget)

        # Widgets
        self.conversationHistory = chatHistory(self)
        self.textbox = QLineEdit(self)
        self.textbox.resize(260, 40)
        self.sendBtn = QPushButton(self)
        self.sendBtn.setText('SEND')
        self.sendBtn.clicked.connect(self.send_clicked)

        # Layout
        self.v_box = QVBoxLayout()
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

    def send_clicked(self):
        msg = self.textbox.text()
        if (len(msg) > 0):
            now = dt.today()
            # if thismsgtime - lastmsgtime > 5min : add new timestamp
            # if (now - self.textbox.last_msg > )
            now = dt.today().strftime('%d, %b %Y at %H:%M')
            self.textbox.clear()
            self.conversationHistory.append(now)
            self.conversationHistory.append(msg)


def start():
    app = QApplication(sys.argv)
    app.setStyle('Fusion')
    w = chatWindow()
    # w.update()
    sys.exit(app.exec_())


if __name__ == '__main__':
    start()
