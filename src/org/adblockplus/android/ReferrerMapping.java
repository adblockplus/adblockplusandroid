/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2015 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.android;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReferrerMapping
{
  private static class MappingCache extends LinkedHashMap<String, String>
  {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SIZE = 5000;

    public MappingCache()
    {
      super(MAX_SIZE + 1, 0.75f, true);
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, String> eldest)
    {
      return size() > MAX_SIZE;
    }
  };

  private final MappingCache mappingCache = new MappingCache();

  public void add(String url, String referrer)
  {
    mappingCache.put(url, referrer);
  }

  public List<String> buildReferrerChain(String url)
  {
    final List<String> referrerChain = new ArrayList<String>();
    // We need to limit the chain length to ensure we don't block indefinitely
    // if there's a referrer loop.
    final int maxChainLength = 10;
    for (int i = 0; i < maxChainLength && url != null; i++)
    {
      referrerChain.add(0, url);
      url = mappingCache.get(url);
    }
    return referrerChain;
  }
}
