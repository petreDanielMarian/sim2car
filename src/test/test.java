package test;

import java.util.Vector;

public class test {
	public static void add( int [] v)
	{
		v[1] = 1;
		v[0] = 0;
	}
	public static void main(String[] args) {
		int [] v = new int[2];
		add(v);
		System.out.println(v[0] + " " +v[1]);
	}

}
