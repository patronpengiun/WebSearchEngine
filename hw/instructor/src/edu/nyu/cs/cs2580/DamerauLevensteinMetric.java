package edu.nyu.cs.cs2580;

public class DamerauLevensteinMetric {
	public static void main(String[] args) {
		DamerauLevensteinMetric m = new DamerauLevensteinMetric();
		System.out.println(m.getDistance("abcde", "dfd"));
	}
	

	public DamerauLevensteinMetric() {
		this(DEFAULT_LENGTH);
	}

	public DamerauLevensteinMetric(int maxLength) {
		currentRow = new int[maxLength + 1];
		previousRow = new int[maxLength + 1];
		transpositionRow = new int[maxLength + 1];
	}

	public int getDistance(CharSequence first, CharSequence second) {
		int max = -1;
		int firstLength = first.length();
		int secondLength = second.length();

		if (firstLength == 0)
			return secondLength;
		else if (secondLength == 0) return firstLength;

		if (firstLength > secondLength) {
			CharSequence tmp = first;
			first = second;
			second = tmp;
			firstLength = secondLength;
			secondLength = second.length();
		}

		if (max < 0) max = secondLength;
		if (secondLength - firstLength > max) return max + 1;

		if (firstLength > currentRow.length) {
			currentRow = new int[firstLength + 1];
			previousRow = new int[firstLength + 1];
			transpositionRow = new int[firstLength + 1];
		}

		for (int i = 0; i <= firstLength; i++)
			previousRow[i] = i;

		char lastSecondCh = 0;
		for (int i = 1; i <= secondLength; i++) {
			char secondCh = second.charAt(i - 1);
			currentRow[0] = i;

			int from = Math.max(i - max - 1, 1);
			int to = Math.min(i + max + 1, firstLength);

			char lastFirstCh = 0;
			for (int j = from; j <= to; j++) {
				char firstCh = first.charAt(j - 1);

				int cost = firstCh == secondCh ? 0 : 1;
				int value = Math.min(Math.min(currentRow[j - 1] + 1, previousRow[j] + 1), previousRow[j - 1] + cost);

				if (firstCh == lastSecondCh && secondCh == lastFirstCh)
					value = Math.min(value, transpositionRow[j - 2] + cost);

				currentRow[j] = value;
				lastFirstCh = firstCh;
			}
			lastSecondCh = secondCh;

			int tempRow[] = transpositionRow;
			transpositionRow = previousRow;
			previousRow = currentRow;
			currentRow = tempRow;
		}
		return previousRow[firstLength];

	}

	
	private static final int DEFAULT_LENGTH = 255;
	private int[] currentRow;
	private int[] previousRow;
	private int[] transpositionRow;
}
