package com.mattprecious.stacker

fun <T> List<T>.radiateFrom(index: Int) = sequence {
	yield(get(index))

	var left = index - 1
	var right = index + 1
	while (left >= 0 || right < size) {
		if (left >= 0) yield(get(left--))
		if (right < size) yield(get(right++))
	}
}
