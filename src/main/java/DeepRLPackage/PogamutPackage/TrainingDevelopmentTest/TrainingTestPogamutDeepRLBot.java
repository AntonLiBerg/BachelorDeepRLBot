package DeepRLPackage.PogamutPackage.TrainingDevelopmentTest;
import DeepRLPackage.DeepRLBrain;
import DeepRLPackage.PogamutPackage.*;
import com.google.common.io.Files;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import java.util.List;
import java.util.Arrays;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.Rotation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ItemPickedUp;        
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Move;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.Date;



////// New GameState and changed rewards
@AgentScoped
public class TrainingTestPogamutDeepRLBot extends UT2004BotModuleController {

    public static final double LIVING_REWARD = -0.00004;
    public static final double DEATH_REWARD = -0.2;
    public static final double FRAG_REWARD = 0.6;
    public static final double DEAL_DAMAGE_REWARD_MODIFIER = FRAG_REWARD * 0.001;
    public static final double TAKE_DAMAGE_REWARD_MODIFIER = DEATH_REWARD * 0.001;
    public static final List<String> Actions = Arrays.asList(
            /*Name of file containing functions that trigger actions*/
            /*AdvancedLocomotion*/
           "strafeRight", "strafeLeft", "doubleJump", "jump", "turnToEnemy",
          "RunTowardsNearestAmmo", "RunTowardsNearestHealthItem", "ChaseEnemy",            
           /* AdvancedShooting*/ 
          "shootPrimaryAtEnemy",
            /*Custom*/
          "MoveForwards","MoveBackwards","MoveLeft","MoveRight"
    );
    private static final String NN_FILES_PATH = "C:\\Users\\Anton\\Desktop\\C-Uppsatts\\Projects\\DeepRLProject\\src\\main\\java\\DeepRLPackage\\PogamutPackage\\TrainingDevelopmentTest\\NewTrainingData";
    private static final String SCORE_SAVE_PATH = "C:\\Users\\Anton\\Desktop\\C-Uppsatts\\Projects\\DeepRLProject\\src\\main\\java\\DeepRLPackage\\PogamutPackage\\TrainingDevelopmentTest\\";
    
    private DeepRLBrain qLearningAgent;
    private Boolean isTraining = false; 
    private Location previousLocation;
    private Boolean stoppedShooting = false;
    private Boolean isDead = false;
    private String action, previousAction = "";
    private double reward;
    private PogamutGameState state;
    private int stuckCounter;
    private int epochCounter;
    public double totalReward = 0;
    private int counter = 220;
    private File oldestFile;
    private double epsilon;
    private Long startTime;
    
    @Override
    public void prepareBot(UT2004Bot bot) {  
        //<editor-fold defaultstate="collapsed" desc="Init and Find Oldest File">
        boolean noFilesFound = false;
        oldestFile = null;
        File dir = new File(NN_FILES_PATH);
        File[] files = dir.listFiles();
        if(files.length != 0){
            oldestFile = files[0];
            for (int i = 1; i < files.length; i++) {
                if (oldestFile.lastModified() > files[i].lastModified()) {
                    oldestFile = files[i];
                }
            }
        }
        System.out.println("Retrieved File: "+oldestFile.getName());
        //</editor-fold>
        
        // DEFINE LEARNING RATE BASED ON 20 minute sessions
        if(files.length >= 360){
            epsilon = 1.0;
        }
        else if(files.length >= 342){
            epsilon = 0.8;
        }
        else if(files.length >= 288){
            epsilon = 0.6;
        }
        else if(files.length >= 180){
            epsilon = 0.4;
        }
        else if(files.length >= 72){
            epsilon = 0.2;
        }
        else if(files.length >= 36){
            epsilon = 0.1;
        }          
        qLearningAgent = new DeepRLBrain(oldestFile,0, PogamutGameState.UT2004NrOfFeatures,Actions.size(),NN_FILES_PATH);           
        
        //DEFINE LEARNING RATE BASED ON 20 minute sessions
    }

    @Override
    public void beforeFirstLogic() {  
        stuckCounter = 0;
        epochCounter = 0;
        startTime = new Date().getTime();
        
        move.setRotationSpeed(new Rotation(4608,90000 , 3072));
        if(isTraining)
            System.out.println("IS TRAINING------------------------------------------------------------------------------------++++++++++++++");
    }
    
    @Override
    public void logic() throws PogamutException {        
        if (previousAction.equals("shootPrimaryAtEnemy") && !stoppedShooting) {
            shoot.stopShooting();
            stoppedShooting = true;
        } else {            
            //<editor-fold defaultstate="collapsed" desc="Stuck Detection">
            if (info.atLocation(previousLocation)) {
                stuckCounter++;
            } else {
                stuckCounter = 0;
            }
            if (stuckCounter == 300) {
                stuckCounter = 0;
                System.out.println("dddddddddddddddddddd_BOT STUCK!_bbbbbbbbbbbbbbbbbbbbbbb");
                bot.respawn();
            }
            //</editor-fold>
            
            stoppedShooting = false;
            state = new PogamutGameState(info, players, items, senses);  
            
            totalReward += reward;
            reward = LIVING_REWARD;   
            
            action = qLearningAgent.getPolicy(state, Actions);            
            performAction(action);
            
            previousAction = action;            
            previousLocation = info.getLocation();
            isDead = false;
        }
        //Is it time to restart the map?
        if (epochCounter == 5000) {            
            System.out.println("öööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööö");
            System.out.println("öööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööö");
            System.out.println("Test Finished after "+ String.valueOf((new Date().getTime() - startTime) * 0.00001666667));
            try{        
                String s = "Name:"+oldestFile.getName()+",Epsilon:?"+"," +"Score:"+totalReward+"\n";
                System.out.println("-Saving values-");
                System.out.println(s);
                
                try {
                    Files.append(s, new File(SCORE_SAVE_PATH+"Score.csv"), Charset.defaultCharset());
                }catch (Exception e) {}                
                
                System.out.println("SCORE SAVED!");
            }catch(Exception e){
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!ERROR SAVING SCORE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }            
            System.out.println("öööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööö");
            System.out.println("öööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööööö");
            
            if(!oldestFile.delete()){
                System.out.println("DELETE FAAIIIIIIIIIIIIIIIIIIIIILED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }            
            getBot().kill();
        }
        epochCounter++;
    }
    
    private void performAction(String action) {
        try {
          
            //<editor-fold defaultstate="collapsed" desc="Dodging">
            //else if(action.equals("moveContinuos"))
            // move.moveContinuos();
            if (action.equals("strafeRight")) {
                move.strafeRight(100);
            } else if (action.equals("strafeLeft")) {
                move.strafeLeft(100);
            } else if (action.equals("doubleJump")) {
                move.doubleJump();
            } else if (action.equals("jump")) {
                move.jump();
            }
            
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="Looking (turnTo)">            
            else if (action.equals("turnToEnemy")) {
                if(players.canSeeEnemies())
                    move.turnTo(players.getNearestVisibleEnemy());
            } //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Run To Target">
            
            else if (action.equals("RunTowardsNearestAmmo") && items.getPathNearestSpawnedItem(ItemType.Category.AMMO) != null) {
                navigation.navigate(items.getPathNearestSpawnedItem(ItemType.Category.AMMO));
            } 
            else if (action.equals("RunTowardsNearestHealthItem") && items.getPathNearestSpawnedItem(ItemType.Category.HEALTH) != null) {
                navigation.navigate(items.getPathNearestSpawnedItem(ItemType.Category.HEALTH));
            }            
            else if (action.equals("ChaseEnemy")) {
                if (players.canSeeEnemies()) {
                    navigation.navigate(players.getNearestVisibleEnemy());
                }
                else if(players.getNearestEnemy(Double.MAX_VALUE) != null){
                    navigation.navigate(players.getNearestEnemy(Double.MAX_VALUE));
                }
                
            }
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="Directional Movement">               
            else if (action.equals("MoveForwards")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().scale(119)));
            }
            else if (action.equals("MoveBackwards")) {             
                getAct().act(
                    new Move()
                            .setFirstLocation(info.getLocation().sub(info.getRotation().toLocation().scale(119)))
                            .setFocusLocation(info.getLocation()));            
            }
            else if (action.equals("MoveLeft")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().cross(new Location(new double[]{0,0,-1}).getNormalized().scale(119))));
            }
            else if (action.equals("MoveRight")) {             
                move.moveTo(info.getLocation().add(info.getRotation().toLocation().cross(new Location(new double[]{0,0,1}).getNormalized().scale(119))));
            }
            //</editor-fold>            
            //<editor-fold defaultstate="collapsed" desc="ShootCommands">
            else if (action.equals("shootPrimaryAtEnemy")) {
                if (players.canSeeEnemies()) {
                    shoot.shoot(players.getNearestVisibleEnemy());                    
                }
            }
//</editor-fold>

        } //Sometimes, some actions are unavailable. At which point, nullpointerException is Thrown.
        catch (Exception e) {
            System.out.println("EXCEPTION: " + e.toString() + ". During action: "+action);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Reward Functions and Listeners">
    private Boolean isTerminalState() {
        if (isDead) {
            return true;
        }
        return false;
    }
    @Override
    public void botKilled(BotKilled event) {
        reward = DEATH_REWARD;
        isDead = true;
    }
    
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        reward = FRAG_REWARD;     
        System.err.println("!!!!!AGENT KILLED ENEMY!!!!!");
    }
    
    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
        reward += (double) event.getDamage() * DEAL_DAMAGE_REWARD_MODIFIER;
    }
    
    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
        reward += (double) event.getDamage() * TAKE_DAMAGE_REWARD_MODIFIER;
    }
    
    @EventListener(eventClass = ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
        if (event.getType().getCategory() == ItemType.Category.HEALTH) {
            double r = ((200 - info.getHealth())) * event.getAmount(); // MAX = 0.009950 MIN = 0
            reward += r * 0.000002;
        } else if (event.getType().getCategory() == ItemType.Category.AMMO
                || event.getType().getCategory() == ItemType.Category.WEAPON) {
            double r = ((30 - info.getCurrentAmmo())) * event.getAmount(); // MAX = 0.00072 MIN = 0            
            reward += r * 0.000002;
        }
    }
    
    
    
//</editor-fold>
        
    public static void main(String args[]) throws PogamutException {
        //<editor-fold defaultstate="collapsed" desc="Init">
        AddNativeBot addBot = null;
        UT2004BotRunner ut = new UT2004BotRunner( // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                TrainingTestPogamutDeepRLBot.class, // which UT2004BotController it should instantiate
                "Opponent1" // what name the runner should be using
        ).setMain(true);                // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed        
        //</editor-fold>
        
        while(true){
            try{                      
                addBot = new AddNativeBot();
                ut.startAgent();             // tells the runner to start 1 agent
                
            }catch(Exception e){}  
            addBot.RestartMap();
       }
    }
}
