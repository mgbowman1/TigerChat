package util;

public class Utility {

	public static int byteToInt(byte[] arr, int offset) {
		int total = 0;
		for (int i = 0; i < 4; i++) {
			total += arr[i + offset] << 8 * i;
		}
		return total;
	}
	
	public static byte[] intToByte(int num) {
		byte[] b = new byte[4];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) (num >>> 8 * i);
		}
		return b;
	}
	
	public static byte[] mergeBytes(byte[][] bytes) {
		int lenSum = 0;
		int totalLen = 0;
		for (int i = 0; i < bytes.length; i++) {
			lenSum += bytes[i].length;
		}
		byte[] ret = new byte[lenSum];
		for (int i = 0; i < bytes.length; i++) {
			for (int j = 0; j < bytes[i].length; j++) {
				ret[j + totalLen] = bytes[i][j];
			}
			totalLen += bytes[i].length;
		}
		return ret;
	}
	
	public static byte[] mergeBytes(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i];
			if (i < b.length) c[i + a.length] = b[i];
		}
		if (b.length > a.length) {
			for (int i = a.length; i < b.length; i++) {
				c[i + a.length] = b[i];
			}
		}
		return c;
	}
	
	public static byte[] splitBytes(byte[] arr, int offset) {
		byte[] ret = new byte[arr.length - offset];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = arr[i + offset];
		}
		return ret;
	}
	
}
