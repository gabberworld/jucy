package uc.protocols.hub;

import java.io.IOException;
import java.util.Collections;


import uc.IUser;
import uc.protocols.CPType;
import uc.protocols.SendContext;
import uc.user.User;

public class RevConnectToMe extends AbstractNMDCHubProtocolCommand {

	public RevConnectToMe() {
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		if (!hub.getFavHub().isChatOnly()) {
	  		String[] a = command.split(" ");
			if (a.length >= 2 ) {
				User other = hub.getUserByNick(a[1]);
				if (other != null &&  hub.getIdentity().isActive()) {
					hub.sendCTM(other,CPType.NMDC,null);
				}
			}
		}
	}

	

	
	public static void sendRCM(Hub hub, IUser target) {
		hub.sendRaw("$RevConnectToMe %[myNI] %[userNI]|",  
				new SendContext(target,Collections.<String,String>emptyMap()));
	}

}
