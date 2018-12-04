import utility
import struct
from datetime import datetime

FLAG_TYPE = [
    "MSG",
    "FIL",
    "INF",
    "RQM",
    "FRG",
    "CON",
    "CLS",
    "CCV"
]


class TTP_packet():
    def __init__(self, bytes_arr=b'', flag=0, sender_id="", conversation_id="", message="", file_name="", size=0, message_block_number=0, file_id=0, received_percentage=0, username="", password="", username_arr=[]):
        self.DELIM = '|'.encode("utf-8")
        timestamp = str(datetime.now())[:-7]
        if len(bytes_arr) > 0:
            self.flag = struct.unpack("<B", bytes_arr[0:1])[0]
            self.data = bytes_arr[1:]
        else:
            self.flag = flag
            global FLAG_TYPE
            s = FLAG_TYPE[self.flag]
            if s == "MSG":
                self.data = b"".join([
                                    sender_id.encode("utf-8"),
                                    self.DELIM,
                                    conversation_id.encode("utf-8"),
                                    self.DELIM,
                                    timestamp.encode("utf-8"),
                                    self.DELIM,
                                    message.encode("utf-8")])
            elif s == "FIL":
                self.data = b"".join([
                                    conversation_id.encode("utf-8"),
                                    self.DELIM,
                                    str(size).encode(),
                                    self.DELIM,
                                    file_name.encode(),
                                    self.DELIM,
                                    message.encode("utf-8")])
            elif s == "INF":
                self.data = b"".join([
                                    sender_id.encode("utf-8"),
                                    self.DELIM,
                                    conversation_id.encode("utf-8"),
                                    self.DELIM,
                                    timestamp.encode("utf-8"),
                                    self.DELIM,
                                    utility.int_to_byte(size),
                                    self.DELIM,
                                    file_name.encode("utf-8")])
            elif s == "RQM":
                self.data = b"".join([
                                    conversation_id.encode("utf-8"),
                                    self.DELIM,
                                    str(message_block_number).encode()])
            elif s == "FRG":
                self.data = b"".join([
                                    str(file_id).encode(),
                                    self.DELIM,
                                    str(received_percentage).encode()])
            elif s == "CON":
                self.data = b"".join(
                                    [
                                        username.encode("utf-8"),
                                        self.DELIM,
                                        password.encode("utf-8")])
            elif s == "CCV":
                self.data = username_arr[0]
                for i in range(1,len(username_arr)):
                    self.data += "," + username_arr[i]
                self.data = self.data.encode("utf-8")

    def get_bytes(self):
        return struct.pack("<B", self.flag) + self.data