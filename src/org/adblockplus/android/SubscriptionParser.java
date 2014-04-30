/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2014 Eyeo GmbH
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

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class SubscriptionParser extends DefaultHandler
{
  private static final String SUBSCRIPTION = "subscription";
  private static final String TITLE = "title";
  private static final String SPECIALIZATION = "specialization";
  private static final String URL = "url";
  private static final String HOMEPAGE = "homepage";
  private static final String PREFIXES = "prefixes";
  private static final String AUTHOR = "author";

  private final List<Subscription> subscriptions;
  private Subscription currentSubscription;

  public SubscriptionParser(final List<Subscription> subscriptions)
  {
    super();
    this.subscriptions = subscriptions;
  }

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
  {
    if (localName.equalsIgnoreCase(SUBSCRIPTION))
    {
      currentSubscription = new Subscription();
      currentSubscription.title = attributes.getValue(TITLE);
      currentSubscription.specialization = attributes.getValue(SPECIALIZATION);
      currentSubscription.url = attributes.getValue(URL);
      currentSubscription.homepage = attributes.getValue(HOMEPAGE);
      final String prefix = attributes.getValue(PREFIXES);
      if (prefix != null)
      {
        final String[] prefixes = prefix.split(",");
        currentSubscription.prefixes = prefixes;
      }
      currentSubscription.author = attributes.getValue(AUTHOR);
    }
    super.startElement(uri, localName, qName, attributes);
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException
  {
    if (localName.equalsIgnoreCase(SUBSCRIPTION))
    {
      if (subscriptions != null && currentSubscription != null)
      {
        subscriptions.add(currentSubscription);
      }
      currentSubscription = null;
    }
    super.endElement(uri, localName, qName);
  }
}
