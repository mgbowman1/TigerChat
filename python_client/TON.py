import struct

class TON():
    def __init__(self, bytes_arr=b''):
        self.entries = []
        if len(bytes_arr) > 0:
            if bytes_arr[0] == b'[':
                index = 1
                while index < len(bytes_arr):
                    if bytes_arr[index] == b'{':
                        sender_id = b''
                        conversation_id = b''
                        timestamp = b''
                        message = b''
                        index += 1
                        while bytes_arr[index] != b',':
                            sender_id += bytes_arr[index]
                            index += 1
                        index += 1
                        while bytes_arr[index] != b',':
                            conversation_id += bytes_arr[index]
                            index += 1
                        index += 1
                        while bytes_arr[index] != b',':
                            timestamp += bytes_arr[index]
                            index += 1
                        index += 2
                        while bytes_arr[index] != b'"':
                            message += bytes_arr[index]
                            index += 1
                            if bytes_arr[index] == b'"' and bytes_arr[index - 1] == b'\\':
                                message += bytes_arr[index]
                                index += 1
                        self.entries.append(Entry(sender_id, conversation_id, timestamp, message))
                        self.entries[-1].un_escape()
                        index += 3
                    else:
                        break

    def add_entry(self, sender_id, conversation_id, timestamp, message):
        self.entries.append(Entry(sender_id, conversation_id, timestamp, message))

    def get_bytes(self):
        bytes_arr = b'['
        for e in self.entries:
            bytes_arr += b'{' + e.get_bytes() + b'},'
        bytes_arr[-1] = b']'
        return bytes_arr


class Entry():
    def __init__(self, sender_id, conversation_id, timestamp, message):
        self.sender_id = sender_id
        self.conversation_id = conversation_id
        self.timestamp = timestamp
        self.message = message

    def get_bytes(self):
        return self.sender_id.encode("utf-8")
        + b','
        + self.conversation_id.encode("utf-8")
        + b','
        + self.timestamp.encode("utf-8")
        + b','
        + self.escape(self.message).encode("utf-8")

    def escape(self, s):
        return s.replace("\\", "\\\\").replace("\"", "\\\"")

    def un_escape(self):
        self.message = self.message.replace("\\\\", "\\").replace("\\\"", "\"")