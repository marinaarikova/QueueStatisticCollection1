package net.floodlightcontroller.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageExceptionHandler;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.nosql.NoSqlResultSet;

public class QueueStatisticDB implements IStorageSourceListener, IStorageExceptionHandler {
	

	IStorageSourceService  storagesourceservice;
	int index = 0;
	String StatsTable="QueueStatisticTable";
	
	public QueueStatisticDB(IStorageSourceService  storagesourceservice) {
		Set<String> setColumn = new HashSet<>();
	    setColumn.add("id");
	    setColumn.add("txb");
	    setColumn.add("dur");
	    setColumn.add("txpack");
	    this.storagesourceservice=storagesourceservice;

	   
	
	storagesourceservice.createTable(StatsTable, setColumn);
	
	
	this.storagesourceservice.addListener(StatsTable, this);
	this.storagesourceservice.setExceptionHandler(this);

	}

	public void writetotable(DatapathId dpid, U64 txb, U64 packets) {
		index = (index + 1)%20;
		Map<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("id", dpid);
		hashMap.put("txb", txb);
		hashMap.put("packets", packets);
		
		this.storagesourceservice.insertRow(StatsTable, hashMap);
		
	}
	public Future ReadDataFromTable(DatapathId dpid) {
		System.out.println("Read data from table");
		index = (index + 1)%20;
		Map<String, Object> hashMap = new HashMap<String, Object>();
		Future<?> resulte;
		
		hashMap.put("id", dpid); 
		
		resulte = this.storagesourceservice.getRowAsync(StatsTable, hashMap);
     System.out.println("ResultHashMap");	

		return resulte;
	}
  
	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		System.out.println(tableName + rowKeys);
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		System.out.println(tableName + rowKeys);
	}

	@Override
	public void handleException(Exception exc) {
		// TODO Auto-generated method stub
		System.err.println(exc);;
		
	}
	
	
}
