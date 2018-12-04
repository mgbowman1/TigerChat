import struct

def byte_to_int(data, offset):
	return struct.unpack("<i", data[offset:offset + 4])[0]

def int_to_byte(num):
	return struct.pack("<i", num)

def merge_bytes(byte_arr):
	final = b''
	for b in byte_arr:
		final += b
	return final

def split_bytes_data(data, size):
	final = []
	for i in range(len(data) // size):
		final.append(data[size * i: size * (i + 1)])
	if len(data) % size > 0:
		start = len(data) // size
		start *= size
		final.append(data[start:])
	return final