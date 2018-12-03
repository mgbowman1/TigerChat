import utility
import struct

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
    def __init__(self, bytes_arr=b'', flag=0, sender_id="", conversation_id="", timestamp="", message="", file_name="", size=0, message_block_number=0, file_id=0, received_percentage=0, username="", password="", username_arr=[]):
        if len(bytes_arr) > 0:
            self.flag = struct.unpack("<B", bytes_arr[0])
            self.data = bytes_arr[1:]
        else:
            self.flag = flag
            global FLAG_TYPE
            s = FLAG_TYPE[self.flag]
            