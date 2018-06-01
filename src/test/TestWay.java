package test;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import model.LocationParse;
import model.OSMgraph.Node;
import model.OSMgraph.Way;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestWay {
	double min_lat, min_long, max_lat, max_long;
	
	@Before
	public void setUp() throws Exception {
		Way way = new Way(0);
		min_lat = way.min_lat;
		min_long = way.min_long;
		max_lat = way.max_lat;
		max_long = way.max_long;
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testDirectionTrue() {
		Way way = new Way(0);
		way.setDirection(true);
		assertEquals(way.getDirection(), true);
	}
	
	@Test
	public void testDirectionFalse() {
		Way way = new Way(0);
		way.setDirection(false);
		assertEquals(way.getDirection(), false);
	}

	@Test
	public void testAddVirtualNode() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		
		way.addVirtualNode(n1);
		assertEquals(n1, way.getNode(1));
		
		assertEquals(min_lat, way.min_lat, 0.01);
		assertEquals(min_long, way.min_long, 0.01);
		assertEquals(max_lat, way.max_lat, 0.01);
		assertEquals(max_long, way.max_long, 0.01);
	}
	
	@Test
	public void testAddVirtualNodes() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		Node n2 = new Node(2, 10, 20);
		
		way.addVirtualNode(n1);
		assertEquals(n1, way.getNode(1));
		
		way.addVirtualNode(n2);
		assertEquals(n1, way.getNode(1));
		assertEquals(n2, way.getNode(2));
		
		assertEquals(min_lat, way.min_lat, 0.01);
		assertEquals(min_long, way.min_long, 0.01);
		assertEquals(max_lat, way.max_lat, 0.01);
		assertEquals(max_long, way.max_long, 0.01);
	}

	@Test
	public void testAddNode() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		
		way.addNode(n1);
		assertEquals(n1, way.getNode(1));
		
		assertEquals(10, way.min_lat, 0.01);
		assertEquals(10, way.min_long, 0.01);
		assertEquals(10, way.max_lat, 0.01);
		assertEquals(10, way.max_long, 0.01);
	}

	@Test
	public void testAddNodes() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		Node n2 = new Node(2, 5, 20);
		
		way.addNode(n1);
		way.addNode(n2);
		assertEquals(n1, way.getNode(1));
		assertEquals(n2, way.getNode(2));
		
		assertEquals(5, way.min_lat, 0.01);
		assertEquals(10, way.min_long, 0.01);
		assertEquals(10, way.max_lat, 0.01);
		assertEquals(20, way.max_long, 0.01);
	}
	
	@Test
	public void testGetNode() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		Node n2 = new Node(2, 5, 20);
		
		way.addNode(n1);
		way.addVirtualNode(n2);
		
		assertEquals(n1, way.getNode(1));
		assertEquals(n2, way.getNode(2));
	}

	@Test
	public void testGetNodeIndex() {
		Way way = new Way(0);
		Node n1 = new Node(23, 10, 10);
		Node n2 = new Node(42, 5, 20);
		
		way.addNode(n1);
		way.addVirtualNode(n2);
		
		assertEquals(0, way.getNodeIndex(23));
		assertEquals(1, way.getNodeIndex(42));
	}

	@Test
	public void testGetNodesFromA2BNode() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		Node n2 = new Node(2, 10, 15);
		Node n3 = new Node(4, 10, 5);
		Node n4 = new Node(3, 10, 20);
		
		
		way.addNode(n1);
		way.addNode(n2);
		way.addNode(n3);
		way.addNode(n4);
		
		Vector<Node> result = way.getNodesFromA2B(n1, n4);
		assertEquals(3, result.size());
		assertEquals(n1, result.get(0));
		assertEquals(n2, result.get(1));
		assertEquals(n4, result.get(2));
	}
	
	@Test
	public void testGetNodesFromA2BLocationParse() {
		Way way = new Way(0);
		Node n1 = new Node(1, 10, 10);
		Node n2 = new Node(2, 10, 15);
		Node n3 = new Node(4, 10, 5);
		Node n4 = new Node(3, 10, 20);
		LocationParse lp1 = new LocationParse(10, 10, 0, 0);
		LocationParse lp4 = new LocationParse(10, 20, 0, 0);
		
		way.addNode(n1);
		way.addNode(n2);
		way.addNode(n3);
		way.addNode(n4);
		
		Vector<Node> result = way.getNodesFromA2B(lp1, lp4);
		assertEquals(3, result.size());
		assertEquals(n1, result.get(0));
		assertEquals(n2, result.get(1));
		assertEquals(n4, result.get(2));
	}

	@Test
	public void testGetAllNeighbours() {
		Way way = new Way(0);
		// First neighbour
		Long neigh1 = new Long(1);
		Vector<Long> neigh1Conn = new Vector<Long>();
		neigh1Conn.add(new Long(2));

		// Second neighbour
		Long neigh2 = new Long(2);
		Vector<Long> neigh2Conn = new Vector<Long>();
		neigh2Conn.add(new Long(2));
		
		assertEquals(0, way.getAllNeighbors().size());
		way.neighs.put(neigh1, neigh1Conn);
		assertEquals(1, way.getAllNeighbors().size());
		way.neighs.put(neigh2, neigh2Conn);
		assertEquals(2, way.getAllNeighbors().size());
		way.neighs.remove(new Long(1));
		assertEquals(1, way.getAllNeighbors().size());
	}

}
