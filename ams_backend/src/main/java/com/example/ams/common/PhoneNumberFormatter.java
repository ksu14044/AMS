package com.example.ams.common;

public final class PhoneNumberFormatter {

	private PhoneNumberFormatter() {
	}

	public static String digitsOnly(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll("\\D", "");
	}

	public static String format(String value) {
		String d = digitsOnly(value);
		if (d.isEmpty()) {
			return "";
		}

		if (d.startsWith("02")) {
			if (d.length() <= 2) {
				return d;
			}
			if (d.length() <= 5) {
				return d.substring(0, 2) + "-" + d.substring(2);
			}
			if (d.length() <= 9) {
				return d.substring(0, 2) + "-" + d.substring(2, 5) + "-" + d.substring(5);
			}
			return d.substring(0, 2) + "-" + d.substring(2, 6) + "-" + d.substring(6, Math.min(10, d.length()));
		}

		if (d.matches("^01[016789].*")) {
			if (d.length() <= 3) {
				return d;
			}
			if (d.length() <= 7) {
				return d.substring(0, 3) + "-" + d.substring(3);
			}
			return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7, Math.min(11, d.length()));
		}

		if (d.startsWith("0")) {
			if (d.length() <= 3) {
				return d;
			}
			if (d.length() <= 6) {
				return d.substring(0, 3) + "-" + d.substring(3);
			}
			if (d.length() <= 10) {
				return d.substring(0, 3) + "-" + d.substring(3, 6) + "-" + d.substring(6);
			}
			return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7, Math.min(11, d.length()));
		}

		if (d.length() <= 4) {
			return d;
		}
		if (d.length() <= 8) {
			return d.substring(0, 4) + "-" + d.substring(4);
		}
		return d.substring(0, 4) + "-" + d.substring(4, 8) + "-" + d.substring(8, Math.min(12, d.length()));
	}
}
