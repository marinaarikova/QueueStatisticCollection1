import org.projectfloodlight.openflow.types.U64;

public class StatisticEntry {

	private U64 txbits;
	private U64 txpackets;
	private U64 txerror;
	
	private StatisticEntry() {
		
	}
	
	@Override
	public String toString() {
		return "StatisticEntry [txbits=" + txbits + ", txpackets=" + txpackets + ", txerror=" + txerror + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((txbits == null) ? 0 : txbits.hashCode());
		result = prime * result + ((txerror == null) ? 0 : txerror.hashCode());
		result = prime * result + ((txpackets == null) ? 0 : txpackets.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatisticEntry other = (StatisticEntry) obj;
		if (txbits == null) {
			if (other.txbits != null)
				return false;
		} else if (!txbits.equals(other.txbits))
			return false;
		if (txerror == null) {
			if (other.txerror != null)
				return false;
		} else if (!txerror.equals(other.txerror))
			return false;
		if (txpackets == null) {
			if (other.txpackets != null)
				return false;
		} else if (!txpackets.equals(other.txpackets))
			return false;
		return true;
	}
	public StatisticEntry(U64 txbits, U64 txpackets, U64 txerror) {
		super();
		this.txbits = txbits;
		this.txpackets = txpackets;
		this.txerror = txerror;
	}
	public U64 getTxbits() {
		return txbits;
	}
	public void setTxbits(U64 txbits) {
		this.txbits = txbits;
	}
	public U64 getTxpackets() {
		return txpackets;
	}
	public void setTxpackets(U64 txpackets) {
		this.txpackets = txpackets;
	}
	public U64 getTxerror() {
		return txerror;
	}
	public void setTxerror(U64 txerror) {
		this.txerror = txerror;
	}
	
}
