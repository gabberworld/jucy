package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import uc.protocols.DCProtocol;

public class Lock extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	

	
	@Override
	public void handle(Hub hub,String command) throws IOException {
		logger.debug("Lock received: "+command);
		//Lock will be accepted only once .. clear all other commands.. (so ADC detector is gone or other stuff is gone)
	//	hub.clearCommands();
		//add next accepted commands
	
		


		//hub.setNmdchub(true);
		hub.setOnceConnected();
		
		//now handle the lock and send the key, supports and validate nick back	
	
		
		byte[] key = DCProtocol.generateKey(command,hub.getCharset());

		
		String validateNick = "$ValidateNick "+hub.getSelf().getNick()+ "|";
		
		byte[] send = GH.concatenate(key,validateNick.getBytes(hub.getCharset().name()));
		
		if (command.contains("EXTENDEDPROTOCOL")) {
			send = GH.concatenate(Supports.getSupports(hub).getBytes(hub.getCharset().name()),send);
			
	//		hub.sendUnmodifiedRaw(GH.concatenate(
	//				Supports.HUBSUPPORTS.getBytes(hub.getCharset().name()),
	//				key,
	//				validateNick.getBytes(hub.getCharset().name())));
			
		//	hub.sendUnmodifiedRaw(Supports.HUBSUPPORTS+key+validateNick);//done so all are sent in the first packet..
			logger.debug("Lock received -> responded with Key, ValidateNick and Supports: "+new String(key,hub.getCharset().name()));
		} else {
			hub.addCommand(new NickList());
			//hub.sendUnmodifiedRaw(GH.concatenate(key,validateNick.getBytes(hub.getCharset().name())));
		}
		hub.sendUnmodifiedRaw(send);
		hub.removeCommand(this);

	}

	
	
}
