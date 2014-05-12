package org.rpi.netty.songcast.ohu;

/**
 * OHUMessageAudioHandler
 * Handles the OHUMessageAudio
 * Create an Player and passes the sound bytes
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.log4j.Logger;
import org.rpi.config.Config;
import org.rpi.java.sound.IJavaSoundPlayer;
import org.rpi.java.sound.JavaSoundPlayerBasic;
import org.rpi.java.sound.JavaSoundPlayerLatency;
import org.rpi.mplayer.TrackInfo;
import org.rpi.player.PlayManager;
import org.rpi.player.events.EventUpdateTrackInfo;
import org.scratchpad.songcast.core.AudioInformation;
import org.scratchpad.songcast.core.SongcastPlayerJSLatency;
import org.scratchpad.songcast.core.SongcastPlayerJavaSound;

public class OHUMessageAudioHandler extends SimpleChannelInboundHandler<OHUMessageAudio> {

	private Logger log = Logger.getLogger(this.getClass());
	private IJavaSoundPlayer player = null;
	private AudioInformation audioInformation = null;
	private Thread threadPlayer = null;
	private int lastFrameNumber = 0;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, OHUMessageAudio msg) throws Exception {
		if (msg instanceof OHUMessageAudio) {
			try {
				if (player != null) {
					AudioInformation ai = msg.getAudioInformation();
					if (ai != null && !ai.compare(audioInformation)) {
						player.createSoundLine(ai);
						setAudioInformation(ai);
					}
					ai = null;
					player.put(msg);
					if (msg.getFrameNumber() - lastFrameNumber != 1) {
						log.debug("Missed a Frame: " + msg.getFrameNumber() + " LastFrameNumber: " + lastFrameNumber + " " + ((msg.getFrameNumber() - lastFrameNumber)-1));
					}
					lastFrameNumber = msg.getFrameNumber();
				} else {
					if (Config.getInstance().isSongcastLatencyEnabled()) {
						// With Latency
						player = new JavaSoundPlayerLatency();
						threadPlayer = new Thread(player, "SongcastPlayerJavaSoundLatency");
					} else {
						player = new JavaSoundPlayerBasic();
						threadPlayer = new Thread(player, "SongcastPlayerJavaSound");
					}
					threadPlayer = new Thread(player, "SongcastPlayerJavaSound");
					threadPlayer.start();
					setAudioInformation(msg.getAudioInformation());
					player.createSoundLine(audioInformation);
					player.put(msg);
					lastFrameNumber = msg.getFrameNumber();
				}
				// msg.getData().release();
			} catch (Exception e) {
				log.error("Error Handling Audio Message", e);
			}
		}
	}

	private void setAudioInformation(AudioInformation ai) {
		audioInformation = ai;
		try {
			TrackInfo info = new TrackInfo();
			info.setBitDepth(ai.getBitDepth());
			info.setCodec(ai.getCodec());
			info.setBitrate(ai.getBitRate());
			info.setSampleRate((long) ai.getSampleRate());
			info.setDuration(0);
			EventUpdateTrackInfo ev = new EventUpdateTrackInfo();
			ev.setTrackInfo(info);
			if (ev != null) {
				PlayManager.getInstance().updateTrackInfo(ev);
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(cause);
		ctx.close();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		log.debug("Channel Registered: " + ctx.name());
		super.channelRegistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug("Channel Actvie: " + ctx.name());
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug("Channel Inactive: " + ctx.name());
		super.channelInactive(ctx);
	};

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		log.debug("Channel Unregistered: " + ctx.name());
		if (player != null) {
			PlayManager.getInstance().setStatus("Stopped", "SONGCAST");
			player.stop();
			player = null;
		}
		if (threadPlayer != null) {
			threadPlayer = null;
		}
		super.channelUnregistered(ctx);
	}
}
