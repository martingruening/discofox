package eu.gruning.discofox.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class Discotime {
	private static final String timezone = "Europe/Berlin";

	public static long beginOf(LocalDate date) {
		return LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli();
	}

	public static long endOf(LocalDate date) {
		return LocalDateTime.of(date, LocalTime.MAX).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli();
	}

	public static LocalTime LocalTimeGermany() {
		return LocalTime.now(ZoneId.of(timezone));
	}
}