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

  private List<Subscription> subscriptions;
  private Subscription currentSubscription;

  public SubscriptionParser(List<Subscription> subscriptions)
  {
    super();
    this.subscriptions = subscriptions;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
  {
    if (localName.equalsIgnoreCase(SUBSCRIPTION))
    {
      currentSubscription = new Subscription();
      currentSubscription.title = attributes.getValue(TITLE);
      currentSubscription.specialization = attributes.getValue(SPECIALIZATION);
      currentSubscription.url = attributes.getValue(URL);
      currentSubscription.homepage = attributes.getValue(HOMEPAGE);
      String prefix = attributes.getValue(PREFIXES);
      if (prefix != null)
      {
        String[] prefixes = prefix.split(",");
        currentSubscription.prefixes = prefixes;
      }
      currentSubscription.author = attributes.getValue(AUTHOR);
    }
    super.startElement(uri, localName, qName, attributes);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
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
