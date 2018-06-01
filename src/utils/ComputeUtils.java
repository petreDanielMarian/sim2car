package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ComputeUtils {

	
	public static <F, S> List<Pair<F, S>> sortHashMap(HashMap<F, S> map) {
		List<Pair<F, S>> sortedData = new ArrayList<Pair<F, S>>();
		
		for(F key : map.keySet()) {
			sortedData.add(new Pair<F, S>(key, map.get(key)));
		}

		Collections.sort(sortedData, new Comparator<Pair<F, S>>() {
			@Override
			public int compare(Pair<F, S> o1, Pair<F, S> o2) {
				if (o1.getSecond() instanceof Double)
					return ((Double)o1.getSecond()).compareTo((Double)o2.getSecond());
				if (o1.getSecond() instanceof Integer)
					return (int)((Integer)o1.getSecond() - (Integer)o2.getSecond());
				return 0;
			}
		});
		
		return sortedData;
	}
	
}
