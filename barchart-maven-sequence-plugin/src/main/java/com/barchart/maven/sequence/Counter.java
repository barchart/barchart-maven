package com.barchart.maven.sequence;

public class Counter {

	private long start = 0;

	private long finish = Long.MAX_VALUE;

	private long increment = 1;

	private String name;

	public String getName() {
		return name;
	}

	public String getValueStringWithIncrement(String counterStringValue) {

		// check start, finish, increment - for consistency
		if (increment > 0) {
			if (start >= finish) {
				throw new IllegalArgumentException(
						"increment > 0, but start >= finish");
			}
		} else if (increment < 0) {
			if (start <= finish) {
				throw new IllegalArgumentException(
						"increment < 0, but start <= finish");
			}
		} else {
			throw new IllegalArgumentException("increment == 0");
		}

		long counterValue = Long.parseLong(counterStringValue);

		// reset invalid value before increment
		counterValue = boundToRange(counterValue);

		counterValue += increment;

		// verify counter value after increment; reset when end of range
		counterValue = boundToRange(counterValue);

		counterStringValue = Long.toString(counterValue);

		return counterStringValue;

	}

	private long boundToRange(long value) {
		if (increment > 0) {
			if (value < start || finish < value) {
				value = start;
			}
		} else {
			if (value < finish || start < value) {
				value = start;
			}
		}
		return value;
	}

	public String getStartValueString() {
		return Long.toString(start);
	}

}
