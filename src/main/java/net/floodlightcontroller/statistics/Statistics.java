package net.floodlightcontroller.statistics;


import java.net.SocketAddress;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFQueueStatsEntry;
import org.projectfloodlight.openflow.protocol.OFQueueStatsReply;
import org.projectfloodlight.openflow.protocol.OFQueueStatsRequest;
import org.projectfloodlight.openflow.protocol.OFRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFConnection;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.LogicalOFMessageCategory;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.SwitchDescription;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.OFConnection;
import net.floodlightcontroller.core.internal.TableFeatures;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import static java.util.concurrent.TimeUnit.*;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.storage.IStorageSourceService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class Statistics implements IThreadPoolService,IFloodlightModule,IOFSwitchListener,IOFSwitch, IOFMessageListener {
	public final int DEFAULT_CACHE_SIZE = 10;
	protected IFloodlightProviderService floodlightProvider;
    protected IStorageSourceService storagesourceservice;   
	protected IThreadPoolService threadspool;
	protected IOFSwitchService switchService;
	protected QueueStatisticDB QueueStatisticDB;
	
	
	
	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT=0;
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT=0;
	protected static short FLOWMOD_PRIORITY=101;
	private Logger logger;
	private IStatisticsService staticcollector;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    

	
	@Override
	public String getName() {
		return "Names";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	// This is where we pull fields from the packet-in
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
	    case PACKET_IN:
	    	
	        /* Retrieve the deserialized packet in message */
	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	 
	        /* Various getters and setters are exposed in Ethernet */
	        if (eth.getEtherType() == EthType.IPv4) {
	            /* We got an IPv4 packet; get the payload from Ethernet */
            IPv4 ipv4 = (IPv4) eth.getPayload();   
	            /* 
	             * Check the IP protocol version of the IPv4 packet's payload.
	             */
	            if (ipv4.getProtocol() == IpProtocol.UDP) {
	                /* We got a TCP packet; get the payload from IPv4 */
	                UDP udp = (UDP) ipv4.getPayload();
	                 
	                /* Various getters and setters are exposed in TCP */
	            
	                TransportPort srcPort = udp.getSourcePort();
	                TransportPort dstPort = udp.getDestinationPort();
	                System.out.println("Port 5001 is matched" + srcPort + dstPort);
	                logger.info(": " + staticcollector.getBandwidthConsumption());
	                Map<NodePortTuple, SwitchPortBandwidth> nodeport = staticcollector.getBandwidthConsumption();
	                for (Entry<NodePortTuple, SwitchPortBandwidth> entry : nodeport.entrySet()) {
	                	System.out.println(entry.getKey() + "/" + entry.getValue().getBitsPerSecondRx().getValue() +" "+ entry.getValue().getBitsPerSecondTx().getValue());
	                	}
	            }
	        }
	        break;
	    default:
	        break;
	    }
	    return Command.CONTINUE;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}
	 
	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
	}

	@Override
	public void switchAdded(DatapathId switchId){
		// TODO Auto-generated method stub
	} 
	 
	public synchronized void queue_stats(DatapathId dpid){
        logger.info("queue stats with dpid " + dpid);
        DatapathId DatpathId = dpid;
        OFFactoryVer13 factory = new OFFactoryVer13();
        OFQueueStatsRequest sr = factory.buildQueueStatsRequest().setPortNo(OFPort.of(2)).setQueueId(1).build();
        /* Note use of writeStatsRequest (not writeRequest) */
        ListenableFuture<List<OFQueueStatsReply>> future = switchService.getSwitch(DatpathId).writeStatsRequest(sr);
        List<OFQueueStatsReply> replies;
		try {
			replies = future.get(10, TimeUnit.SECONDS);
			for (OFQueueStatsReply reply : replies) {
                for (OFQueueStatsEntry e : reply.getEntries()) {
                    long id = e.getQueueId();
                    U64 txb = e.getTxBytes();
                    long dur = e.getDurationSec();
                    U64 packets = e.getTxPackets();
                    logger.info("Queue Stats "+id+" txb "+txb+" dur "+dur+" txpack "+packets+" "+e.toString());
                    this.QueueStatisticDB.writetotable(dpid, txb, packets);
                    this.QueueStatisticDB.ReadDataFromTable(dpid);
                }}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
						e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (java.util.concurrent.TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	public void request_stat(DatapathId dpid) {
        final Runnable beeper = new Runnable() {
                public void run() {queue_stats(dpid);
                System.out.println("Beeper!");}
            };
        final ScheduledFuture<?> beeperHandle =
            scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
        scheduler.schedule(new Runnable() {
                public void run() { beeperHandle.cancel(true); }
            }, 60 * 60, SECONDS);
    }
	
	public void statistics_record(DatapathId dpid) {
       final Runnable statisticsrec = new Runnable() {
                public void run() {queue_stats(dpid);
                System.out.println("Statistics was recorded!");}
            };
	}
       
	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		logger.info(": {} seen on switch: {.... }");
		logger.info(": PUSH");
		logger.info("Hello!");
		//queue_stats(switchId);
		request_stat(switchId);
    	statistics_record(switchId);
    
				
		
		//Initialized builders for creating a flow entry
		IOFSwitch sw = switchService.getSwitch(switchId);
		OFFactory factory = sw.getOFFactory();
		
		//Create a matcher
		Match myMatch = factory.buildMatch()
			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
			    .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			    .setExact(MatchField.IPV4_DST, IPv4Address.of("192.168.10.80"))
			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.10.81"))
			    .setExact(MatchField.UDP_DST, TransportPort.of(5566))
			    .build();
		
		//Create action list
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = factory.actions();
		
		OFActionOutput output = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(OFPort.of(2))
			    .build();
		
		OFActionOutput sentToController = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(OFPort.CONTROLLER)
			    .build();
		
		OFActionSetQueue sentToQueue = actions.buildSetQueue()
				.setQueueId(1)
			    .build();
		
		actionList.add(sentToQueue);
		actionList.add(output);
		//actionList.add(sentToController);
		
		
		//Create instructions
		ArrayList<OFInstruction> instructionsList = new ArrayList<OFInstruction>();
		OFInstructions instructions = factory.instructions();
		
		OFInstructionApplyActions applyActions = instructions.buildApplyActions()
			    .setActions(actionList)
			    .build();
		
		instructionsList.add(applyActions);
		
		//Build flow entry
		
		
		OFFlowAdd flowAdd13 = factory.buildFlowAdd()
					.setBufferId(OFBufferId.NO_BUFFER)
				    .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				    .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				    .setCookie(U64.of(0x7777))
				    .setPriority(32768)
				    .setMatch(myMatch)
			        .setInstructions(instructionsList)
			        .build();
		
		sw.write(flowAdd13);
		System.out.println("Flow entry is pushed");
	}
//		
	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// TODO Auto-generated method stub
	}

	
	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchDeactivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean write(OFMessage m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<OFMessage> write(Iterable<OFMessage> msgList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SwitchStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getBuffers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<OFActionType> getActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<OFCapabilities> getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<TableId> getTables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SwitchDescription getSwitchDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketAddress getInetAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getEnabledPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPort> getEnabledPortNumbers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFPortDesc getPort(OFPort portNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFPortDesc getPort(String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getSortedPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean portEnabled(OFPort portNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean portEnabled(String portName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Date getConnectedSince() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DatapathId getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public OFControllerRole getControllerRole() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean attributeEquals(String name, Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFFactory getOFFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableList<IOFConnection> getConnections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean write(OFMessage m, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<OFMessage> write(Iterable<OFMessage> msglist, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFConnection getConnectionByCategory(LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TableFeatures getTableFeatures(TableId table) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getNumTables() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public U64 getLatency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IStaticEntryPusherService.class);
		l.add(IOFSwitchService.class);
		l.add(IStatisticsService.class);
		l.add(IThreadPoolService.class);
		l.add(IStorageSourceService.class);
		l.add(IDebugCounterService.class);
		l.add(IRestApiService.class);
		
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		staticcollector = context.getServiceImpl(IStatisticsService.class);
		staticcollector.collectStatistics(true);
		logger = LoggerFactory.getLogger(Statistics.class);
		logger.info(": privet");
		logger.info(": " + staticcollector.getBandwidthConsumption());
		threadspool = context.getServiceImpl(IThreadPoolService.class);
    	storagesourceservice = context.getServiceImpl(IStorageSourceService.class);
    	QueueStatisticDB = new QueueStatisticDB(storagesourceservice);
   
    	
	}

}