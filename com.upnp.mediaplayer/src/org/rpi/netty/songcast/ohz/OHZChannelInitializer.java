package org.rpi.netty.songcast.ohz;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class OHZChannelInitializer extends ChannelInitializer<NioDatagramChannel> {

	private Logger log = Logger.getLogger(this.getClass());

	@Override
	protected void initChannel(NioDatagramChannel ch) throws Exception {
		try {
			log.debug("Start of OHZChannelInitializer");
			ChannelPipeline p = ch.pipeline();
			p.addLast(new LoggingHandler(LogLevel.DEBUG));
			p.addLast("OHZDecoder", new OHZMessageDecoder());
			p.addLast("OHZRequestHandler", new OHZRequestHandler());
			p.addLast("OHZTest", new OHZTest());
			log.debug("End of OHZChannelInitializer");
		} catch (Exception e) {
			log.error("Error ChannelInitializer", e);
		}
	}
}