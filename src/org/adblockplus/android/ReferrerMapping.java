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

  public void add(String fullUrl, String referrer)
  {
    mappingCache.put(fullUrl, referrer);
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
