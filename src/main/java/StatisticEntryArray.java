import org.projectfloodlight.openflow.types.DatapathId;

public class StatisticEntryArray {
	
	 private DatapathId dpid;
	 private static final int length = 20;
	 private int index = 0;
	 private StatisticEntry lastentry = null;
	 private StatisticEntry[] anArray = new StatisticEntry[length];

	public DatapathId getDpid() {
		return dpid;
	}

	public void setDpid(DatapathId dpid) {
		this.dpid = dpid;
	}

	public StatisticEntryArray(DatapathId dpid) {
		super();
		this.dpid = dpid;
	}
	 
	 public void insertvalue(StatisticEntry insertedvalue) {
		 
		 this.anArray[index] = insertedvalue;
		 index = (index +1)%length;
		 
		 this.lastentry = insertedvalue;
		 
		 		 
		 
	 }

	public StatisticEntry getLastentry() {
		return lastentry;
	}
	
	
}
