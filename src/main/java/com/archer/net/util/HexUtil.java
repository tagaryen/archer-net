package com.archer.net.util;

public class HexUtil {
	private static int[] radix16 = new int['z' + 1];
	private static char[] hex = new char[] {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	
	static {
		for(int i = '0'; i <= '9'; i++) {
			radix16[i] = i - '0' + 1;
		}
		for(int i = 'a'; i <= 'z'; i++) {
			radix16[i] = i - 'a' + 11;
		}
		for(int i = 'A'; i <= 'Z'; i++) {
			radix16[i] = i - 'A' + 11;
		}
	}
	
	public static String intToHex(long n) {
		String s = "";
		while(n > 0) {
			s = hex[(int)(n % 16)] + s;
			n /= 16;
		}
		return s;
	}
	
	public static int hexToInt(String h) {
		char[] hc = h.toCharArray();
		int n = 0;
		for(int i = hc.length - 1; i > -1; i--) {
			if(hc[i] >= radix16.length || radix16[hc[i]] == 0) {
				throw new IllegalArgumentException("invalid hex " + h);
			}
			n = n * 16 + radix16[hc[i]];
		}
		return n;
	}
	
	public static int bytesToInt(byte[] data, int from, int to) {
		int ret = 0;
		for(int i = from; i < to; i++) {
			int v = radix16[data[i]];
			if(v > 0) {
				ret = ret * 16 + v - 1;
			}
		}
		return ret;
	}
	
	public static byte[] intToBytes(int n) {
		if(n >= (1 << 24)) {
			return new byte[] {(byte) ((n >> 24) & 0xff), (byte) ((n >> 16) & 0xff), (byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
		} else if(n >= (1 << 16)) {
			return new byte[] {(byte) ((n >> 16) & 0xff), (byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
		} else if(n >= (1 << 8)) {
			return new byte[] {(byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
		} else {
			return new byte[] {(byte) (n & 0xff)};
		}
	}
}
