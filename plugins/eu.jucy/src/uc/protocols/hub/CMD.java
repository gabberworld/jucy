package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;


import uc.Command;

public class CMD extends AbstractADCHubCommand {

	
	
	public CMD() {  // ICMD Status\\Do\snot\sdisturb CT1 TTBINF\s%[mySID]\sAW2\n
		setPattern(getHeader()+" ("+ADCTEXT+") (.*)",true);
	}

	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		if (command.charAt(0) != 'I') {
			return;//Ignore if its not from hub..
		}
		String name = revReplaces(matcher.group(HeaderCapt+1));
		name = GH.switchChars(name, '/', '\\');

		
		Map<Flag,String> flags = getFlagMap(matcher.group(HeaderCapt+2));
		int context  =  0;
		if (flags.containsKey(Flag.CT)) {
			context = Integer.parseInt( flags.get(Flag.CT) );
		}
		boolean remove = "1".equals( flags.get(Flag.RM) );
		boolean seperator = "1".equals(flags.get(Flag.SP));
		
		Command c;
		if (seperator) {
		//	Command com = hub.getLastUserCommand();
		//	if (GH.isEmpty(name) && com != null) {
			//if (!name.endsWith("\\")) {
			//	name += "\\";
		//	}
		//	}
			c = new Command(context,name,hub.getFavHub().getHubaddy());
		} else {
			String exec = flags.get(Flag.TT);
			boolean allowMulti = !"1".equals(flags.get(Flag.CO));
			c =  new Command(name,allowMulti,context,exec,hub.getFavHub().getHubaddy());
		}
		
		logger.debug("Received cmd: "+command+" remove:"+remove+"  name:"+name+":");
		 
		
		if (remove) {
			hub.removeUserCommand(c);
		} else {
			hub.addUserCommand(c);
		}
	}

}
