package DeepRLPackage.PogamutPackage;

import cz.cuni.amis.pogamut.base.utils.Pogamut;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.params.UT2004AgentParameters;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.AddBot;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.DisconnectBot;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.KillBot;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Kick;
import cz.cuni.amis.pogamut.ut2004.factory.guice.remoteagent.UT2004ServerFactory;
import cz.cuni.amis.pogamut.ut2004.factory.guice.remoteagent.UT2004ServerModule;
import cz.cuni.amis.pogamut.ut2004.server.IUT2004Server;
import cz.cuni.amis.pogamut.ut2004.server.impl.UT2004Server;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004ServerRunner;
import cz.cuni.amis.utils.exception.PogamutException;

/**
 * This simple example will show you how to add native bot to a game programatically.
 * 
 * @author Jakub Gemrot aka Jimmy
 */
@AgentScoped
public class AddNativeBot extends UT2004BotModuleController {
    public UT2004Server server = null;
    public AddNativeBot() throws PogamutException {
    	try {
	    	// First we will need to instantiate a UT2004Server - for that, we need UT2004ServerRunner.
	    	UT2004ServerRunner<IUT2004Server, UT2004AgentParameters> serverRunner = 
	    		new UT2004ServerRunner(
	    			// it needs a factory that will provide new UT2004Server instances for the runner
	    			new UT2004ServerFactory<IUT2004Server, UT2004AgentParameters>(
	    				// and factory must know how to assemble UT2004Server which is encoded inside UT2004ServerModule
	    				new UT2004ServerModule<UT2004AgentParameters>()
	    			)
	    		);	    	
	    	// let's start the server!
	    	server = (UT2004Server)serverRunner.startAgent();
	    	
	    	// at this point we're connected to ControlConnection of GameBots2004 (port 3001), this is a "service" channel
	    	// that allows you to configure the game / watch players, etc.
	    	
	    	// so, let's add a bot
	    	
                //server.connectNativeBot("Bot1", "Bot1");
                
	    	// for that we will need AddBot command
	    	AddBot addBotCommand = new AddBot();
	    	
	    	// and we need to configure it properly
	    	addBotCommand.setName("Opponent2").setSkill(5).setTeam(0);
	    	
	    	// and fire it up!
	    	server.getAct().act(addBotCommand);
	    	/*
	    	// now just wait a bit to let JVM dispatch the command
	    	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
	    	
	    	// AND THAT'S ALL FOLKS!
	    	
	    	// BYE BYE!
	    	
	    	
                */
	    	
    	} finally {
    		// we have to tear down everything in the end...
    		//if (server != null) server.kill();
    		//server = null;
    		// ... and close the platform that will close MBeanServer(s) as well, shutting down RMI registry daemon thread
    		//Pogamut.getPlatform().close();
    	}        
    }

    public void RestartMap(){        
        server.setGameMap(server.getMapName());
        
        server.stop();
        if (server != null) server.kill();
    	server = null;    	
    	Pogamut.getPlatform().close();
        synchronized(this){
            try{
                wait(10000);
            }catch(Exception e){}
        }
    }
}
