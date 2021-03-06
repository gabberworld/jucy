package eu.jucy.gui.texteditor;



import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IHasUser.IMultiUser;
import uc.files.MagnetLink;
import uc.files.filelist.FileListFile;
import uc.files.filelist.IOwnFileList;
import uc.files.filelist.IOwnFileList.AddedFile;
import uc.protocols.hub.FeedType;

import uihelpers.SUIJob;
import uihelpers.SelectionProviderIntermediate;


import eu.jucy.gui.Application;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCMessageEditor;
import eu.jucy.gui.UCWorkbenchPart;
import eu.jucy.gui.itemhandler.SendScreenHandler.ScreenShotContributions;
import eu.jucy.gui.texteditor.hub.ItemSelectionProvider;

/**
 * 
 * holds common functionality of Hub and PM editor
 * protected fields not set by this must be set by implementing classes
 * -> hard to extend...
 * 
 * @author Quicksilver
 *
 */
public abstract class UCTextEditor extends UCMessageEditor {

	public static final String TEXT_POPUP_ID = "eu.jucy.gui.texteditor";
	
	private static final long NOT_CHATTED_TIME = 60 * 60 *1000;
	
	private static final Logger logger = LoggerFactory.make();
	
	protected SendingWriteline sendingWriteline;
	protected LabelViewer feedLabelViewer;
	protected StyledTextViewer textViewer;
	protected boolean messagesWaiting = false;
	
	protected TextUserSelectionprovider tus;
	
	protected final SelectionProviderIntermediate spi = new SelectionProviderIntermediate();
	
	private final Map<IUser,Long> recentlyChatted = new HashMap<IUser, Long>(); 
	
	/**
	 * first cleans up added user then adds the latest again..
	 * @param usr - true if the user wasn't present before..
	 */
	protected boolean put(IUser usr) {
		long removeAllBefore = System.currentTimeMillis() - NOT_CHATTED_TIME; 
		synchronized (recentlyChatted) {	
			Iterator<Entry<IUser,Long>> it = recentlyChatted.entrySet().iterator();
			while (it.hasNext()) {
				Entry<IUser,Long> e = it.next();
				if (e.getValue() < removeAllBefore) {
					it.remove();
					userRemovedFromRecentChatted(e.getKey());
				}
			}
			if (recentlyChatted.size() > 1000) { //protection against floods
				removeAll();
			}
			if (recentlyChatted.put(usr, System.currentTimeMillis()) == null) {
				userAddedToRecentChatted(usr);
				return true;
			}
			return false;
		}
	}
	
	protected boolean contains(IUser usr) {
		synchronized (recentlyChatted) {
			Long chattedLast= recentlyChatted.get(usr);
			if (chattedLast == null) {
				return false;
			} else {
				return chattedLast >  System.currentTimeMillis() - NOT_CHATTED_TIME;
			}
		}
	}
	
	protected void removeAll() {
		synchronized (recentlyChatted) {
			for (IUser usr: new ArrayList<IUser>(recentlyChatted.keySet())) {
				recentlyChatted.remove(usr);
				userRemovedFromRecentChatted(usr);
			}
		}
	}
	
	protected void userRemovedFromRecentChatted(IUser usr) {}
	protected void userAddedToRecentChatted(IUser usr) {}
	
	/**
	 * 
	 * @param usr
	 * @param join
	 */
	protected void showJoinsParts(IUser usr,boolean join) {
		String joinmes = "*** "+String.format(join?Lang.UserJoins:Lang.UserParts,usr.getNick())+" ***";
		appendText( joinmes , usr,MessageType.JOINPART);
		Long lastChatted;
		if (!join && (lastChatted = recentlyChatted.get(usr)) != null) {
			recentlyChatted.put(usr, lastChatted-(NOT_CHATTED_TIME/3) );
		}
		
	}
	
	
	public UCTextEditor() {}

	
	public SendingWriteline getSendingWriteline() {
		return sendingWriteline;
	}

	public Text getWriteline() {
		return sendingWriteline.getWriteline();
	}
	
	protected void makeTextActions() {
		tus = new TextUserSelectionprovider(getText(),getHub());

		spi.addSelectionProvider(getText(), tus);
		getSite().setSelectionProvider(spi);
		
		UCWorkbenchPart.createContextPopups(getSite(), TEXT_POPUP_ID, tus, getText());
		
		
		DropTarget target = new DropTarget(getText(),  DND.DROP_DEFAULT | DND.DROP_MOVE);
		target.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		target.addDropListener(new MagnetDropAdapter(false));
		
		DropTarget target2 = new DropTarget(getSendingWriteline().getWriteline(),  DND.DROP_DEFAULT | DND.DROP_MOVE);
		target2.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		target2.addDropListener(new MagnetDropAdapter(true));
		
	}
	
	protected void addedFile(FileListFile file,boolean append,boolean addedOutsideShare) {
		MagnetLink ml = new MagnetLink(file);
		if (append) {
			getSendingWriteline().getWriteline().append(ml.toString());
		} else {
			getSendingWriteline().send(ml.toString());
		}
	}
	
	public abstract void storedPM(IUser receiver,String message,boolean me);
	
	protected abstract void setTitleImage();
	
	public void statusMessage(final String message,  int severity) {
		appendText( "*** " +message,null,MessageType.STATUS);			
		changeLabel(message,severity);
	}
	
	
	protected void changeLabel(final String message, final int severity) {
		new SUIJob(feedLabelViewer.getLabel()) {
			public void run() {
				FeedType ft;
				switch(severity) {
				case 1: 
					ft = FeedType.WARN;
				break;
				case 2: 
					ft = FeedType.ERROR;
				break;
				default: 
					ft = FeedType.NONE;
				}
				feedLabelViewer.addFeedMessage(ft,message);
			}
			
		}.scheduleOrRun();
	}
	
	public void appendText(String text,IUser usr,MessageType type) {
		appendText(text, usr,System.currentTimeMillis(),type);
	}
	
	
	
	/**
	 * 
	 * @param text - fully formatted text to append (except textmodificators)
	 * @param usr - usr associated with text.. potentially null
	 * @param received - timestamp text is associated with
	 * @param message - true if that append is a message and not just some text
	 * (if true put will be called)
	 */
	public void appendText(final String text,final IUser usr,final long received , final MessageType type) {
		new SUIJob(textViewer.getText()) {
			public void run() {
				if (usr != null && !getHub().getSelf().equals(usr) && MessageType.CHAT.equals(type)) {
					put(usr);
				}
				textViewer.addMessage(text,usr,new Date(received),type);
			}	
		}.scheduleOrRun();
	}
	
	@Override
	public void partActivated() {
		super.partActivated();
		getText().redraw();
		messagesWaiting = false;
		setTitleImage();
	}

	public void replaceSelectedText(String replacement,String expectedSelection) {
		textViewer.replaceSelection(replacement, expectedSelection);
	}
	

	public abstract IHub getHub();
	
	public void dispose() {
		removeAll();
		super.dispose();
		
	}
	
	
	public void getContributionItems(List<IContributionItem> items) {
		super.getContributionItems(items);
		
		MenuManager mm = new MenuManager(Lang.SendScreen
				,AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.SCREENSHOT_ICON)
				,null);
		
		mm.add(new ScreenShotContributions());
	
		items.add( mm );
	}
	
	
	/**
	 * 
	 * @param f - the file that should be dropped
	 * @param append - if true -> append to writeline else -> send directly..
	 */
	public void dropFile(File f,final boolean append) {
		if (f.isFile()) {
			IOwnFileList iof = ApplicationWorkbenchWindowAdvisor.get().getFilelist();
			UCTextEditor uct = UCTextEditor.this;
			IUser droppedFor =  	  uct instanceof IHasUser 
								&& ! (uct instanceof IMultiUser) ?
										((IHasUser)UCTextEditor.this).getUser() 
										:null;
					
			iof.immediatelyAddFile(f, true,droppedFor, new AddedFile() {
					@Override
					public void addedFile(final FileListFile file,final boolean addedOutsideShare) {
						new SUIJob(getText()) {
							@Override
							public void run() {
								UCTextEditor.this.addedFile(file,append,addedOutsideShare);
							}
						}.schedule();
					}
			});
		}
	}
	
	private final class MagnetDropAdapter extends DropTargetAdapter {
		private final boolean append;
		public MagnetDropAdapter(boolean append) {
			this.append = append;
		}
		public void drop(DropTargetEvent event) {
			String fileList[] = null;
			FileTransfer ft = FileTransfer.getInstance();
			if (ft.isSupportedType(event.currentDataType)) {
				fileList = (String[])event.data;
				for (String file:fileList) {
					File f = new File(file);
					UCTextEditor.this.dropFile(f,append);
				}
			}
		}
	}


	/**
	 * selects user if a nick is under the mousepointer
	 * otherwise selects the text currently selected
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class TextUserSelectionprovider extends ItemSelectionProvider implements ISelectionProvider {
		
		private final StyledText text;
		private final IHub hub;
		private final Point mousepos = new Point(0,0);
		/**
		 * 
		 * @param text a text on which selections happen
		 * @param hub where the users appearing in that text are from
		 */
		public TextUserSelectionprovider(StyledText text,IHub hub) {
			this.text = text;
			this.hub = hub;
			addListeners();
			setSelection(null);
		}
		
		private void addListeners() {
			text.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					mousepos.x = e.x;
					mousepos.y = e.y;
					IUser usr = getUsrFromPosition(mousepos.x,mousepos.y);
					logger.debug("slection set to: "+ (usr!= null?usr.toString():"empty"));
					if (usr != null) {
						setSelection(usr);
					} else {
						setSelection(text.getSelectionText());
					}
					
				}
	    	});
		}
		
		
		/**
		 * gets user by MousePosition in styled Text..
		 * 
		 * @return null if none found
		 */
		protected IUser getUsrFromPosition(int x, int y) {
			int cursorPos ;
			try {
				cursorPos = text.getOffsetAtLocation(new Point(x,y));
			} catch (IllegalArgumentException iae) {
				return null; //break .. don't know how to do this diffently..
			}
			
			String tFound = text.getText();
			int space = tFound.lastIndexOf(' ', cursorPos);
			int smaller = tFound.lastIndexOf('<', cursorPos);
			int smaller2 =  tFound.lastIndexOf('\n', cursorPos);
			int begin = Math.max(space, smaller);
			begin = Math.max(smaller2, begin);
			
			int space2 = tFound.indexOf(' ', cursorPos);
			int bigger = tFound.indexOf('>', cursorPos);
			int bigger2 = tFound.indexOf('\n', cursorPos);
			int end = smallestNonnegative(space2,  bigger);
			end = smallestNonnegative(end, bigger2);
			
//			logger.debug("spacebegin:"+space+" <begin:"+smaller+"\n"
//						+" spaceend:"+space2+" >ende:"+bigger +"\nstart:"+begin+" end:"+end);
			
			if (begin != -1 && end != -1 && begin < end) {
				String nick = tFound.substring(begin+1, end);
				logger.debug("nick found: "+nick);
				return hub.getUserByNick(nick);
			}
			return null;
		}
		
		private static int smallestNonnegative(int a ,int b) {
			if (a < 0 || b < 0) {
				return Math.max(a, b);
			} else {
				return Math.min(a, b);
			}
		}
		
	}

}
