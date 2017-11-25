package test.model.network;

import java.io.Serializable;

import org.junit.Test;

import application.ApplicationType;

import model.network.Message;
import model.network.MessageType;

import junit.framework.TestCase;

public class MessageTest extends TestCase {

	@Test
	public void testMessageSerialization() {
		
		MyData md = new MyData("test", 5);
		
		Message m =  new Message(1, 1, null, MessageType.REQUEST_ROUTE_TILES_PEER, ApplicationType.TILES_APP);
		
		m.setPayload(md);
		
		MyData sameMd = (MyData)m.getPayload();
		
		assertTrue("Object is not serialized properly", md.equals(sameMd));
	}
}


class MyData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String msg;
	public int i;
	
	public MyData(String msg, int i) {
		
		this.msg = msg;
		this.i = i;
	}
	
	public boolean equals(Object md) {
		
		MyData a = (MyData)md;
		return this.i == a.i && this.msg.compareTo(a.msg) == 0;	
	}
}