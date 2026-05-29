package com.example.ams.domain.study;

public enum GaugeLevel {
	GREEN,
	ORANGE,
	RED;

	public static GaugeLevel fromOverallPercent(int percent) {
		if (percent >= 75) {
			return GREEN;
		}
		if (percent >= 50) {
			return ORANGE;
		}
		return RED;
	}
}
