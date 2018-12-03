from PyQt5.Core import QThread
from PyQt5.QtNetwork import QUdpSocket, QHostAddress
from datetime import datetime as dt
from collections import deque
import RDP_datagram as RDP
from TTP_packet import TTP_packet
import utility

MAX_PENDING_DATAGRAMS = 210
MAX_DATAGRAM_SIZE = 504-16
SERVERPORT = 11000
HOSTADDRESS = QHostAddress('75.65.192.207')


class DataSocket(QThread):

    _alpha = .125
    _beta = .25
    _estimateRTT = 0
    _devRTT = 0

    def __init__(self, reader):
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
        self.reader = reader

    def run(self):
        while (self._running):
            pending_gram = self._pending_sent_datagrams[self.timeout_datagram_index]
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
            elif (round(dt.now().microsecond / 1000) - pending_gram.time_sent >= self.timeout_time):
                self.timeout()
            while (self._running):
                try:
                    rdp = self.receive()
                    if (rdp is None):
                        break
                    else:
                        if (rdp.acknowledgement > 0):
                            self.process_ack(rdp.acknowledgement)
                        if (len(rdp.data > 0)):
                            self._received_sequences.append(rdp.sequence)
                            if (rdp.head > 0):
                                self.defragment_packet(rdp)
                            else:
                                self.reader.receive(rdp.get_TTPPacket())
                except Exception as e:
                    self.timeout()

    def defragment_packet(self, rdp):
        if (rdp.head in self._fragmented_data):
            self._fragmented_data[rdp.head].add_datagram(rdp)
            if (self._fragmented_data[rdp.head].num_datagrams_needed == 0):
                self.reader.receive(self._fragmented_data[rdp.head].get_TTPPacket())
        else:
            self._fragmented_data[rdp.head] = FragmentedDatagram(rdp.head, rdp.tail)
            self._fragmented_data[rdp.head].add_datagram(rdp)

    def timeout(self):
        self._send_window = 1
        self._ssthresh /= 2
        self._ssthresh = int(self._ssthresh)
        self.numtimeouts += 1
        if (self.numtimeouts == 3):
            check = self.reader.reconnect()
            if (check == 1):
                self.close()
            elif (check == -1):
                self.reset()

    def process_ack(self, ack_number):
        index = 0
        for pending_gram in self._pending_sent_datagrams:
            if (pending_gram.datagram.sequence == ack_number):
                self._pending_sent_datagrams.remove(pending_gram)
                if (index == self.timeout_datagram_index):
                    self.timeout_datagram_index += 1
                if (self.timeout_datagram_index >= len(self._pending_sent_datagrams)):
                    self.timeout_datagram_index = 0
                if (index == self.timeout_watch_index):
                    self.calculate_timeout(round(dt.now().microsecond / 1000) - pending_gram.time_sent)
                index = 0
                for pending_gram in self._pending_sent_datagrams:
                    if(pending_gram.datagram.sequence < ack_number):
                        pending_gram.dup_ack += 1
                        if (pending_gram.dup_ack == 3):
                            self.resend(pending_gram)
                            if (index == self.timeout_datagram_index):
                                self.timeout_datagram_index += 1
                            if (self.timeout_datagram_index == len(self._pending_sent_datagrams)):
                                self.timeout_datagram_index = 0
                            if (index == self.timeout_watch_index):
                                self.timeout_watch_index += 1
                        index += 1
                    else:
                        break
                break
            index += 1

    def calculate_timeout(self, sample):
        self._estimateRTT = (1 - self._alpha) * self._estimateRTT + self._alpha * sample
        self._devRTT = (1 - self._beta) * self._devRTT + self._beta * abs(sample - self._estimateRTT)
        self.timeout_time = self._estimateRTT + 4 * self._devRTT

    def receive(self):
        buffer = bytearray(512)
        if not self.socket.hasPendingDatagrams():
            return None
        else:
            self.socket.readDatagram(buffer, 512)
            return RDP.RDP_datagram(buffer)

    def _send_protocol(self):  # send the current window
        numsent = 0
        self.timeout_datagram_index = 0
        self.timeout_watch_index = 0

        while (len(self._datagram_send_list) > 0 and numsent < self._send_window):
            r = self._datagram_send_list.popleft()
            self.send(r)
            self.numtimeouts = 0
            self._pending_sent_datagrams.append(PendingDatagram(r, 0, round(dt.now().microsecond / 1000)))
            numsent += 1
        while (numsent < self._send_window and len(self._received_sequences) > 0):
            seq = self._received_sequences.popleft()
            real_seq = abs(seq)
            r = RDP.RDP_datagram(acknowledgement=real_seq)
            self.send(r)
            if seq < 0:
                self._pending_sent_datagrams.append(PendingDatagram(r, 0, round(dt.now().microsecond / 1000)))

    def send(self, datagram):  # send a datagram
        bites = datagram.get_bytes()
        self.socket.writeDatagram(bites, HOSTADDRESS, SERVERPORT)

    def add_send(self, ttp):
        bites = ttp.get_bytes()
        if (len(bites) > MAX_DATAGRAM_SIZE):
            data = utility.split_bytes_data(bites, MAX_DATAGRAM_SIZE - 1)
            RDP.reset_sequence_if_larger(len(data))
            for i in range(len(data)):
                t = TTP_packet(data[i])
                ack = 0
                if len(self._received_sequences) > 0:
                    ack = self._received_sequences.popleft()
                r = RDP.RDP_datagram(acknowledgement=ack, frag_distance=len(data), packet=t)
                self._datagram_send_list.append(self.r)
        else:
            ack = 0
            if len(self._received_sequences) > 0:
                ack = self._received_sequences.popleft()
            self._datagram_send_list.append(RDP.RDP_datagram(acknowledgement=ack, packet=ttp))

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
        bites = rdp.get_data()
        if (rdp.sequence == rdp.tail and len(bites) < MAX_DATAGRAM_SIZE):
            data_len = len(self.data) - (MAX_DATAGRAM_SIZE - len(bites))
            self.data = self.data[:data_len]
        num = rdp.sequence - rdp.head
        self.data[num:num + len(bites)] = bites

    def get_TTPPacket(self):
        return TTP_packet(self.data)
