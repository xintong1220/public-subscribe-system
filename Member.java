package pssystem;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Member implements Serializable {
	/**
	 * The member address in the form IP:Port
	 */
	private String address;

	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean leader) {
		isLeader = leader;
	}

	private boolean isLeader;

	private int heartbeat;

	private transient TimeoutTimer timeoutTimer;

	public Member(String address, int heartbeat, Server server, int t_cleanup) {
		this.address = address;
		this.heartbeat = heartbeat;
		this.timeoutTimer = new TimeoutTimer(t_cleanup, server, this);
		this.isLeader = false;
	}

	public void startTimeoutTimer() {
		this.timeoutTimer.start();
	}

	public void resetTimeoutTimer() {
		this.timeoutTimer.reset();
	}

	public String getAddress() {
		return address;
	}

	public int getHeartbeat() {
		return heartbeat;
	}

	public void setHeartbeat(int heartbeat) {
		this.heartbeat = heartbeat;
	}

	@Override
	public String toString() {
		return "Member [address=" + address + ", heartbeat=" + heartbeat + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((address == null) ? 0 : address.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Member other = (Member) obj;
		if (address == null) {
			if (other.address != null) {
				return false;
			}
		} else if (!address.equals(other.address)) {
			return false;
		}
		return true;
	}
}
