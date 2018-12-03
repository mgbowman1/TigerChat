import utility
import TTP_packet

MAX_INTEGER = 2147483647

current_sequence_number = 1


def get_next_sequence_number():
    global current_sequence_number
    global MAX_INTEGER
    if current_sequence_number == MAX_INTEGER:
        current_sequence_number = 1
        return MAX_INTEGER
    else:
        current_sequence_number += 1
        return current_sequence_number - 1


def reset_sequence_if_larger(size):
    global current_sequence_number
    global MAX_INTEGER
    if current_sequence_number + size > MAX_INTEGER:
        current_sequence_number = 1


class RDPDatagram():
    def __init__(self, bytes_arr=[], acknowledgement=0, head=0, tail=0, frag_distance=None, packet=None):
        if len(bytes_arr) > 0:
            self.sequence = utility.byte_to_int(bytes_arr, 0)
            self.acknowledgement = utility.byte_to_int(bytes_arr, 4)
            self.head = utility.byte_to_int(bytes_arr, 8)
            self.tail = utility.byte_to_int(bytes_arr, 12)
            self.data = utility.byte_to_int(bytes_arr, 16)
        else:
            self.sequence = get_next_sequence_number()
            self.acknowledgement = acknowledgement
            if packet is None:
                self.data = b''
            else:
                self.data = packet.get_bytes()
            if frag_distance is None:
                self.head = head
                self.tail = tail
            else:
                self.head = self.sequence
                self.tail = self.head + frag_distance

    def get_bytes(self):
        return utility.int_to_byte(self.sequence)
        + utility.int_to_byte(self.acknowledgement)
        + utility.int_to_byte(self.head)
        + utility.int_to_byte(self.tail)
        + self.data

    def get_TTP_packet(self):
        return TTP_packet(self.data)