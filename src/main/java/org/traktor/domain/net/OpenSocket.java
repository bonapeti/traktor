package org.traktor.domain.net;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.traktor.domain.Sampler;

public class OpenSocket implements Sampler<Boolean> {
	
	private final InetSocketAddress inetSocketAddress;
	private final int timeout = 0;
	
	public OpenSocket(InetSocketAddress inetSocketAddress, int timeout) {
		super();
		this.inetSocketAddress = inetSocketAddress;
		if (timeout <= 0) {
			throw new IllegalArgumentException("Time out should be greater than zero!");
		}
	}

	@Override
	public Boolean takeSample() throws Exception {
		try (Socket socket = new Socket()) {
			socket.connect(inetSocketAddress, timeout);
			return Boolean.TRUE;
		}
	}

}
