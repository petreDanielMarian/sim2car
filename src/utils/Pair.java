package utils;


public class Pair<F, S> implements Comparable<Pair>{
    final private F first;  /* first member of pair */
    final private S second; /* second member of pair */

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
    
	public boolean equals(Object o){
		Pair<Long, Long> a = (Pair<Long, Long>) o;
		return a.first.equals(first) && a.second.equals(second);
	}

	@Override
	public int compareTo(Pair o) {
		/* First sort after first value and then after the second one */
        int dif;
        
        if(this.first instanceof Comparable<?>)
        {
        	dif = ((Comparable) this.first).compareTo(o.first);
        	if( dif == 0 )
        	{
        		 if(this.second instanceof Comparable<?>)
        	     {
        			 dif = ((Comparable) this.second).compareTo(o.second);
        			 return dif;
        	     }
        		 else
        		 {
        			 /* force to add this new pair after the current pair */
        			 return 1;
        		 }
        	}
        	else
        	{
        		return dif;
        	}
        }
        else
        {
			 /* force to add this new pair after the current pair */
			 return 1;
        }

	}
	
	public String toString()
	{
		return "(" + this.first + " - " + this.second + ")";
	}
		
}