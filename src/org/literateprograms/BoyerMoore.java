/* Copyright (c) 2012 the authors listed at the following URL, and/or
the authors of referenced articles or incorporated external code:
http://en.literateprograms.org/Boyer-Moore_string_search_algorithm_(Java)?action=history&offset=20100924202517

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Retrieved from: http://en.literateprograms.org/Boyer-Moore_string_search_algorithm_(Java)?oldid=16950

Modified by Andrey Novikov
 */

package org.literateprograms;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class BoyerMoore
{
	private Map<Byte, Integer> mRightMostIndexes;
	private byte[] mPattern;

	public BoyerMoore(byte[] pattern)
	{
		if (pattern == null)
			throw new NullPointerException("Pattern can not be null");
		if (pattern.length == 0)
			throw new IllegalArgumentException("Pattern can not be empty");

		mPattern = pattern;
		mRightMostIndexes = preprocessForBadByteShift(pattern);
	}

	public List<Integer> match(byte[] buffer)
	{
		return match(mPattern, buffer, 0, -1, mRightMostIndexes);
	}

	public List<Integer> match(byte[] buffer, int offset, int length)
	{
		return match(mPattern, buffer, offset, length, mRightMostIndexes);
	}

	public static List<Integer> match(byte[] pattern, byte[] buffer)
	{
		Map<Byte, Integer> rightMostIndexes = preprocessForBadByteShift(pattern);
		return match(pattern, buffer, 0, -1, rightMostIndexes);
	}

	private static List<Integer> match(byte[] pattern, byte[] buffer, int offset, int length, Map<Byte, Integer> rightMostIndexes)
	{
		if (buffer == null)
			throw new NullPointerException("Buffer can not be null");

		List<Integer> matches = new ArrayList<Integer>();
		int m = length > 0 ? length : buffer.length;
		int n = pattern.length;

		int alignedAt = offset;
		while (alignedAt + (n - 1) < m)
		{
			for (int indexInPattern = n - 1; indexInPattern >= 0; indexInPattern--)
			{
				int indexInBuffer = alignedAt + indexInPattern;
				byte x = buffer[indexInBuffer];
				byte y = pattern[indexInPattern];
				if (indexInBuffer >= m)
					break;
				if (x != y)
				{
					Integer r = rightMostIndexes.get(x);
					if (r == null)
					{
						alignedAt = indexInBuffer + 1;
					}
					else
					{
						int shift = indexInBuffer - (alignedAt + r);
						alignedAt += shift > 0 ? shift : 1;
					}
					break;
				}
				else if (indexInPattern == 0)
				{
					matches.add(alignedAt);
					alignedAt++;
				}
			}
		}
		return matches;
	}

	private static Map<Byte, Integer> preprocessForBadByteShift(byte[] pattern)
	{
		Map<Byte, Integer> map = new HashMap<Byte, Integer>();
		for (int i = pattern.length - 1; i >= 0; i--)
		{
			byte b = pattern[i];
			if (!map.containsKey(b))
				map.put(b, i);
		}
		return map;
	}
}
