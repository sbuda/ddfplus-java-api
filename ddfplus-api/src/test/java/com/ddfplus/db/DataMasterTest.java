package com.ddfplus.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ddfplus.db.MarketEvent.MarketEventType;
import com.ddfplus.messages.CtrlTimestamp;
import com.ddfplus.messages.DdfMarketBase;

public class DataMasterTest {

	private DataMaster dataMaster;

	private Quote quote;

	private SymbolInfo symbolInfo;

	@Before
	public void setUp() throws Exception {
		dataMaster = new DataMaster(MasterType.Realtime);

	}

	@Test
	public void processMessageEmptyBytes() {
		byte[] data = null;
		FeedEvent fe = dataMaster.processMessage(data);
		assertNull(fe);
		data = new byte[1];
		fe = dataMaster.processMessage(data);
		assertNull(fe);
	}

	@Test
	public void processMessageDdfMessageIsAlwaysSaved() {

		DdfMarketBase msg = null;
		FeedEvent fe = dataMaster.processMessage(msg);
		assertNull(fe);
		// Set EOD
		dataMaster = new DataMaster(MasterType.EndOfDay);
		msg = new CtrlTimestamp(null);
		fe = dataMaster.processMessage(msg);
		assertNotNull(fe);
		assertTrue(fe.isDdfMessage());
		assertEquals(msg, fe.getDdfMessage());

	}

	@Test
	public void process20OpenMessage() {
		final byte[] msg = "\u00012MEZ900C,0\u00022G10100,A0C \u0003".getBytes();

		symbolInfo = new SymbolInfo("MEZ900C", "MEZ900C", "G", '2', null, 1);
		quote = new Quote(symbolInfo);
		dataMaster.putQuote(quote);

		FeedEvent fe = dataMaster.processMessage(msg);
		assertNotNull(fe);
		Quote update = fe.getQuote();
		assertNotNull(update);
		Session session = update.getCombinedSession();
		assertEquals(10.0f, session.getHigh(), 0.0);
		assertEquals(10.0f, session.getLow(), 0.0);
		assertEquals(10.0f, session.getLast(), 0.0);
		assertEquals(10.0f, session.getOpen(), 0.0);
		// Should be a market event
		List<MarketEvent> events = fe.getMarketEvents();
		MarketEvent me = events.get(0);
		assertNotNull(me);
		assertEquals(MarketEventType.Open, me.getEventType());
		assertEquals("MEZ900C", me.getSymbol());
		assertEquals(10.0f, me.getOpen(), 0.0);

	}

	@Test
	public void process2ZSaleConditionShouldBeSet() {
		// 2HEUS,ZbAA152591,100,NMc
		final byte[] msg = "\u00012HEUS,Z\u0002A152591,100,NA\u0003".getBytes();

		symbolInfo = new SymbolInfo("HEUS", "HEUS", "G", '2', null, 1);
		quote = new Quote(symbolInfo);
		// Set existing volume to 500
		quote._combinedSession._volume = 500;
		dataMaster.putQuote(quote);
		FeedEvent fe = dataMaster.processMessage(msg);
		assertNotNull(fe);
		Quote update = fe.getQuote();
		assertNotNull(update);
		Session session = update.getCombinedSession();
		assertEquals("Volume should be set", 500 + 100, session.getVolume(), 0.0);

	}

	@Test
	public void process2ZSaleConditionShouldNotBeSet() {
		// 2HEUS,ZbAA152591,100,NMc
		byte[] msg = "\u00012HEUS,Z\u0002A152591,100,NM\u0003".getBytes();

		symbolInfo = new SymbolInfo("HEUS", "HEUS", "G", '2', null, 1);
		quote = new Quote(symbolInfo);
		// Set existing volume to 500
		quote._combinedSession._volume = 500;
		dataMaster.putQuote(quote);
		FeedEvent fe = dataMaster.processMessage(msg);
		Quote update = fe.getQuote();
		Session session = update.getCombinedSession();
		assertEquals("Volume not set", 500, session.getVolume(), 0.0);

		// Q sale condition
		msg = "\u00012HEUS,Z\u0002A152591,100,NQ\u0003".getBytes();
		dataMaster.putQuote(quote);
		fe = dataMaster.processMessage(msg);
		update = fe.getQuote();
		session = update.getCombinedSession();
		assertEquals("Volume not set", 500, session.getVolume(), 0.0);

		// 9 sale condition
		msg = "\u00012HEUS,Z\u0002A152591,100,N9\u0003".getBytes();
		dataMaster.putQuote(quote);
		fe = dataMaster.processMessage(msg);
		update = fe.getQuote();
		session = update.getCombinedSession();
		assertEquals("Volume not set", 500, session.getVolume(), 0.0);

	}

}
