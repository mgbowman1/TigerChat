from PyQt5.Core import QThread
from PyQt5.QtNetwork import QUdpSocket, QHostAddress
from datetime import datetime as dt
from collections import deque
import RDPDatagram as RDP

MAX_PENDING_DATAGRAMS = 210
MAX_DATAGRAM_SIZE = 504-16
SERVERPORT = 11000
HOSTADDRESS = QHostAddress('75.65.192.207')


class DataSocket(QThread):

    _alpha = .125
    _beta = .25
    _estimateRTT = 0
    _devRTT = 0

    def __init__(self):
        QThread.__init__(self)
        self._ssthresh = MAX_PENDING_DATAGRAMS / 2
        self._pending_sent_datagrams = deque([])
        self._datagram_send_list = deque([])
        self._received_sequences = deque([])
        self._fragmented_data = dict()
        self._running = True
        self._send_window = 1
        self.timeout_time = 1000
        self.numtimeouts = 0
        self.timeout_datagram_index = 0
        self.socket = QUdpSocket()
        self.reader = Processor()

    def run(self):
        while (self._running):
            self.pending_index = self._pending_sent_datagrams.index(self.timeout_datagram_index)
            self.pending_gram = self._pending_sent_datagrams[self.pending_index]
            if (len(self._pending_sent_datagrams) == 0):
                try:
                    self._send_protocol()
                    if (self._send_window < self._ssthresh):
                        self._send_window *= 2
                    else:
                        self._send_window += 1
                    if (self._send_window > MAX_PENDING_DATAGRAMS):
                        self._send_window = MAX_PENDING_DATAGRAMS
                except Exception as e:
                    self.timeout()
            elif (round(dt.now().microsecond / 1000) - self.pending_gram.time_sent >= self.timeout_time):
                self.timeout()
            while (self._running):
                try:
                    self.rdp = self.receive()
                    if (self.rdp is None):
                        break
                    else:
                        if (self.rdp.acknowledgement > 0):
                            self.process_ack(self.rdp.acknowledgement)
                        if (len(self.rdp.data > 0)):
                            self._received_sequences.append(self.rdp.sequence)
                            if (self.rdp.head > 0):
                                self.defragment_packet(self.rdp)
                            else:
                                self.reader.receive(self.rdp.get_TTPPacket())
                                if (isinstance(self.reader, Client)):
                                    break
                except Exception as e:
                    self.timeout()

    def defragment_packet(self, rdp):
        if (rdp.head in self._fragmented_data):
            self._fragmented_data.get(rdp.head).add_datagram(rdp)
            if (self._fragmented_data.get(rdp.head).num_datagrams_needed == 0):
                self.reader.receive(self._fragmented_data.get(rdp.head).get_TTPPacket())
        else:
            self._fragmented_data[rdp.head, FragmentedDatagram(rdp.head, rdp.tail)]
            self._fragmented_data.get(rdp.head).add_datagram(rdp)

    def timeout(self):
        self._send_window = 1
        self._ssthresh /= 2
        self._ssthresh = int(self._ssthresh)
        self.numtimeouts += 1
        if (self.numtimeouts == 3):
            self.check = self.reader.reconnect()
            if (self.check == 1):
                self.close()
            elif (self.check == -1):
                self.reset()

    def process_ack(self, ack_number):
        self.index = 0
        for pending_gram in self._pending_sent_datagrams:
            if (pending_gram.datagram.sequence == ack_number):
                self._pending_sent_datagrams.remove(pending_gram)
                if (self.index == self.timeout_datagram_index):
                    self.timeout_datagram_index += 1
                if (self.timeout_datagram_index >= len(self._pending_sent_datagrams)):
                    self.timeout_datagram_index = 0
                if (self.index == self.timeout_watch_index):
                    self.calculate_timeout(round(dt.now().microsecond / 1000) - pending_gram.time_sent)
                self.index = 0
                for pending_gram in self._pending_sent_datagrams:
                    if(pending_gram.datagram.sequence < ack_number):
                        pending_gram.dup_ack += 1
                        if (pending_gram.dup_ack == 3):
                            self.resend(pending_gram)
                            if (self.index == self.timeout_datagram_index):
                                self.timeout_datagram_index += 1
                            if (self.timeout_datagram_index == len(self._pending_sent_datagrams)):
                                self.timeout_datagram_index = 0
                            if (self.index == self.timeout_watch_index):
                                self.timeout_watch_index += 1
                        self.index += 1
                    else:
                        break
                break
            self.index += 1

    def calculate_timeout(self, sample):
        global ALPHA, BETA
        self._estimateRTT = (1 - ALPHA) * self._estimateRTT + ALPHA * sample
        self._devRTT = (1 - BETA) * self._devRTT + BETA * abs(sample - self._estimateRTT)
        self.timeout_time = self._estimateRTT + 4 * self._devRTT

    def receive(self):
        self.buffer = bytearray(512)
        try:
            self.socket.waitForReadyRead()
            self.datagram_size = self.socket.pendingDatagramSize()
            self.d = self.socket.readDatagram(self.datagram_size)
            return RDP.RDPDatagram(self.d[0])
        except Exception as e:
            return None

    def _send_protocol(self):  # send the current window
        self.d = round(dt.now().microsecond / 1000)
        self.numsent = 0
        self.timeout_datagram_index = 0
        self.timeout_watch_index = 0

        while (len(self._datagram_send_list) > 0 and self.numsent < self._send_window):
            self.r = self._datagram_send_list[0]
            self.send(self.r)
            self.numtimeouts = 0
            self._pending_sent_datagrams.append(PendingDatagram(self.r, 0, self.d))
            self.numsent += 1
        while (self.numsent < self._send_window and len(self._received_sequences) > 0):
            self.r = RDP.RDPDatagram(self._received_sequences[0], 0, 0, None)
            self.send(self.r)
            self._pending_sent_datagrams.append(PendingDatagram(self.r, 0, self.d))

    def send(self, datagram):  # send a datagram
        self.bites = datagram.get_bytes()
        self.socket.writeDatagram(self.bites, HOSTADDRESS, SERVERPORT)

    def add_send(self, ttp):
        self.bites = ttp.get_bytes()
        if (len(self.bites) > MAX_DATAGRAM_SIZE):
            self.data = Utility.split_bytes_data(self.bites, MAX_DATAGRAM_SIZE - 1)
            RDP.reset_sequence_if_larger(len(self.data))
            for i in range(len(self.data)):
                self.t = TTPPacket(self.data[i])
                self.ack = self._received_sequences[0]
                if (self.ack == None):
                    self.ack = 0
                self.r = RDP.RDPDatagram(self.ack, len(self.data), self.t)
                self._datagram_send_list.append(self.r)
        else:
            self.ack = self._received_sequences[0]
            if (self.ack == None):
                self.ack = 0
            self._datagram_send_list.append(RDP.RDPDatagram(self.ack, 0, 0, ttp))

    def resend(self, pd):
        pd.dup_ack = 0
        self._send_window /= 2
        self._send_window = int(self._send_window)
        pd.time_sent = round(dt.now().microsecond / 1000)
        while (self._running):
            try:
                self.send(pd.datagram)
                break
            except Exception as e:
                self.timeout()
                try:
                    self.wait(100)
                except Exception as e:
                    raise e

    def reset(self):
        self._datagram_send_list.clear()
        self._fragmented_data.clear()
        self.numtimeouts = 0
        self._pending_sent_datagrams.clear()
        self._received_sequences.clear()
        self._ssthresh = 210
        self.timeout_time = 1000

    def close(self):
        self.socket.close()
        try:
            self.reader.close()
        except Exception as e:
            raise e
        self._running = False


class PendingDatagram():
    def __init__(self, datagram, dup_ack, time_sent):
        self.datagram = datagram
        self.dup_ack = dup_ack
        self.time_sent = time_sent


class FragmentedDatagram():
    def __init__(self, start, end):
        self.num_datagrams_needed = end - start
        self.data = bytearray(self.num_datagrams_needed * MAX_DATAGRAM_SIZE)

    def add_datagram(self, rdp):
        self.num_datagrams_needed -= 1
        self.bites = rdp.get_data()
        if (rdp.sequence == rdp.tail and len(self.bites) < MAX_DATAGRAM_SIZE):
            self.len = len(self.data) - (MAX_DATAGRAM_SIZE - len(self.bites))
            self.data = self.data[:self.len]
        self.data.extend(self.bites)

    def get_TTPPacket(self):
        return TTPPacket(self.data)
