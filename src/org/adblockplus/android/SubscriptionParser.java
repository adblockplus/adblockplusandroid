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
	private Subscription subscription;

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
			subscription = new Subscription();
			subscription.title = attributes.getValue(TITLE);
			subscription.specialization = attributes.getValue(SPECIALIZATION);
			subscription.url = attributes.getValue(URL);
			subscription.homepage = attributes.getValue(HOMEPAGE);
			String prefix = attributes.getValue(PREFIXES);
			if (prefix != null)
			{
				String[] prefixes = prefix.split(",");
				subscription.prefixes = prefixes;
			}
			subscription.author = attributes.getValue(AUTHOR);
		}
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if (localName.equalsIgnoreCase(SUBSCRIPTION))
		{
			if (subscriptions != null && subscription != null)
			{
				subscriptions.add(subscription);
			}
			subscription = null;
		}
		super.endElement(uri, localName, qName);
	}
}
