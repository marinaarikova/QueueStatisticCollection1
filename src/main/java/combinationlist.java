import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;

public class combinationlist {
	
	private Map<DatapathId, StatisticEntryArray> hashMap = new HashMap<DatapathId, StatisticEntryArray>();
	     
	 public void StoreEntry(DatapathId dpid, StatisticEntry statisticentry) {
		 
		 if(this.hashMap.containsKey(dpid)  ) {
		 		 
	 }  else {
		 StatisticEntryArray newarrayincaseabsense = new StatisticEntryArray(dpid);
		 
		 newarrayincaseabsense.insertvalue(statisticentry);
		 hashMap.put(dpid, newarrayincaseabsense);
	 }
 
	 }	
}
